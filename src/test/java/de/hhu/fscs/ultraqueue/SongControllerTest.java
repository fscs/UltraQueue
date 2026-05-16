package de.hhu.fscs.ultraqueue;

import de.hhu.fscs.ultraqueue.dto.QueuedSongDetailsDto;
import de.hhu.fscs.ultraqueue.service.QueueService;
import de.hhu.fscs.ultraqueue.service.SongCatalogService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
class SongControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    QueueService queueService;

    @MockitoBean
    SongCatalogService songCatalogService;

    @Test
    @DisplayName("GET /song/{id} renders metadata and lyrics")
    void songPageRendersMetadataAndLyrics() throws Exception {
        UUID entryId = UUID.randomUUID();
        UUID songId = UUID.randomUUID();

        QueuedSongDetailsDto dto = new QueuedSongDetailsDto(
                entryId,
                songId,
                "My Song",
                "My Artist",
                "EN",
                2024,
                "Pop",
                187,
                "20:15",
                "Alice",
                "#112233",
                2
        );

        Mockito.when(queueService.getQueuedSongDetails(entryId)).thenReturn(Optional.of(dto));
        Mockito.when(songCatalogService.findLyricsById(songId)).thenReturn(Optional.of(List.of("line one", "line two")));

        mvc.perform(get("/song/" + entryId))
                .andExpect(status().isOk())
                .andExpect(view().name("song"))
                .andExpect(content().string(containsString("My Song")))
                .andExpect(content().string(containsString("My Artist")))
                .andExpect(content().string(containsString("line one")));
    }

    @Test
    @DisplayName("GET /song/{id} returns 404 for unknown queued song")
    void songPageReturnsNotFoundForUnknownEntry() throws Exception {
        UUID entryId = UUID.randomUUID();
        Mockito.when(queueService.getQueuedSongDetails(entryId)).thenReturn(Optional.empty());

        mvc.perform(get("/song/" + entryId))
                .andExpect(status().isNotFound());
    }
}

