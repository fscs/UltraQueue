package de.hhu.fscs.ultraqueue.controller;

import de.hhu.fscs.ultraqueue.web.UserContext;
import de.hhu.fscs.ultraqueue.dto.QueueEntryDto;
import de.hhu.fscs.ultraqueue.exception.BusinessException;
import de.hhu.fscs.ultraqueue.service.QueueService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
@RequestMapping("/queue")
public class QueueController {

    private final QueueService queueService;
    private final UserContext userContext;

    /** Show the current queue with estimated start times. */
    @GetMapping
    public String viewQueue(Model model, HttpServletRequest request) {
        String userId = userContext.getCurrentUserId(request);
        List<QueueEntryDto> entries = queueService.getQueueWithEstimates(userId);
        model.addAttribute("queue", entries);
        return "queue"; // src/main/resources/templates/queue.html
    }

    /** Add a song to the *current* user’s queue (POST from catalogue). */
    @PostMapping("/add")
    public String addSong(@RequestParam @NotBlank String songId,
                          HttpServletRequest request,
                          Model model) {
        String userId = userContext.getCurrentUserId(request);
        try {
            queueService.addSong(userId, UUID.fromString(songId));
            model.addAttribute("flash", "Song added to your queue.");
        } catch (BusinessException ex) {
            model.addAttribute("error", ex.getMessage());
        }
        return "redirect:/queue";
    }

    /** Remove a queue entry – user can delete *their own* entry, admin can delete any. */
    @PostMapping("/remove/{entryId}")
    public String remove(@PathVariable UUID entryId,
                         HttpServletRequest request,
                         Model model) {
        String userId = userContext.getCurrentUserId(request);
        boolean isAdmin = request.isUserInRole("ADMIN");
        queueService.removeEntry(userId, entryId, isAdmin);
        return "redirect:/queue";
    }

    /** Replace a user's entry with another song (keeps the position). */
    @PostMapping("/replace/{entryId}")
    public String replace(@PathVariable UUID entryId,
                          @RequestParam @NotBlank String newSongId,
                          HttpServletRequest request,
                          Model model) {
        String userId = userContext.getCurrentUserId(request);
        queueService.replaceEntry(userId, entryId, UUID.fromString(newSongId));
        return "redirect:/queue";
    }
}
