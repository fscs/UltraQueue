package de.hhu.fscs.ultraqueue.controller;

import de.hhu.fscs.ultraqueue.config.UltraQueueProperties;
import de.hhu.fscs.ultraqueue.web.UserContext;
import de.hhu.fscs.ultraqueue.dto.QueueEntryDto;
import de.hhu.fscs.ultraqueue.exception.BusinessException;
import de.hhu.fscs.ultraqueue.service.QueueService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotBlank;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/queue")
public class QueueController {

    private static final String ROLE_ADMIN = "ADMIN";
    private static final String FLASH = "flash";
    private static final String ERROR = "error";
    private static final String REDIRECT_QUEUE = "redirect:/queue";

    private final UltraQueueProperties props;
    private final QueueService queueService;

    public QueueController(UltraQueueProperties props, QueueService queueService) {
        this.props = props;
        this.queueService = queueService;
    }

    /** Show the current queue with estimated start times. */
    @GetMapping
    public String viewQueue(Model model, HttpServletRequest request) {
        addCurrentUserAttributes(model, request);
        String userId = UserContext.getCurrentUserId(request);
        List<QueueEntryDto> entries = queueService.getQueueWithEstimates(userId);
        model.addAttribute("queue", entries);
        return "queue";
    }

    /** Add a song to the *current* user’s queue (POST from catalogue). */
    @PostMapping("/add")
    public String addSong(@RequestParam @NotBlank String songId,
                          @RequestParam(required = false) String username,
                          HttpServletRequest request,
                          HttpServletResponse response,
                          RedirectAttributes redirectAttributes) {
        try {
            boolean isAdmin = request.isUserInRole(ROLE_ADMIN);
            String userId = UserContext.getCurrentUserId(request);
            String resolvedUsername = resolveUsername(request, username, isAdmin);
            UserContext.setUsernameCookie(response, userId, resolvedUsername, props.cookie().signingSecret());
            queueService.addSong(userId, resolvedUsername, UUID.fromString(songId), isAdmin);
            redirectAttributes.addFlashAttribute(FLASH, "Song added to your queue.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute(ERROR, ex.getMessage());
        }
        return REDIRECT_QUEUE;
    }

    /** Remove a queue entry – user can delete *their own* entry, admin can delete any. */
    @PostMapping("/remove/{entryId}")
    public String remove(@PathVariable UUID entryId,
                         HttpServletRequest request,
                         RedirectAttributes redirectAttributes) {
        String userId = UserContext.getCurrentUserId(request);
        boolean isAdmin = request.isUserInRole(ROLE_ADMIN);
        try {
            queueService.removeEntry(userId, entryId, isAdmin);
            redirectAttributes.addFlashAttribute(FLASH, "Entry removed.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute(ERROR, ex.getMessage());
        }
        return REDIRECT_QUEUE;
    }

    /** Replace a user's entry with another song (keeps the position). */
    @PostMapping("/replace/{entryId}")
    public String replace(@PathVariable UUID entryId,
                          @RequestParam @NotBlank String newSongId,
                          HttpServletRequest request,
                          RedirectAttributes redirectAttributes) {
        String userId = UserContext.getCurrentUserId(request);
        boolean isAdmin = request.isUserInRole(ROLE_ADMIN);
        try {
            queueService.replaceEntry(userId, entryId, UUID.fromString(newSongId), isAdmin);
            redirectAttributes.addFlashAttribute(FLASH, "Entry replaced.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute(ERROR, ex.getMessage());
        }
        return REDIRECT_QUEUE;
    }

    private void addCurrentUserAttributes(Model model, HttpServletRequest request) {
        boolean isAdmin = request.isUserInRole(ROLE_ADMIN);
        String userId = UserContext.getCurrentUserId(request);
        String username = isAdmin
                ? props.admin().username()
                : UserContext.getCurrentUsername(request).orElse(null);
        model.addAttribute("currentUserId", userId);
        model.addAttribute("currentUsername", username);
        model.addAttribute("currentUserColor", UserContext.getColorForUserId(userId));
        model.addAttribute("usernameSet", username != null && !username.isBlank());
    }

    private String resolveUsername(HttpServletRequest request, String submittedUsername, boolean isAdmin) {
        if (isAdmin) {
            return props.admin().username();
        }

        String currentUsername = UserContext.getCurrentUsername(request).orElse(null);
        if (currentUsername != null && !currentUsername.isBlank()) {
            validateNonAdminUsername(currentUsername);
            return currentUsername;
        }

        if (submittedUsername == null || submittedUsername.isBlank()) {
            throw new BusinessException("Please choose a username before queuing a song.");
        }

        String normalized = submittedUsername.trim();
        validateNonAdminUsername(normalized);
        return normalized;
    }

    private void validateNonAdminUsername(String username) {
        if (username.equalsIgnoreCase(props.admin().username())) {
            throw new BusinessException("This username is reserved. Please choose a different one.");
        }
    }
}
