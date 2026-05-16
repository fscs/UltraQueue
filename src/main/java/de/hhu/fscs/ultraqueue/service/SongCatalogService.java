package de.hhu.fscs.ultraqueue.service;

import de.hhu.fscs.ultraqueue.model.Song;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

/**
 * Public API that the MVC layer (controllers) and the queue logic need.
 */
public interface SongCatalogService {

    /** Returns a paged list of *all* songs, sorted according to {@code pageable}. */
    Page<Song> findAll(Pageable pageable);

    /** Full‑text search across title and artist (case‑insensitive). */
    Page<Song> search(String query, Pageable pageable);

    /** Find a song by its internal UUID (generated during the scan). */
    Optional<Song> findById(UUID id);

    /** Find a song by *exact* title + artist (used by the UltraStar API). */
    Optional<Song> findByTitleArtist(String title, String artist);

    /** Load lyrics for a song by id (plain lines). */
    Optional<List<String>> findLyricsById(UUID id);
}