package de.hhu.fscs.ultraqueue.persistence.implementation.db;

import de.hhu.fscs.ultraqueue.persistence.implementation.db.dto.QueueEntryDto;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SpringDataQueueEntryRepository extends CrudRepository<QueueEntryDto, UUID> {
    @Override
    List<QueueEntryDto> findAll();

    @Override
    void deleteAll();

    @Modifying
    @Query("""
        INSERT INTO queue_entries
            (id,
             song_id,
             user_id,
             username,
             user_color,
             position)
        VALUES
            (:#{#dto.id},
             :#{#dto.songId.id},
             :#{#dto.userId},
             :#{#dto.username},
             :#{#dto.userColor},
             :#{#dto.position})
        """)
    void addQueueEntry(QueueEntryDto dto);
}
