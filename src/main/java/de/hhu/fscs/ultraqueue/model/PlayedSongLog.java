package de.hhu.fscs.ultraqueue.model;

import java.time.Instant;
import java.util.UUID;

public record PlayedSongLog(UUID songId, Instant playedAt) {
}