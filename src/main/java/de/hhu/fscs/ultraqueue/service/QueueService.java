package de.hhu.fscs.ultraqueue.service;

import de.hhu.fscs.ultraqueue.dto.QueueEntryDto;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
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

    public void addSong(String userId, UUID songId) {
        lock.lock();
        try {
            // 1. enforce “max songs per user”
            if (userToEntry.containsKey(userId) && props.getMaxSongsPerUser() == 1) {
                throw new BusinessException("You already have a song in the queue");
            }
            // 2. song must exist
            Song song = catalog.getSong(songId)
                    .orElseThrow(() -> new NotFoundException("Song not found"));
            // 3. song must not already be queued
            if (queue.stream().anyMatch(e -> e.getSong().getId().equals(songId))) {
                throw new BusinessException("Song already in queue");
            }
            // 4. 60‑minute repeat rule
            Instant now = Instant.now();
            PlayedSongLog recent = playedLog.stream()
                    .filter(l -> l.getSongId().equals(songId))
                    .max(Comparator.comparing(PlayedSongLog::getPlayedAt))
                    .orElse(null);
            if (recent != null) {
                long minutes = Duration.between(recent.getPlayedAt(), now).toMinutes();
                if (minutes < props.getMinIntervalMinutes()) {
                    throw new BusinessException(
                            "Song was sung %d minutes ago – wait %d more minutes".formatted(
                                    minutes, props.getMinIntervalMinutes() - minutes));
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
            old.setSong(catalog.getSong(newSongId)
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
            return queue.get(0).getSong().getTitle();
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

    public List<QueueEntryDto> getQueueWithEstimates() {
        lock.lock();
        try {
            List<QueueEntryDto> result = new ArrayList<>();
            Instant now = Instant.now();
            long cumulatedSec = 0;
            for (QueueEntry e : queue) {
                cumulatedSec += e.getSong().getLengthSec();
                Instant estimate = now.plusSeconds(cumulatedSec);
                result.add(new QueueEntryDto(e, estimate));
            }
            return result;
        } finally {
            lock.unlock();
        }
    }

    // inside QueueService (add near other public methods)

    public String getNextSongTitle() {
        lock.lock();
        try {
            if (queue.isEmpty()) {
                return "";
            }
            return queue.get(0).getSong().getTitle();  // plain text, as required
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
