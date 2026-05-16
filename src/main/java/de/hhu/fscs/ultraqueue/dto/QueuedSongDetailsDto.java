package de.hhu.fscs.ultraqueue.dto;

import java.util.UUID;

public record QueuedSongDetailsDto(
        UUID entryId,
        UUID songId,
        String title,
        String artist,
        String language,
        Integer year,
        String genre,
        long lengthSeconds,
        String estimatedStart,
        String username,
        String userColor,
        int position
) {
}

