package de.hhu.fscs.ultraqueue;

import de.hhu.fscs.ultraqueue.model.Song;
import de.hhu.fscs.ultraqueue.service.SongCatalogService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Choose a username")))
                .andExpect(content().string(containsString("name=\"username\"")));
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

    @Test
    @DisplayName("Sorting by title DESC should be passed to service")
    void testSorting() throws Exception {
        Mockito.when(catalog.findAll(any())).thenReturn(new PageImpl<>(Collections.emptyList()));

        mvc.perform(get("/").param("sort", "title").param("dir", "DESC"))
                .andExpect(status().isOk());

        Mockito.verify(catalog).findAll(org.mockito.ArgumentMatchers.argThat(pageable -> {
            var order = pageable.getSort().getOrderFor("title");
            return order != null && order.isDescending();
        }));
    }

    @Test
    @DisplayName("Pagination should be passed correctly")
    void testPagination() throws Exception {
        List<Song> songs = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            songs.add(new Song.Builder().title("Song " + i).artist("Artist").length(Duration.ofSeconds(180)).build());
        }
        
        // Mock returning page 1 (second page)
        PageRequest pageable = PageRequest.of(1, 10);
        Mockito.when(catalog.findAll(org.mockito.ArgumentMatchers.any(Pageable.class)))
                .thenReturn(new PageImpl<>(songs.subList(10, 20), pageable, 20));

        mvc.perform(get("/").param("page", "1").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Page 2 of 2")));
    }
    @Test
    @DisplayName("Search query and sorting should be passed to service")
    void testSearchAndSorting() throws Exception {
        Mockito.when(catalog.search(any(), any())).thenReturn(new PageImpl<>(Collections.emptyList()));

        mvc.perform(get("/").param("query", "test").param("sort", "artist").param("dir", "DESC"))
                .andExpect(status().isOk());

        Mockito.verify(catalog).search(eq("test"), org.mockito.ArgumentMatchers.argThat(pageable -> {
            var order = pageable.getSort().getOrderFor("artist");
            return order != null && order.isDescending();
        }));
    }
}

