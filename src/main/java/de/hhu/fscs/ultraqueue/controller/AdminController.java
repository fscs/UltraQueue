package de.hhu.fscs.ultraqueue.controller;

import de.hhu.fscs.ultraqueue.config.UltraQueueProperties;
import de.hhu.fscs.ultraqueue.service.QueueEventService;
import de.hhu.fscs.ultraqueue.service.QueueService;
import de.hhu.fscs.ultraqueue.service.SongCatalogService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private final QueueService queueService;
    private final QueueEventService queueEventService;
    private final SongCatalogService songCatalogService;

    public AdminController(QueueService queueService, QueueEventService queueEventService, SongCatalogService songCatalogService) {
        this.queueService = queueService;
        this.queueEventService = queueEventService;
        this.songCatalogService = songCatalogService;
    }
    @GetMapping(value = "", produces = MediaType.TEXT_PLAIN_VALUE)
    public String login() {
        return "logged in";
    }

    @GetMapping(value = "/start", produces = MediaType.TEXT_HTML_VALUE)
    public String start() {
        queueService.clearQueue();
        queueService.setAllowEditing(true);
        return "<script>alert('The Queue has been cleared and is now open for editing'); history.back();</script>";
    }

    @GetMapping(value = "/stop", produces = MediaType.TEXT_HTML_VALUE)
    public String stop() {
        queueService.setAllowEditing(false);
        return "<script>alert('The Queue has been stopped'); history.back();</script>";
    }

    @GetMapping(value = "/clear", produces = MediaType.TEXT_HTML_VALUE)
    public String clear() {
        queueService.clearQueue();
        queueEventService.notifyQueueChanged();
        return "<script>alert('The Queue has been cleared'); history.back();</script>";
    }

    @GetMapping(value = "/refresh", produces = MediaType.TEXT_HTML_VALUE)
    public String refreshData() {
        songCatalogService.refreshData();
        return "<script>alert('The Data has been refreshed'); history.back();</script>";
    }
}
