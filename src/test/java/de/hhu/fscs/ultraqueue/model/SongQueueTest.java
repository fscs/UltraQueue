package de.hhu.fscs.ultraqueue.model;

import de.hhu.fscs.ultraqueue.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SongQueueTest {

    @Test
    @DisplayName("remove entry reorders positions and clears user ownership")
    void removeEntryReordersAndClearsOwner() {
        SongQueue queue = new SongQueue();
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
        SongQueue queue = new SongQueue();
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
        SongQueue queue = new SongQueue();
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
        SongQueue queue = new SongQueue();
        Song song1 = song("Song 1", "Artist 1");

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

    private Song song(String title, String artist) {
        return new Song.Builder()
                .title(title)
                .artist(artist)
                .length(Duration.ofSeconds(180))
                .build();
    }

    private QueueEntry entry(Song song, String userId, String username) {
        return new QueueEntry(UUID.randomUUID(), song, userId, username, "#123456", 1);
    }
}


