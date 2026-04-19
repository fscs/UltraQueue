package de.hhu.fscs.ultraqueue.controller;

import de.hhu.fscs.ultraqueue.dto.SongFinishedDto;
import de.hhu.fscs.ultraqueue.service.QueueService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * End‑points used by the UltraStar client (the game binary).
 */
@RestController
public class ApiController {

    private final QueueService queueService;

    public ApiController(QueueService queueService) {
        this.queueService = queueService;
    }

    /** UltraStar asks for the title of the next song (plain text). */
    @GetMapping(value = "/nextsong", produces = MediaType.TEXT_PLAIN_VALUE)
    public String nextSong() {
        // Returns empty string when the queue is empty – UltraStar will just stay on the selection screen with search open.
        return queueService.getNextSongTitle();
    }

    /** UltraStar informs that a song has finished (JSON payload). */
    @PostMapping(value = "/songfinished",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.TEXT_PLAIN_VALUE)
    public String songFinished(@Valid @RequestBody SongFinishedDto payload) {
        UUID songId = queueService.resolveSongId(payload.title(), payload.artist());
        queueService.markFinished(songId);
        return "OK";
    }
}
