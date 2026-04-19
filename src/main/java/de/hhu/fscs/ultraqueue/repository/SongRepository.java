package de.hhu.fscs.ultraqueue.repository;

import de.hhu.fscs.ultraqueue.model.Song;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SongRepository {
    Optional<Song> findById(UUID id);
    List<Song> findAll();
    // search / sort is delegated to SongCatalogService
}
