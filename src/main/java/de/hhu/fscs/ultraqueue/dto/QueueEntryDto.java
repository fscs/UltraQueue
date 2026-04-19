package de.hhu.fscs.ultraqueue.dto;

import de.hhu.fscs.ultraqueue.model.QueueEntry;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Data that is sent to the Thymeleaf template.  The DTO is immutable –
 * the service builds it once per request.
 */
public record QueueEntryDto(
        UUID entryId,
        String title,
        String artist,
        int position,
        String estimatedStart,   // formatted "HH:mm:ss"
        boolean ownedByMe) {     // true if the entry belongs to the current user

    public static QueueEntryDto of(QueueEntry e, Instant estimate, String currentUserId) {
        String fmt = DateTimeFormatter.ofPattern("HH:mm:ss")
                .withZone(ZoneId.systemDefault())
                .format(estimate);
        boolean mine = e.getUserId().equals(currentUserId);
        return new QueueEntryDto(e.getId(),
                e.getSong().title(),
                e.getSong().artist(),
                e.getPosition(),
                fmt,
                mine);
    }
}