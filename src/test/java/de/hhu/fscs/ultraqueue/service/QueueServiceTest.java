package de.hhu.fscs.ultraqueue.service;

import de.hhu.fscs.ultraqueue.config.UltraQueueProperties;
import de.hhu.fscs.ultraqueue.config.UltraQueuePropertiesBuilder;
import de.hhu.fscs.ultraqueue.exception.BusinessException;
import de.hhu.fscs.ultraqueue.model.Song;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

class QueueServiceTest {

    private QueueService queueService;
    private UltraQueueProperties props;
    private Clock clock;
    private Instant baseTime;

    private Song song1;
    private Song song2;

    private SongCatalogService catalog;

    @BeforeEach
    void setUp() {
        catalog = Mockito.mock(SongCatalogService.class);
        props = new UltraQueuePropertiesBuilder()
                .onlyOneSongPerUser(true)
                .minIntervalMinutes(0)
                .build();
        
        baseTime = Instant.parse("2024-05-20T10:00:00Z");
        clock = Mockito.mock(Clock.class);
        when(clock.instant()).thenReturn(baseTime);
        when(clock.getZone()).thenReturn(ZoneId.of("UTC"));

        queueService = new QueueService(props, catalog, clock);

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
        queueService.addSong("user1", "User One", song1.id(), false);
        queueService.addSong("user2", "User Two", song2.id(), false);

        assertThat(queueService.getQueueWithEstimates("user1").getFirst().username()).isEqualTo("User One");
        assertThat(queueService.getQueueWithEstimates("user1").getFirst().userColor()).matches("#[0-9a-fA-F]{6}");

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
        assertThat(props.onlyOneSongPerUser()).isTrue();

        queueService.addSong("user1", "User One", song1.id(), false);

        UUID song2Id = song2.id();
        assertThatThrownBy(() -> queueService.addSong("user1", "User One", song2Id, false))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already have");
    }

    @Test
    @DisplayName("non-admin users may not queue using the reserved admin username")
    void testReservedAdminUsernameRejected() {
        assertThatThrownBy(() -> queueService.addSong("user1", "admin", song1.id(), false))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("reserved");
    }

    @Test
    @DisplayName("replacing a song works and keeps position")
    void testReplaceSongWorks() {
        queueService.addSong("user1", "User One", song1.id(), false);
        queueService.addSong("user2", "User Two", song2.id(), false);

        UUID entry2Id = queueService.getQueueWithEstimates("user2").get(1).entryId();
        
        Song song3 = new Song.Builder()
                .title("Song 3")
                .artist("Artist 3")
                .length(Duration.ofSeconds(210))
                .build();
        
        when(catalog.findById(song3.id())).thenReturn(Optional.of(song3));
        
        queueService.replaceEntry("user2", entry2Id, song3.id(), false);

        var queue = queueService.getQueueWithEstimates("user2");
        assertThat(queue.get(1).title()).isEqualTo("Song 3");
        assertThat(queue.get(1).position()).isEqualTo(2);
        assertThat(queue.get(1).username()).isEqualTo("User Two");
    }

    @Test
    @DisplayName("replacing with a song already in queue fails")
    void testReplaceDuplicateSongFails() {
        queueService.addSong("user1", "User One", song1.id(), false);
        queueService.addSong("user2", "User Two", song2.id(), false);

        UUID entry2Id = queueService.getQueueWithEstimates("user2").get(1).entryId();
        UUID song1Id = song1.id();

        assertThatThrownBy(() -> queueService.replaceEntry("user2", entry2Id, song1Id, false))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already in queue");
    }

    @Test
    @DisplayName("adding two songs from the same user is ok for admins")
    void testAddingTwoSongsFromSameUserAdmin() {
        // Ensure config is set (it is in setUp, but being explicit here for the requirement)
        assertThat(props.onlyOneSongPerUser()).isTrue();

        queueService.addSong("user1", "User One", song1.id(), false);
        queueService.addSong("user1", "Admin", song2.id(), true);

        assertThat(queueService.getQueueWithEstimates("")).hasSize(2);
    }

    @Test
    @DisplayName("adding a song twice from two different users fails")
    void testAddingSameSongTwiceFails() {
        UUID song1Id = song1.id();
        queueService.addSong("user1", "User One", song1Id, false);

        assertThatThrownBy(() -> queueService.addSong("user2", "User Two", song1Id, false))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already in queue");
    }

    @Test
    @DisplayName("adding a song, singing it, and adding another one from the same users works")
    void testAddingSongAfterFinishingWorks() {
        queueService.addSong("user1", "User One", song1.id(), false);
        queueService.markFinished(song1.id());

        queueService.addSong("user1", "User One", song2.id(), false);

        assertThat(queueService.getNextSongTitle()).isEqualTo("Song 2");
    }

    @Test
    @DisplayName("song may repeat for admins")
    void testSongRepetitionIntervalAdmin() {
        // Given a 5 minute interval
        props = new UltraQueuePropertiesBuilder()
                .minIntervalMinutes(5)
                .build();
        // re-setup queueService with new props
        SongCatalogService mockCatalog = Mockito.mock(SongCatalogService.class);
        UUID song1Id = song1.id();
        when(mockCatalog.findById(song1Id)).thenReturn(Optional.of(song1));
        queueService = new QueueService(props, mockCatalog, clock);

        // When song 1 is added and finished at baseTime
        queueService.addSong("user1", "User One", song1Id, false);
        queueService.markFinished(song1Id);

        // Then adding it again 4 minutes later fails
        Instant fourMinutesLater = baseTime.plus(Duration.ofMinutes(4));
        when(clock.instant()).thenReturn(fourMinutesLater);

        queueService.addSong("user2", "Admin", song1Id, true);

        assertThat(queueService.getNextSongTitle()).isEqualTo("Song 1");
    }

    @Test
    @DisplayName("finished a song re-numbers songs")
    void testRenumberAfterFinish() {
        queueService.addSong("user1", "User One", song1.id(), false);
        queueService.addSong("user2", "User Two", song2.id(), false);
        queueService.markFinished(song1.id());

        assertThat(queueService.getNextSongTitle()).isEqualTo(song2.title());
        assertThat(queueService.getQueueWithEstimates("user1").getFirst().position()).isOne();
    }

    @Test
    @DisplayName("song may not repeat within 5 minutes")
    void testSongRepetitionInterval() {
        // Given a 5 minute interval
        props = new UltraQueuePropertiesBuilder()
                .minIntervalMinutes(5)
                .build();
        // re-setup queueService with new props
        SongCatalogService mockCatalog = Mockito.mock(SongCatalogService.class);
        UUID song1Id = song1.id();
        when(mockCatalog.findById(song1Id)).thenReturn(Optional.of(song1));
        queueService = new QueueService(props, mockCatalog, clock);

        // When song 1 is added and finished at baseTime
        queueService.addSong("user1", "User One", song1Id, false);
        queueService.markFinished(song1Id);

        // Then adding it again 4 minutes later fails
        Instant fourMinutesLater = baseTime.plus(Duration.ofMinutes(4));
        when(clock.instant()).thenReturn(fourMinutesLater);

        assertThatThrownBy(() -> queueService.addSong("user2", "User Two", song1Id, false))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("wait");

        // But adding it 5 minutes later works
        Instant fiveMinutesLater = baseTime.plus(Duration.ofMinutes(5));
        when(clock.instant()).thenReturn(fiveMinutesLater);

        queueService.addSong("user2", "User Two", song1Id, false);
        assertThat(queueService.getNextSongTitle()).isEqualTo("Song 1");
    }

    @Test
    @DisplayName("user A cannot remove a song of user B")
    void testUserCannotRemoveOthersSong() {
        queueService.addSong("userB", "User B", song1.id(), false);
        UUID entryId = queueService.getQueueWithEstimates("userB").getFirst().entryId();

        assertThatThrownBy(() -> queueService.removeEntry("userA", entryId, false))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class)
                .hasMessageContaining("Cannot delete another user’s entry");
    }

    @Test
    @DisplayName("user A can add new song after admin removed their song")
    void testAddAfterAdminDelete() {
        queueService.addSong("userB", "User B", song1.id(), false);
        UUID entryId = queueService.getQueueWithEstimates("userB").getFirst().entryId();
        queueService.removeEntry("userA", entryId, true);
        queueService.addSong("userB", "User B", song2.id(), false);

        assertThat(queueService.getNextSongTitle()).isEqualTo("Song 2");
    }

    @Test
    @DisplayName("song may not repeat within 5 minutes, even when set using the replacement feature")
    void testSongRepetitionIntervalWithReplacement() {
        // Given a 5 minute interval
        props = new UltraQueuePropertiesBuilder()
                .minIntervalMinutes(5)
                .build();
        // re-setup queueService with new props
        SongCatalogService mockCatalog = Mockito.mock(SongCatalogService.class);
        UUID song1Id = song1.id();
        when(mockCatalog.findById(song1Id)).thenReturn(Optional.of(song1));
        queueService = new QueueService(props, mockCatalog, clock);

        // When song 1 is added and finished at baseTime
        queueService.addSong("user1", "User One", song1Id, false);
        queueService.markFinished(song1Id);

        UUID song2Id = song2.id();
        when(mockCatalog.findById(song2Id)).thenReturn(Optional.of(song2));
        queueService.addSong("user2", "User Two", song2Id, false);

        // Then adding song 1 again 4 minutes later fails even when using replacement
        UUID entryId = queueService.getQueueWithEstimates("x").getFirst().entryId();
        Instant fourMinutesLater = baseTime.plus(Duration.ofMinutes(4));
        when(clock.instant()).thenReturn(fourMinutesLater);

        assertThatThrownBy(() -> queueService.replaceEntry("user2", entryId, song1Id, false))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("wait");

        // But replacing it 5 minutes later works
        Instant fiveMinutesLater = baseTime.plus(Duration.ofMinutes(5));
        when(clock.instant()).thenReturn(fiveMinutesLater);

        queueService.replaceEntry("user2", entryId, song1Id, false);
        assertThat(queueService.getNextSongTitle()).isEqualTo("Song 1");
    }

    @Test
    @DisplayName("song may not be in queue twice, even when set using the replacement feature")
    void testSongRepetitionWithReplacement() {
        props = new UltraQueuePropertiesBuilder()
                .onlyOneSongPerUser(true)
                .build();
        // re-setup queueService with new props
        SongCatalogService mockCatalog = Mockito.mock(SongCatalogService.class);
        UUID song1Id = song1.id();
        when(mockCatalog.findById(song1Id)).thenReturn(Optional.of(song1));
        queueService = new QueueService(props, mockCatalog, clock);

        // When song 1 is added
        queueService.addSong("user1", "User One", song1Id, false);

        UUID song2Id = song2.id();
        when(mockCatalog.findById(song2Id)).thenReturn(Optional.of(song2));
        queueService.addSong("user2", "User Two", song2Id, false);

        // Then adding song 1 again fails even when using replacement
        UUID entryId = queueService.getQueueWithEstimates("x").get(1).entryId();
        Instant fourMinutesLater = baseTime.plus(Duration.ofMinutes(4));
        when(clock.instant()).thenReturn(fourMinutesLater);

        assertThatThrownBy(() -> queueService.replaceEntry("user2", entryId, song1Id, false))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already");
    }
}
