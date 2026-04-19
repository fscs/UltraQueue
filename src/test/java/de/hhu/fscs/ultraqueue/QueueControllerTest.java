package de.hhu.fscs.ultraqueue;

import de.hhu.fscs.ultraqueue.exception.BusinessException;
import de.hhu.fscs.ultraqueue.service.QueueService;
import de.hhu.fscs.ultraqueue.service.SongCatalogService;
import de.hhu.fscs.ultraqueue.web.UserContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class QueueControllerTest {

    @Autowired MockMvc mvc;

    @MockitoBean
    SongCatalogService catalog;
    @MockitoBean QueueService queueService;

    @Test
    @DisplayName("GET /queue shows empty list when service returns nothing")
    void getEmptyQueue() throws Exception {
        try (MockedStatic<UserContext> userContextMockedStatic = Mockito.mockStatic(UserContext.class)) {
            userContextMockedStatic.when(() -> UserContext.getCurrentUserId(any()))
                    .thenReturn("test-user");

            Mockito.when(queueService.getQueueWithEstimates(eq("test-user")))
                    .thenReturn(java.util.Collections.emptyList());

            mvc.perform(get("/queue"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("queue"))
                    .andExpect(content().string(containsString("Current Queue")));
        }
    }

    @Test
    @DisplayName("POST /queue/add delegates to service and redirects")
    void addSongRedirects() throws Exception {
        try (MockedStatic<UserContext> userContextMockedStatic = Mockito.mockStatic(UserContext.class)) {
            userContextMockedStatic.when(() -> UserContext.getCurrentUserId(any()))
                    .thenReturn("abc-123");

            mvc.perform(post("/queue/add")
                            .param("songId", "d2c1e5f0-1234-4b1a-9a2b-99f150c0e8e9")
                            .with(csrf()))   // Spring Security CSRF token helper
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/queue"));

            Mockito.verify(queueService).addSong(eq("abc-123"),
                    eq(UUID.fromString("d2c1e5f0-1234-4b1a-9a2b-99f150c0e8e9")));
        }
    }

    @Test
    @DisplayName("POST /queue/add should have flash message when successful")
    void addSongShowsFlashMessage() throws Exception {
        try (MockedStatic<UserContext> userContextMockedStatic = Mockito.mockStatic(UserContext.class)) {
            userContextMockedStatic.when(() -> UserContext.getCurrentUserId(any()))
                    .thenReturn("abc-123");

            mvc.perform(post("/queue/add")
                            .param("songId", UUID.randomUUID().toString())
                            .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(flash().attribute("flash", "Song added to your queue."));
        }
    }

    @Test
    @DisplayName("POST /queue/add should have error message when BusinessException occurs")
    void addSongShowsErrorMessage() throws Exception {
        try (MockedStatic<UserContext> userContextMockedStatic = Mockito.mockStatic(UserContext.class)) {
            userContextMockedStatic.when(() -> UserContext.getCurrentUserId(any()))
                    .thenReturn("abc-123");

            Mockito.doThrow(new BusinessException("Song already in queue"))
                    .when(queueService).addSong(eq("abc-123"), any());

            mvc.perform(post("/queue/add")
                            .param("songId", UUID.randomUUID().toString())
                            .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(flash().attribute("error", "Song already in queue"));
        }
    }

    @Test
    @DisplayName("POST /queue/remove should have flash message when successful")
    void removeEntryShowsFlashMessage() throws Exception {
        try (MockedStatic<UserContext> userContextMockedStatic = Mockito.mockStatic(UserContext.class)) {
            userContextMockedStatic.when(() -> UserContext.getCurrentUserId(any()))
                    .thenReturn("abc-123");

            UUID entryId = UUID.randomUUID();
            mvc.perform(post("/queue/remove/" + entryId)
                            .with(user("admin").roles("ADMIN"))
                            .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(flash().attribute("flash", "Entry removed."));
        }
    }

    @Test
    @DisplayName("POST /queue/replace should have flash message when successful")
    void replaceEntryShowsFlashMessage() throws Exception {
        try (MockedStatic<UserContext> userContextMockedStatic = Mockito.mockStatic(UserContext.class)) {
            userContextMockedStatic.when(() -> UserContext.getCurrentUserId(any()))
                    .thenReturn("abc-123");

            UUID entryId = UUID.randomUUID();
            mvc.perform(post("/queue/replace/" + entryId)
                            .param("newSongId", UUID.randomUUID().toString())
                            .with(user("abc-123"))
                            .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(flash().attribute("flash", "Entry replaced."));
        }
    }
}