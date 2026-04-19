package de.hhu.fscs.ultraqueue.repository;

public interface SongRepository {
    Optional<Song> findById(UUID id);
    List<Song> findAll();
    // search / sort is delegated to SongCatalogService
}
