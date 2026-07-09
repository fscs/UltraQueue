package de.hhu.fscs.ultraqueue.persistence.implementation.db.dto;

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
