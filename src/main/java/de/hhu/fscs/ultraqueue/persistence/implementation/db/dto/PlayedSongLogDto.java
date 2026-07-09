package de.hhu.fscs.ultraqueue.persistence.implementation.db.dto;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Table("song_logs")
public record PlayedSongLogDto (
        @Id
        UUID logId,
        UUID songId,
        Instant playedAt
)
{}
