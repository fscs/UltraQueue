package de.hhu.fscs.ultraqueue.service;

import de.hhu.fscs.ultraqueue.config.UltraQueueProperties;
import de.hhu.fscs.ultraqueue.model.Song;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * In‑memory implementation of the catalog.  It scans the configured
 * UltraStar song folder once on start‑up and keeps the data in a
 * thread‑safe map.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SongCatalogServiceImpl implements SongCatalogService {

    private final UltraQueueProperties props;

    /** UUID → Song */
    private final Map<UUID, Song> songById = new ConcurrentHashMap<>();

    /** (title‑lowercase, artist‑lowercase) → UUID – gives O(1) lookup for the API */
    private final Map<String, UUID> titleArtistIndex = new ConcurrentHashMap<>();

    /** -----------------------------------------------------------------
     *  Scan the folder at application start‑up.
     *  ----------------------------------------------------------------- */
    @PostConstruct
    public void init() {
        Path root = Paths.get(props.getSongFolder()).toAbsolutePath().normalize();
        log.info("Scanning UltraStar song folder: {}", root);
        if (!Files.isDirectory(root)) {
            throw new IllegalStateException("Song folder does not exist or is not a directory: " + root);
        }

        try (Stream<Path> dirs = Files.list(root)) {
            dirs.filter(Files::isDirectory)
                    .forEach(this::processSongDirectory);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to list song folder", e);
        }

        log.info("Loaded {} songs into the catalogue", songById.size());
    }

    /** -----------------------------------------------------------------
     *  One UltraStar song lives in its own directory.  This method reads
     *  the *.txt* file inside that directory and creates a {@link Song}
     *  instance.
     *  ----------------------------------------------------------------- */
    private void processSongDirectory(Path songDir) {
        try (Stream<Path> txtFiles = Files.list(songDir)
                .filter(p -> p.getFileName().toString().toLowerCase().endsWith(".txt"))) {

            Optional<Path> maybeTxt = txtFiles.findFirst();
            if (maybeTxt.isEmpty()) {
                log.warn("Directory {} does not contain a .txt file – ignored", songDir);
                return;
            }

            Path txt = maybeTxt.get();
            Song song = parseTxtFile(txt, songDir);
            songById.put(song.getId(), song);
            String key = makeTitleArtistKey(song.getTitle(), song.getArtist());
            titleArtistIndex.put(key, song.getId());

        } catch (IOException e) {
            log.warn("Unable to read directory {} – skipping ({}).", songDir, e.getMessage());
        }
    }

    /** -----------------------------------------------------------------
     *  Very small parser that extracts the five fields we care about.
     *  UltraStar .txt files are UTF‑8 text files with lines like
     *  “#TITLE: My Song”.
     *  ----------------------------------------------------------------- */
    private Song parseTxtFile(Path txtFile, Path songFolder) {
        String title = null, artist = null, language = null;
        Integer year = null;
        Duration length = null;   // we will try to deduce it from #START/END or default to 180 s

        try (BufferedReader br = Files.newBufferedReader(txtFile)) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("#TITLE:")) {
                    title = line.substring(7).trim();
                } else if (line.startsWith("#ARTIST:")) {
                    artist = line.substring(8).trim();
                } else if (line.startsWith("#LANGUAGE:")) {
                    language = line.substring(11).trim();
                } else if (line.startsWith("#YEAR:")) {
                    String y = line.substring(6).trim();
                    try {
                        year = Integer.valueOf(y);
                    } catch (NumberFormatException ignored) {}
                } else if (line.startsWith("#START:")) {
                    // UltraStar uses #START and #END to describe the audio range (in seconds)
                    // We’ll capture them later when we also see #END.
                } else if (line.startsWith("#END:")) {
                    // If we have #START already we could calculate, but for simplicity we treat END as length.
                    String secs = line.substring(5).trim();
                    try {
                        length = Duration.ofSeconds(Long.parseLong(secs));
                    } catch (NumberFormatException ignored) {}
                }
                // other tags are ignored for this catalogue
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read song definition: " + txtFile, e);
        }

        // Fallbacks – UltraStar songs are guaranteed to have title/artist, but we protect against missing data.
        if (title == null || title.isBlank()) title = "Untitled";
        if (artist == null || artist.isBlank()) artist = "Unknown";

        // If length is still unknown, use a generic default (3 minutes).  Real UltraStar clients would read the MP3 length,
        // but that would require a heavy library (e.g. jaudiotagger).  For the purpose of queue‑time estimation a constant is fine.
        if (length == null) length = Duration.ofSeconds(180); // 3 min

        return new Song.Builder()
                .title(title)
                .artist(artist)
                .language(language)
                .year(year)
                .length(length)
                .folder(songFolder)
                .build();
    }

    private static String makeTitleArtistKey(String title, String artist) {
        return (title == null ? "" : title.toLowerCase().trim()) + "::" +
                (artist == null ? "" : artist.toLowerCase().trim());
    }

    // -----------------------------------------------------------------
    // PUBLIC API implementations
    // -----------------------------------------------------------------
    @Override
    public Page<Song> findAll(Pageable pageable) {
        List<Song> all = new ArrayList<>(songById.values());

        // Apply sorting defined in the Pageable (Spring will give us a Sort object)
        if (pageable.getSort().isSorted()) {
            Comparator<Song> comparator = createComparator(pageable.getSort());
            all.sort(comparator);
        }

        return toPage(all, pageable);
    }

    @Override
    public Page<Song> search(String query, Pageable pageable) {
        if (query == null) query = "";
        String lowered = query.toLowerCase();

        List<Song> filtered = songById.values().stream()
                .filter(s -> s.getTitle().toLowerCase().contains(lowered) ||
                        s.getArtist().toLowerCase().contains(lowered))
                .collect(Collectors.toList());

        if (pageable.getSort().isSorted()) {
            filtered.sort(createComparator(pageable.getSort()));
        }

        return toPage(filtered, pageable);
    }

    @Override
    public Optional<Song> findById(UUID id) {
        return Optional.ofNullable(songById.get(id));
    }

    @Override
    public Optional<Song> findByTitleArtist(String title, String artist) {
        if (title == null || artist == null) return Optional.empty();
        String key = makeTitleArtistKey(title, artist);
        UUID id = titleArtistIndex.get(key);
        return (id == null) ? Optional.empty() : findById(id);
    }

    // -----------------------------------------------------------------
    // Helper – convert a List → Spring Page
    // -----------------------------------------------------------------
    private Page<Song> toPage(List<Song> source, Pageable pageable) {
        int total = source.size();
        int pageSize = pageable.getPageSize();
        int pageNumber = pageable.getPageNumber();

        int fromIndex = Math.min(pageNumber * pageSize, total);
        int toIndex = Math.min(fromIndex + pageSize, total);
        List<Song> content = source.subList(fromIndex, toIndex);

        return new PageImpl<>(content, pageable, total);
    }

    // -----------------------------------------------------------------
    // Helper – build a comparator from a Spring Sort object.
    // -----------------------------------------------------------------
    private Comparator<Song> createComparator(Sort sort) {
        Comparator<Song> comparator = Comparator.comparing(Song::getTitle, String.CASE_INSENSITIVE_ORDER);
        for (Sort.Order order : sort) {
            Comparator<Song> fieldComparator;
            switch (order.getProperty().toLowerCase()) {
                case "title" -> fieldComparator = Comparator.comparing(Song::getTitle, String.CASE_INSENSITIVE_ORDER);
                case "artist" -> fieldComparator = Comparator.comparing(Song::getArtist, String.CASE_INSENSITIVE_ORDER);
                case "lengthsec", "length" -> fieldComparator = Comparator.comparingLong(Song::getLengthSeconds);
                case "year" -> fieldComparator = Comparator.comparing(s -> s.getYear() == null ? 0 : s.getYear());
                default -> {
                    // unknown field – ignore it
                    continue;
                }
            }
            if (order.isDescending()) {
                fieldComparator = fieldComparator.reversed();
            }
            comparator = comparator.thenComparing(fieldComparator);
        }
        return comparator;
    }
}