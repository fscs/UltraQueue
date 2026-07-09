package de.hhu.fscs.ultraqueue.persistence.implementation.db.dto;

import org.springframework.data.annotation.Id;

import java.time.Instant;
import java.util.UUID;

public record PlayedSongLogDto (
        @Id
        UUID logId,
        UUID songId,
        Instant playedAt
)
{}
