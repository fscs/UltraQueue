package de.hhu.fscs.ultraqueue.service;

import de.hhu.fscs.ultraqueue.config.UltraQueueProperties;
import de.hhu.fscs.ultraqueue.dto.QueueEntryDto;
import de.hhu.fscs.ultraqueue.exception.BusinessException;
import de.hhu.fscs.ultraqueue.exception.NotFoundException;
import de.hhu.fscs.ultraqueue.model.PlayedSongLog;
import de.hhu.fscs.ultraqueue.model.QueueEntry;
import de.hhu.fscs.ultraqueue.model.Song;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class QueueService {

    private final UltraQueueProperties props;
    private final SongCatalogService catalog;
    private final ReentrantLock lock = new ReentrantLock(); // protects queue + log

    // In‑memory holders
    private final List<QueueEntry> queue = new ArrayList<>(); // ordered FIFO
    private final List<PlayedSongLog> playedLog = new ArrayList<>();
    private final Map<String, UUID> userToEntry = new ConcurrentHashMap<>(); // cookie → entry id

    public QueueService(UltraQueueProperties props, SongCatalogService catalog) {
        this.props = props;
        this.catalog = catalog;
    }

    public void addSong(String userId, UUID songId) {
        lock.lock();
        try {
            // 1. enforce “max songs per user”
            if (userToEntry.containsKey(userId) && props.maxSongsPerUser() == 1) {
                throw new BusinessException("You already have a song in the queue");
            }
            // 2. song must exist
            Song song = catalog.findById(songId)
                    .orElseThrow(() -> new NotFoundException("Song not found"));
            // 3. song must not already be queued
            if (queue.stream().anyMatch(e -> e.getSong().getId().equals(songId))) {
                throw new BusinessException("Song already in queue");
            }
            // 4. 60‑minute repeat rule
            Instant now = Instant.now();
            PlayedSongLog recent = playedLog.stream()
                    .filter(l -> l.songId().equals(songId))
                    .max(Comparator.comparing(PlayedSongLog::playedAt))
                    .orElse(null);
            if (recent != null) {
                long minutes = Duration.between(recent.playedAt(), now).toMinutes();
                if (minutes < props.minIntervalMinutes()) {
                    throw new BusinessException(
                            "Song was sung %d minutes ago – wait %d more minutes".formatted(
                                    minutes, props.minIntervalMinutes() - minutes));
                }
            }
            // 5. all good → create entry
            QueueEntry entry = new QueueEntry(UUID.randomUUID(), song, userId, now, queue.size() + 1);
            queue.add(entry);
            userToEntry.put(userId, entry.getId());
        } finally {
            lock.unlock();
        }
    }

    public void removeEntry(String userId, UUID entryId, boolean isAdmin) {
        lock.lock();
        try {
            QueueEntry entry = findEntry(entryId);
            if (!isAdmin && !entry.getUserId().equals(userId)) {
                throw new AccessDeniedException("Cannot delete another user’s entry");
            }
            queue.remove(entry);
            // re‑number positions
            for (int i = 0; i < queue.size(); i++) {
                queue.get(i).setPosition(i + 1);
            }
            if (!isAdmin) userToEntry.remove(userId);
        } finally {
            lock.unlock();
        }
    }

    public void replaceEntry(String userId, UUID entryId, UUID newSongId) {
        lock.lock();
        try {
            QueueEntry old = findEntry(entryId);
            if (!old.getUserId().equals(userId)) {
                throw new AccessDeniedException("Can replace only your own entry");
            }
            // reuse same validation as addSong (except the max‑per‑user check)
            // … (similar to addSong, but we replace the Song field)
            old.setSong(catalog.findById(newSongId)
                    .orElseThrow(() -> new NotFoundException("Song not found")));
        } finally {
            lock.unlock();
        }
    }

    /** Called by the UltraStar engine when a song finishes */
    public void markFinished(UUID songId) {
        lock.lock();
        try {
            Instant now = Instant.now();
            playedLog.add(new PlayedSongLog(songId, now));
            // also remove the entry from the queue if it is still there
            queue.removeIf(e -> e.getSong().getId().equals(songId));
            // clean up user→entry map
            userToEntry.entrySet().removeIf(e -> {
                QueueEntry qe = findEntry(e.getValue());
                return qe != null && qe.getSong().getId().equals(songId);
            });
        } finally {
            lock.unlock();
        }
    }

    public String getNextSongTitle() {
        lock.lock();
        try {
            if (queue.isEmpty()) return "";
            return queue.getFirst().getSong().getTitle();
        } finally {
            lock.unlock();
        }
    }

    // -----------------------------------------------------------------
    // INTERNAL helpers
    // -----------------------------------------------------------------
    private QueueEntry findEntry(UUID id) {
        return queue.stream()
                .filter(e -> e.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Queue entry not found"));
    }

    public List<QueueEntryDto> getQueueWithEstimates(String currentUserId) {
        lock.lock();
        try {
            List<QueueEntryDto> result = new ArrayList<>();
            Instant now = Instant.now();
            long cumulatedSec = 0;
            for (QueueEntry e : queue) {
                cumulatedSec += e.getSong().getLengthSeconds();
                Instant estimate = now.plusSeconds(cumulatedSec);
                result.add(QueueEntryDto.of(e, estimate, currentUserId));
            }
            return result;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Resolve an incoming title+artist pair to the internal Song UUID.
     * Throws NotFoundException if the song does not exist.
     */
    public UUID resolveSongId(String title, String artist) {
        return catalog.findByTitleArtist(title, artist)
                .orElseThrow(() -> new NotFoundException(
                        "Song not found in catalog: %s – %s".formatted(title, artist)))
                .getId();
    }
}
