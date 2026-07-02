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
        String estimatedStart,   // formatted "HH:mm"
        String waitTime,         // in minutes or Soon if <= 1 min
        boolean ownedByMe,       // true if the entry belongs to the current user
        String username,
        String userColor,
        String coverPath
) {

    public static QueueEntryDto of(
            QueueEntry e,
            Instant estimate,
            String currentUserId,
            long waitSeconds
    ) {
        String estStart = DateTimeFormatter.ofPattern("HH:mm")
                .withZone(ZoneId.systemDefault())
                .format(estimate);

        boolean mine = e.getUserId().equals(currentUserId);

        String waitTime = waitSeconds <= 60 ? "Soon" : (waitSeconds / 60) + " min";

        return new QueueEntryDto(
                e.getId(),
                e.getSong().title(),
                e.getSong().artist(),
                e.getPosition(),
                estStart,
                waitTime,
                mine,
                e.getUsername(),
                e.getUserColor(),
                e.getCoverPath()
        );
    }
}