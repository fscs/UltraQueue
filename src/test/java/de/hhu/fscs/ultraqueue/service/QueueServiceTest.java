package de.hhu.fscs.ultraqueue.service;

import de.hhu.fscs.ultraqueue.config.UltraQueueProperties;
import de.hhu.fscs.ultraqueue.config.UltraQueuePropertiesBuilder;
import de.hhu.fscs.ultraqueue.exception.BusinessException;
import de.hhu.fscs.ultraqueue.model.Song;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

class QueueServiceTest {

    private QueueService queueService;
    private UltraQueueProperties props;

    private Song song1;
    private Song song2;

    @BeforeEach
    void setUp() {
        SongCatalogService catalog = Mockito.mock(SongCatalogService.class);
        props = new UltraQueuePropertiesBuilder()
                .maxSongsPerUser(1)
                .minIntervalMinutes(0)
                .build();
        queueService = new QueueService(props, catalog);

        song1 = new Song.Builder()
                .title("Song 1")
                .artist("Artist 1")
                .length(Duration.ofSeconds(180))
                .build();

        song2 = new Song.Builder()
                .title("Song 2")
                .artist("Artist 2")
                .length(Duration.ofSeconds(200))
                .build();

        when(catalog.findById(song1.id())).thenReturn(Optional.of(song1));
        when(catalog.findById(song2.id())).thenReturn(Optional.of(song2));
    }

    @Test
    @DisplayName("adding two different songs, getting the next songs and dequeuing them works as expected")
    void testAddingAndDequeuing() {
        queueService.addSong("user1", song1.id());
        queueService.addSong("user2", song2.id());

        assertThat(queueService.getNextSongTitle()).isEqualTo("Song 1");
        queueService.markFinished(song1.id());

        assertThat(queueService.getNextSongTitle()).isEqualTo("Song 2");
        queueService.markFinished(song2.id());

        assertThat(queueService.getNextSongTitle()).isEmpty();
    }

    @Test
    @DisplayName("adding two songs from the same user fails")
    void testAddingTwoSongsFromSameUserFails() {
        // Ensure config is set (it is in setUp, but being explicit here for the requirement)
        assertThat(props.maxSongsPerUser()).isEqualTo(1);

        queueService.addSong("user1", song1.id());

        UUID song2Id = song2.id();
        assertThatThrownBy(() -> queueService.addSong("user1", song2Id))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already have a song");
    }

    @Test
    @DisplayName("adding a song twice from two different users fails")
    void testAddingSameSongTwiceFails() {
        UUID song1Id = song1.id();
        queueService.addSong("user1", song1Id);

        assertThatThrownBy(() -> queueService.addSong("user2", song1Id))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already in queue");
    }

    @Test
    @DisplayName("adding a song, singing it, and adding another one from the same users works")
    void testAddingSongAfterFinishingWorks() {
        queueService.addSong("user1", song1.id());
        queueService.markFinished(song1.id());

        queueService.addSong("user1", song2.id());

        assertThat(queueService.getNextSongTitle()).isEqualTo("Song 2");
    }
}
