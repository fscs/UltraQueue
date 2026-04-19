package de.hhu.fscs.ultraqueue;

import de.hhu.fscs.ultraqueue.model.Song;
import de.hhu.fscs.ultraqueue.service.SongCatalogService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CatalogControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private SongCatalogService catalog;

    @Test
    void testCatalogPage() throws Exception {
        Song song = new Song.Builder()
                .title("Test Song")
                .artist("Test Artist")
                .length(Duration.ofSeconds(185))
                .build();

        Mockito.when(catalog.findAll(any())).thenReturn(new PageImpl<>(Collections.singletonList(song)));

        mvc.perform(get("/"))
                .andExpect(status().isOk());
    }

    @Test
    void testCatalogPageWithTwoSongs() throws Exception {
        Song song1 = new Song.Builder()
                .title("Song One")
                .artist("Artist One")
                .length(Duration.ofSeconds(180))
                .build();

        Song song2 = new Song.Builder()
                .title("Song Two")
                .artist("Artist Two")
                .length(Duration.ofSeconds(200))
                .build();

        Mockito.when(catalog.findAll(any())).thenReturn(new PageImpl<>(Arrays.asList(song1, song2)));

        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Song One")))
                .andExpect(content().string(containsString("Song Two")));
    }
}

