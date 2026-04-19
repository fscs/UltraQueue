package de.hhu.fscs.ultraqueue.service;

import de.hhu.fscs.ultraqueue.config.UltraQueueProperties;
import de.hhu.fscs.ultraqueue.model.Song;
import de.hhu.fscs.ultraqueue.parser.SongTxtParser;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
public class SongCatalogServiceImpl implements SongCatalogService {
    private static final Logger log = LoggerFactory.getLogger(SongCatalogServiceImpl.class);

    private final UltraQueueProperties props;

    /** UUID → Song */
    private final Map<UUID, Song> songById = new ConcurrentHashMap<>();

    /** (title‑lowercase, artist‑lowercase) → UUID – gives O(1) lookup for the API */
    private final Map<String, UUID> titleArtistIndex = new ConcurrentHashMap<>();

    public SongCatalogServiceImpl(UltraQueueProperties props) {
        this.props = props;
    }

    /** -----------------------------------------------------------------
     *  Scan the folder at application start‑up.
     *  ----------------------------------------------------------------- */
    @PostConstruct
    public void init() {
        Path root = Paths.get(props.songFolder()).toAbsolutePath().normalize();
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
            Song song = SongTxtParser.parse(txt);
            songById.put(song.id(), song);
            String key = makeTitleArtistKey(song.title(), song.artist());
            titleArtistIndex.put(key, song.id());

        } catch (IOException e) {
            log.warn("Unable to read directory {} – skipping ({}).", songDir, e.getMessage());
        }
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
                .filter(s -> s.title().toLowerCase().contains(lowered) ||
                        s.artist().toLowerCase().contains(lowered))
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
        Comparator<Song> comparator = Comparator.comparing(Song::title, String.CASE_INSENSITIVE_ORDER);
        for (Sort.Order order : sort) {
            Comparator<Song> fieldComparator;
            switch (order.getProperty().toLowerCase()) {
                case "title" -> fieldComparator = Comparator.comparing(Song::title, String.CASE_INSENSITIVE_ORDER);
                case "artist" -> fieldComparator = Comparator.comparing(Song::artist, String.CASE_INSENSITIVE_ORDER);
                case "lengthsec", "length" -> fieldComparator = Comparator.comparingLong(Song::getLengthSeconds);
                case "year" -> fieldComparator = Comparator.comparing(s -> s.year() == null ? 0 : s.year());
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