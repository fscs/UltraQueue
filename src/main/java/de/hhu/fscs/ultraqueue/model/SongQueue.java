package de.hhu.fscs.ultraqueue.model;

import de.hhu.fscs.ultraqueue.exception.BusinessException;
import de.hhu.fscs.ultraqueue.persistence.QueueStateRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * In-memory aggregate that owns all mutable queue state and queue mechanics.
 * Business policies (permissions/rules) are applied by the service layer.
 * State is persisted via injected repository.
 */
public final class SongQueue {

    private final QueueStateRepository repository;
    private final List<QueueEntry> queue;
    private final List<PlayedSongLog> playedLog;
    private Instant nextSongStarted;
    private long currentSongLengthSeconds;

    public SongQueue(QueueStateRepository repository) {
        this.repository = repository;
        QueueStateRepository.QueueState state = repository.loadQueue();
        this.queue = new LinkedList<>(state.queue());
        this.playedLog = new ArrayList<>(state.playedLog());
    }

    public int size() {
        return queue.size();
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    public long getCurrentSongLengthSeconds() {
        return currentSongLengthSeconds;
    }

    /**
     * O(n) lookup, ok for normal use
     */
    public boolean hasEntryForUser(String userId) {
        return queue.stream().anyMatch(e -> e.getUserId().equals(userId));
    }

    public boolean hasSong(UUID songId) {
        return queue.stream().anyMatch(e -> e.getSong().id().equals(songId));
    }

    public void enqueue(QueueEntry entry) {
        entry.setPosition(queue.size() + 1);
        queue.add(entry);
        persist();
    }

    public Optional<QueueEntry> findEntry(UUID entryId) {
        return queue.stream().filter(e -> e.getId().equals(entryId)).findFirst();
    }

    public void removeEntry(UUID entryId) {
        queue.removeIf(e -> e.getId().equals(entryId));
        reOrderPositions();
        persist();
    }

    public void replaceSong(UUID entryId, Song song) {
        findEntry(entryId).ifPresent(entry -> entry.setSong(song));
        persist();
    }

    public void markFinished(UUID songId, Instant playedAt) {
        if(!queue.removeIf(e -> e.getSong().id().equals(songId))) {
            throw new IllegalStateException("Song not in queue");
        }
        playedLog.add(new PlayedSongLog(songId, playedAt));

        reOrderPositions();
        persist();
    }

    public Optional<PlayedSongLog> mostRecentPlayedLogForSong(UUID songId) {
        return playedLog.stream()
                .filter(l -> l.songId().equals(songId))
                .max(Comparator.comparing(PlayedSongLog::playedAt));
    }

    public void songNotRecentlyPlayedOrElseThrow(UUID songId, Instant now, int minIntervalMinutes) {
        PlayedSongLog recent = mostRecentPlayedLogForSong(songId).orElse(null);
        if (recent == null) {
            return;
        }

        long minutes = Duration.between(recent.playedAt(), now).toMinutes();
        if (minutes < minIntervalMinutes) {
            throw new BusinessException(
                    "Song was sung %d minutes ago – wait %d more minutes".formatted(
                            minutes, minIntervalMinutes - minutes));
        }
    }

    public String nextSongTitle() {
        if (queue.isEmpty()) {
            return "";
        }
        return queue.getFirst().getSong().title();
    }

    public List<QueueEntry> entriesSnapshot() {
        return List.copyOf(queue);
    }

    private void reOrderPositions() {
        for (int i = 0; i < queue.size(); i++) {
            queue.get(i).setPosition(i + 1);
        }
    }

    private void persist() {
        repository.saveQueue(new QueueStateRepository.QueueState(
                new ArrayList<>(queue),
                new ArrayList<>(playedLog)));
    }

    public Instant getQueueStartedAt() {
        return nextSongStarted;
    }

    public void setNextSongStarted(Instant nextSongStarted) {
        this.nextSongStarted = nextSongStarted;
        if (!queue.isEmpty()) {
            this.currentSongLengthSeconds = queue.getFirst().getSong().getLengthSeconds();
        }
    }

    public Instant getRawEstimatedStart(QueueEntry entry, Instant anchor) {

        long cumulative = 0;

        for (QueueEntry e : queue) {

            if (e.equals(entry)) {
                break;
            }

            cumulative += e.getSong().getLengthSeconds();
        }

        Instant base = (anchor != null) ? anchor : Instant.now();

        return base.plusSeconds(cumulative);
    }
}


