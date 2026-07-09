package de.hhu.fscs.ultraqueue.service;

import de.hhu.fscs.ultraqueue.config.UltraQueueProperties;
import de.hhu.fscs.ultraqueue.model.Song;
import de.hhu.fscs.ultraqueue.parser.SongTxtParser;
import de.hhu.fscs.ultraqueue.persistence.interfaces.SongRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;
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

    private final SongTxtParser songTxtParser;
    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");

    private final SongRepository songRepository;

    public SongCatalogServiceImpl(UltraQueueProperties props, SongTxtParser songTxtParser, SongRepository songRepository) {
        this.songTxtParser = songTxtParser;
        this.props = props;
        this.songRepository = songRepository;
    }


    @Override
    public void refreshData() {
        // fresh read of all songs.
        songRepository.removeAll();
        Path root = Paths.get(props.songFolder()).toAbsolutePath().normalize();
        log.info("Scanning UltraStar song folder: {}", root);
        if (!Files.isDirectory(root)) {
            throw new IllegalStateException("Song folder does not exist or is not a directory: " + root);
        }

        try (Stream<Path> paths = Files.walk(root, FileVisitOption.FOLLOW_LINKS)) {
            paths.filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".txt"))
                    .filter(Files::isRegularFile)
                    .forEach(this::processSongFile);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to scan song folder", e);
        }

        log.info("Loaded {} songs into the catalogue", songRepository.size());
    }

    /** -----------------------------------------------------------------
     *  Parses a single UltraStar .txt file and adds it to the catalogue.
     *  ----------------------------------------------------------------- */
    private void processSongFile(Path txt) {
        try {
            Song song = songTxtParser.parse(txt);
            String key = makeTitleArtistKey(song.title(), song.artist());
            songRepository.addSong(song, txt, key);
        } catch (Exception e) {
            log.warn("Unable to read song file {} – skipping ({}).", txt, e.getMessage());
        }
    }


    private static String makeTitleArtistKey(String title, String artist) {
        return (title == null ? "" : title.toLowerCase(Locale.ROOT).trim()) + "::" +
                (artist == null ? "" : artist.toLowerCase(Locale.ROOT).trim());
    }

    private static String normalize(String text) {
        if (text == null) {
            return "";
        }

        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD);
        normalized = DIACRITICS.matcher(normalized).replaceAll("");
        return normalized.toLowerCase(Locale.ROOT);
    }

    // -----------------------------------------------------------------
    // PUBLIC API implementations
    // -----------------------------------------------------------------
    @Override
    public Page<Song> findAll(Pageable pageable) {
        return search("", pageable);
    }

    @Override
    public Page<Song> search(String query, Pageable pageable) {
        String normalizedQuery = normalize(query);

        List<Song> filtered = songRepository.loadAll().stream()
                .filter(s -> normalizedQuery.isEmpty() || normalize(s.toString()).contains(normalizedQuery))
                .sorted(createComparator(pageable.getSort()))
                .toList();

        return toPage(filtered, pageable);
    }

    @Override
    public Optional<Song> findById(UUID id) {
        return Optional.ofNullable(songRepository.songById(id));
    }

    @Override
    public Optional<Song> findByTitleArtist(String title, String artist) {
        if (title == null || artist == null) return Optional.empty();
        String key = makeTitleArtistKey(title, artist);
        UUID id = songRepository.songIdByTitleArtist(key);
        return (id == null) ? Optional.empty() : findById(id);
    }

    @Override
    public Optional<List<String>> findLyricsById(UUID id) {
        Path txtPath = songRepository.txtById(id);
        if (txtPath == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(songTxtParser.parseLyrics(txtPath));
        } catch (RuntimeException ex) {
            log.warn("Unable to read lyrics for song {} from {}", id, txtPath, ex);
            return Optional.empty();
        }
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

    private Comparator<Song> createComparator(Sort sort) {
        Comparator<Song> comparator = null;
        for (Sort.Order order : sort) {
            Comparator<Song> fieldComparator;
            switch (order.getProperty().toLowerCase(Locale.ROOT)) {
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
            if (comparator == null) {
                comparator = fieldComparator;
            } else {
                comparator = comparator.thenComparing(fieldComparator);
            }
        }
        if (comparator == null) {
            comparator = Comparator.comparing(Song::title, String.CASE_INSENSITIVE_ORDER);
        }
        return comparator;
    }
}