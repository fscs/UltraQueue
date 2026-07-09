package de.hhu.fscs.ultraqueue.model;


import java.time.Instant;
import java.util.UUID;

public record PlayedSongLog(UUID logId, UUID songId, Instant playedAt) {

    public PlayedSongLog(UUID logId, UUID songId, Instant playedAt) {
        this.logId = logId;
        this.songId = songId;
        this.playedAt = playedAt;
    }

    public PlayedSongLog(UUID songId, Instant playedAt) {
        this(null, songId, playedAt);
    }
}