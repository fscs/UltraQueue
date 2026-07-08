package de.hhu.fscs.ultraqueue;

import de.hhu.fscs.ultraqueue.service.QueueService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ApiControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    QueueService queueService;

    @Test
    @DisplayName("GET /nextsong returns plain title from service")
    void nextSongReturnsTitle() throws Exception {
        Mockito.when(queueService.getNextSongTitleAndArtist()).thenReturn("My Next Song");

        mvc.perform(get("/nextsong"))
                .andExpect(status().isOk())
                .andExpect(content().string("My Next Song"));
    }

    @Test
    @DisplayName("POST /songfinished resolves id and marks song as finished")
    void songFinishedDelegatesToService() throws Exception {
        UUID songId = UUID.randomUUID();
        Mockito.when(queueService.resolveSongId("Title A", "Artist A")).thenReturn(songId);

        mvc.perform(post("/songfinished")
                        .contentType("application/json")
                        .content("{\"title\":\"Title A\",\"artist\":\"Artist A\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string("OK"));

        Mockito.verify(queueService).resolveSongId("Title A", "Artist A");
        Mockito.verify(queueService).markFinished(songId);
    }

    @Test
    @DisplayName("POST /songfinished returns error for invalid payload")
    void songFinishedValidationFailure() throws Exception {
        mvc.perform(post("/songfinished")
                        .contentType("application/json")
                        .content("{\"title\":\"\",\"artist\":\"Artist A\"}"))
                .andExpect(status().isInternalServerError());

        Mockito.verifyNoInteractions(queueService);
    }
}


