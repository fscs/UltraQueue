package de.hhu.fscs.ultraqueue.controller;

import de.hhu.fscs.ultraqueue.config.UltraQueueProperties;
import de.hhu.fscs.ultraqueue.web.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.ui.Model;

class Common {
    static final String ROLE_ADMIN = "ADMIN";

    private Common() { /* utility */ }

    static void addCurrentUserAttributes(Model model, HttpServletRequest request, UltraQueueProperties props) {
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
}
