package de.hhu.fscs.ultraqueue.model;

import de.hhu.fscs.ultraqueue.exception.BusinessException;
import de.hhu.fscs.ultraqueue.persistence.implementation.memory.InMemoryQueueStateRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class SongQueueTest {

    private SongQueue createQueue() {
        return new SongQueue(new InMemoryQueueStateRepository());
    }

    @Test
    @DisplayName("remove entry reorders positions and clears user ownership")
    void removeEntryReordersAndClearsOwner() {
        SongQueue queue = createQueue();
        Song song1 = song("Song 1", "Artist 1");
        Song song2 = song("Song 2", "Artist 2");

        QueueEntry first = entry(song1, "user1", "User One");
        QueueEntry second = entry(song2, "user2", "User Two");
        queue.enqueue(first);
        queue.enqueue(second);

        queue.removeEntry(first.getId());

        assertThat(queue.entriesSnapshot()).hasSize(1);
        assertThat(queue.entriesSnapshot().getFirst().getPosition()).isEqualTo(1);
        assertThat(queue.hasEntryForUser("user1")).isFalse();
    }

    @Test
    @DisplayName("replace song updates the selected queue entry")
    void replaceSongUpdatesEntry() {
        SongQueue queue = createQueue();
        Song song1 = song("Song 1", "Artist 1");
        Song song2 = song("Song 2", "Artist 2");

        QueueEntry entry = entry(song1, "user1", "User One");
        queue.enqueue(entry);

        queue.replaceSong(entry.getId(), song2);

        assertThat(queue.findEntry(entry.getId())).isPresent();
        assertThat(queue.findEntry(entry.getId()).orElseThrow().getSong().id()).isEqualTo(song2.id());
    }

    @Test
    @DisplayName("mark finished logs play and removes matching queue entries")
    void markFinishedLogsAndRemovesEntries() {
        SongQueue queue = createQueue();
        Song song1 = song("Song 1", "Artist 1");
        Song song2 = song("Song 2", "Artist 2");

        queue.enqueue(entry(song1, "user1", "User One"));
        queue.enqueue(entry(song2, "user2", "User Two"));

        Instant playedAt = Instant.parse("2024-05-20T10:00:00Z");
        queue.markFinished(song1.id(), playedAt);

        assertThat(queue.entriesSnapshot()).hasSize(1);
        assertThat(queue.entriesSnapshot().getFirst().getSong().id()).isEqualTo(song2.id());
        assertThat(queue.mostRecentPlayedLogForSong(song1.id())).isPresent();
        assertThat(queue.hasEntryForUser("user1")).isFalse();
    }

    @Test
    @DisplayName("recently played check enforces minimum interval")
    void recentlyPlayedCheckEnforcesInterval() {
        SongQueue queue = createQueue();
        Song song1 = song("Song 1", "Artist 1");
        queue.enqueue(entry(song1, "user1", "User One"));

        Instant playedAt = Instant.parse("2024-05-20T10:00:00Z");
        queue.markFinished(song1.id(), playedAt);

        assertThatThrownBy(() -> queue.songNotRecentlyPlayedOrElseThrow(
                song1.id(),
                playedAt.plus(Duration.ofMinutes(4)),
                5))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("wait 1 more minutes");

        queue.songNotRecentlyPlayedOrElseThrow(song1.id(), playedAt.plus(Duration.ofMinutes(5)), 5);
    }

    @Test
    @DisplayName("a song which wasn't queued should not block queuing later (relevant if a song is played during tech set-up testing)")
    void test1() {
        SongQueue queue = createQueue();
        Song song1 = song("Song 1", "Artist 1");

        Instant playedAt = Instant.parse("2024-05-20T10:00:00Z");
        try {
            queue.markFinished(song1.id(), playedAt);
        } catch(Exception _) {
            // markFinished throws if song wasn't queued
        }

        assertThatCode(() -> queue.songNotRecentlyPlayedOrElseThrow(song1.id(), playedAt.plus(Duration.ofMinutes(4)),5)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("positions are calculated on-the-fly from queue index")
    void positionsCalculatedOnTheFly() {
        SongQueue queue = createQueue();
        Song song1 = song("Song 1", "Artist 1");
        Song song2 = song("Song 2", "Artist 2");
        Song song3 = song("Song 3", "Artist 3");

        QueueEntry entry1 = entry(song1, "user1", "User One");
        QueueEntry entry2 = entry(song2, "user2", "User Two");
        QueueEntry entry3 = entry(song3, "user3", "User Three");
        queue.enqueue(entry1);
        queue.enqueue(entry2);
        queue.enqueue(entry3);

        assertThat(queue.entriesSnapshot()).hasSize(3);
        assertThat(queue.entriesSnapshot().get(0).getPosition()).isEqualTo(1);
        assertThat(queue.entriesSnapshot().get(1).getPosition()).isEqualTo(2);
        assertThat(queue.entriesSnapshot().get(2).getPosition()).isEqualTo(3);

        queue.removeEntry(entry2.getId());

        assertThat(queue.entriesSnapshot()).hasSize(2);
        assertThat(queue.entriesSnapshot().get(0).getPosition()).isEqualTo(1);
        assertThat(queue.entriesSnapshot().get(1).getPosition()).isEqualTo(2);
    }

    private Song song(String title, String artist) {
        return new Song.Builder()
                .title(title)
                .artist(artist)
                .length(Duration.ofSeconds(180))
                .build();
    }

    private QueueEntry entry(Song song, String userId, String username) {
        return new QueueEntry(UUID.randomUUID(), song, userId, username, "#123456", 0);
    }
}

