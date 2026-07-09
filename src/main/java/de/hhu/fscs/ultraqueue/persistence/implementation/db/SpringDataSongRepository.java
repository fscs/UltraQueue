package de.hhu.fscs.ultraqueue.persistence.implementation.db;

import de.hhu.fscs.ultraqueue.persistence.implementation.db.dto.SongDto;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataSongRepository extends CrudRepository<SongDto, UUID> {
    SongDto findBySongId(UUID songId);
    UUID findSongIdByTitleArtist(String titleArtist);
    List<SongDto> findAll();
    @Override
    void deleteAll();
}
