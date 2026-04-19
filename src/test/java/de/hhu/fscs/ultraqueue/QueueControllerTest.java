package de.hhu.fscs.ultraqueue;

import de.hhu.fscs.ultraqueue.service.QueueService;
import de.hhu.fscs.ultraqueue.service.SongCatalogService;
import de.hhu.fscs.ultraqueue.controller.CatalogController;
import de.hhu.fscs.ultraqueue.controller.QueueController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {CatalogController.class, QueueController.class})
class QueueControllerTest {

    @Autowired MockMvc mvc;

    @MockBean SongCatalogService catalog;
    @MockBean QueueService queueService;
    @MockBean UserContext userContext;

    @Test
    @DisplayName("GET /queue shows empty list when service returns nothing")
    void getEmptyQueue() throws Exception {
        Mockito.when(userContext.getCurrentUserId(any()))
                .thenReturn("test‑user");

        Mockito.when(queueService.getQueueWithEstimates(eq("test‑user")))
                .thenReturn(java.util.Collections.emptyList());

        mvc.perform(get("/queue"))
                .andExpect(status().isOk())
                .andExpect(view().name("queue"))
                .andExpect(content().string(containsString("Current Queue")));
    }

    @Test
    @DisplayName("POST /queue/add delegates to service and redirects")
    void addSongRedirects() throws Exception {
        Mockito.when(userContext.getCurrentUserId(any()))
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