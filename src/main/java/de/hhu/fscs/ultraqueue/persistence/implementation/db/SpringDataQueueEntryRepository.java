package de.hhu.fscs.ultraqueue.persistence.implementation.db;

import de.hhu.fscs.ultraqueue.persistence.implementation.db.dto.QueueEntryDto;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.UUID;

public interface SpringDataQueueEntryRepository extends CrudRepository<QueueEntryDto, UUID> {
    @Override
    List<QueueEntryDto> findAll();

    @Override
    void deleteAll();
}
