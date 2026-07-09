package de.hhu.fscs.ultraqueue.persistence.dto;

import de.hhu.fscs.ultraqueue.model.QueueEntry;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.jdbc.core.mapping.AggregateReference;

import java.util.UUID;

@Table("queue_entries")
public record QueueEntryDto (
       @Id UUID id,
       AggregateReference<SongDto, UUID> songid,
       String userId,
       String username,
       String userColor,
       int position
)
{}
