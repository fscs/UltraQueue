package de.hhu.fscs.ultraqueue.model;

import de.hhu.fscs.ultraqueue.exception.BusinessException;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory aggregate that owns all mutable queue state and queue mechanics.
 * Business policies (permissions/rules) are applied by the service layer.
 */
public final class SongQueue {

    private final List<QueueEntry> queue = new LinkedList<>();
    private final List<PlayedSongLog> playedLog = new ArrayList<>();
    private final Map<String, UUID> userToEntry = new ConcurrentHashMap<>();

    public int size() {
        return queue.size();
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    public boolean hasEntryForUser(String userId) {
        return userToEntry.containsKey(userId);
    }

    public boolean hasSong(UUID songId) {
        return queue.stream().anyMatch(e -> e.getSong().id().equals(songId));
    }

    public void enqueue(QueueEntry entry) {
        entry.setPosition(queue.size() + 1);
        queue.add(entry);
        userToEntry.put(entry.getUserId(), entry.getId());
    }

    public Optional<QueueEntry> findEntry(UUID entryId) {
        return queue.stream().filter(e -> e.getId().equals(entryId)).findFirst();
    }

    public void removeEntry(UUID entryId) {
        queue.removeIf(e -> e.getId().equals(entryId));
        userToEntry.entrySet().removeIf(e -> e.getValue().equals(entryId));
        reOrderPositions();
    }

    public void replaceSong(UUID entryId, Song song) {
        findEntry(entryId).ifPresent(entry -> entry.setSong(song));
    }

    public void markFinished(UUID songId, Instant playedAt) {
        playedLog.add(new PlayedSongLog(songId, playedAt));

        queue.stream()
                .filter(e -> e.getSong().id().equals(songId))
                .forEach(e -> userToEntry.remove(e.getUserId()));

        queue.removeIf(e -> e.getSong().id().equals(songId));
        reOrderPositions();
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
}


