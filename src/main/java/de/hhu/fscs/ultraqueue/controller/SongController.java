package de.hhu.fscs.ultraqueue.controller;

import de.hhu.fscs.ultraqueue.dto.QueuedSongDetailsDto;
import de.hhu.fscs.ultraqueue.exception.NotFoundException;
import de.hhu.fscs.ultraqueue.service.QueueService;
import de.hhu.fscs.ultraqueue.service.SongCatalogService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.UUID;

@Controller
public class SongController {

    private final QueueService queueService;
    private final SongCatalogService songCatalogService;

    public SongController(QueueService queueService, SongCatalogService songCatalogService) {
        this.queueService = queueService;
        this.songCatalogService = songCatalogService;
    }

    @GetMapping("/song/{id}")
    public String songDetails(@PathVariable UUID id, Model model) {
        QueuedSongDetailsDto details = queueService.getQueuedSongDetails(id)
                .orElseThrow(() -> new NotFoundException("Queue entry not found"));

        List<String> lyrics = songCatalogService.findLyricsById(details.songId())
                .orElse(List.of("Lyrics currently unavailable."));

        model.addAttribute("song", details);
        model.addAttribute("lyrics", lyrics);
        return "song";
    }
}

