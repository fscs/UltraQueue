package de.hhu.fscs.ultraqueue.service;

import de.hhu.fscs.ultraqueue.config.UltraQueueProperties;
import de.hhu.fscs.ultraqueue.dto.QueueEntryDto;
import de.hhu.fscs.ultraqueue.exception.BusinessException;
import de.hhu.fscs.ultraqueue.exception.NotFoundException;
import de.hhu.fscs.ultraqueue.model.PlayedSongLog;
import de.hhu.fscs.ultraqueue.model.QueueEntry;
import de.hhu.fscs.ultraqueue.model.Song;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class QueueService {

    private final UltraQueueProperties props;
    private final SongCatalogService catalog;
    private final Clock clock;
    private final ReentrantLock lock = new ReentrantLock(); // protects queue + log

    // In‑memory holders
    private final List<QueueEntry> queue = new LinkedList<>(); // ordered FIFO
    private final List<PlayedSongLog> playedLog = new ArrayList<>();
    private final Map<String, UUID> userToEntry = new ConcurrentHashMap<>(); // cookie → entry id

    @Autowired
    public QueueService(UltraQueueProperties props, SongCatalogService catalog) {
        this(props, catalog, Clock.systemDefaultZone());
    }

    public QueueService(UltraQueueProperties props, SongCatalogService catalog, Clock clock) {
        this.props = props;
        this.catalog = catalog;
        this.clock = clock;
    }

    public void addSong(String userId, UUID songId, boolean isAdmin) {
        lock.lock();
        try {
            // enforce “only one song per user”
            if (props.onlyOneSongPerUser() && userToEntry.containsKey(userId) && !isAdmin) {
                throw new BusinessException("You already have a song in the queue.");
            }
            
            Song song = getSongByIdOrElseThrow(songId);

            songMayBeQueuedOrElseThrow(songId, isAdmin);

            QueueEntry entry = new QueueEntry(UUID.randomUUID(), song, userId, queue.size() + 1);
            queue.add(entry);
            userToEntry.put(userId, entry.getId());
        } finally {
            lock.unlock();
        }
    }

    private void songNotRecentlyPlayedOrElseThrow(UUID songId, Instant now) {
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
    }

    public void removeEntry(String userId, UUID entryId, boolean isAdmin) { // TODO not working from frontend
        lock.lock();
        try {
            QueueEntry entry = findEntry(entryId);
            if (!isAdmin && !entry.getUserId().equals(userId)) {
                throw new AccessDeniedException("Cannot delete another user’s entry");
            }
            queue.remove(entry);
            reOrderPositions();
            userToEntry.entrySet().removeIf(e -> e.getValue().equals(entryId));
        } finally {
            lock.unlock();
        }
    }

    private void reOrderPositions() {
        // TODO move to Fachobjekt
        for (int i = 0; i < queue.size(); i++) {
            queue.get(i).setPosition(i + 1);
        }
    }

    public void replaceEntry(String userId, UUID entryId, UUID newSongId, boolean isAdmin) {
        lock.lock();
        try {
            QueueEntry old = findEntry(entryId);
            if (!old.getUserId().equals(userId) && !isAdmin) {
                throw new AccessDeniedException("Can replace only your own entry");
            }
            
            Song newSong = getSongByIdOrElseThrow(newSongId);

            songMayBeQueuedOrElseThrow(newSongId, isAdmin);

            old.setSong(newSong);
        } finally {
            lock.unlock();
        }
    }

    private @NonNull Song getSongByIdOrElseThrow(UUID newSongId) {
        return catalog.findById(newSongId)
                .orElseThrow(() -> new NotFoundException("Song not found"));
    }

    private void songMayBeQueuedOrElseThrow(UUID newSongId, boolean isAdmin) {
        if (songIsInQueue(newSongId)) {
            throw new BusinessException("Song already in queue");
        }

        if(!isAdmin) {
            songNotRecentlyPlayedOrElseThrow(newSongId, Instant.now(clock));
        }
    }

    private boolean songIsInQueue(UUID newSongId) {
        return queue.stream().anyMatch(e -> e.getSong().id().equals(newSongId));
    }

    /** Called by the UltraStar engine when a song finishes */
    public void markFinished(UUID songId) {
        lock.lock();
        try {
            Instant now = Instant.now(clock);
            // remember song to prevent it being sung again in the near future
            playedLog.add(new PlayedSongLog(songId, now));
            // remove the entry from the user's list of songs
            userToEntry.entrySet().removeIf(e -> {
                QueueEntry qe = findEntry(e.getValue());
                return qe != null && qe.getSong().id().equals(songId);
            });
            // remove the entry from the queue
            queue.removeIf(e -> e.getSong().id().equals(songId));
            reOrderPositions();
        } finally {
            lock.unlock();
        }
    }

    public String getNextSongTitle() {
        lock.lock();
        try {
            if (queue.isEmpty()) return "";
            return queue.getFirst().getSong().title();
        } finally {
            lock.unlock();
        }
    }

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
            Instant now = Instant.now(clock);
            // the first song is usually currently playing, so to have estimates who are _not too far_ in the future, just assume that song is already over
            long cumulatedSec = queue.isEmpty() ? 0 : -queue.getFirst().getSong().getLengthSeconds();
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
     * @throws NotFoundException if the song does not exist.
     */
    public UUID resolveSongId(String title, String artist) {
        return catalog.findByTitleArtist(title, artist)
                .orElseThrow(() -> new NotFoundException(
                        "Song not found in catalog: %s – %s".formatted(title, artist)))
                .id();
    }
}
