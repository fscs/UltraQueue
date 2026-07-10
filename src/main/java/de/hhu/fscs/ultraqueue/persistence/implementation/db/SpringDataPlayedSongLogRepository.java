package de.hhu.fscs.ultraqueue.persistence.implementation.db;

import de.hhu.fscs.ultraqueue.persistence.implementation.db.dto.PlayedSongLogDto;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SpringDataPlayedSongLogRepository extends CrudRepository<PlayedSongLogDto, UUID> {
    @Override
    List<PlayedSongLogDto> findAll();
}
