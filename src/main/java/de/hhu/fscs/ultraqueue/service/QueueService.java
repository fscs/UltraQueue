package de.hhu.fscs.ultraqueue.service;

import de.hhu.fscs.ultraqueue.config.UltraQueueProperties;
import de.hhu.fscs.ultraqueue.dto.QueuedSongDetailsDto;
import de.hhu.fscs.ultraqueue.dto.QueueEntryDto;
import de.hhu.fscs.ultraqueue.exception.BusinessException;
import de.hhu.fscs.ultraqueue.exception.NotFoundException;
import de.hhu.fscs.ultraqueue.model.QueueEntry;
import de.hhu.fscs.ultraqueue.model.Song;
import de.hhu.fscs.ultraqueue.model.SongQueue;
import de.hhu.fscs.ultraqueue.persistence.QueueStateRepository;
import de.hhu.fscs.ultraqueue.web.UserContext;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class QueueService {

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault());

    private final UltraQueueProperties props;
    private final SongCatalogService catalog;
    private final Clock clock;
    private final ReentrantLock lock = new ReentrantLock(); // protects queue + log

    private final SongQueue songQueue;

    @Autowired
    public QueueService(UltraQueueProperties props, SongCatalogService catalog,
                        QueueStateRepository repository) {
        this(props, catalog, Clock.systemDefaultZone(), repository);
    }

    public QueueService(UltraQueueProperties props, SongCatalogService catalog, Clock clock,
                        QueueStateRepository repository) {
        this.props = props;
        this.catalog = catalog;
        this.clock = clock;
        this.songQueue = new SongQueue(repository);
    }

    public void addSong(String userId, String username, UUID songId, boolean isAdmin) {
        lock.lock();
        try {
            String normalizedUsername = normalizeUsername(username);
            if (!isAdmin && normalizedUsername.equalsIgnoreCase(props.admin().username())) {
                throw new BusinessException("This username is reserved. Please choose a different one.");
            }

            // enforce “only one song per user”
            if (props.onlyOneSongPerUser() && songQueue.hasEntryForUser(userId) && !isAdmin) {
                throw new BusinessException("You already have a song in the queue.");
            }

            Song song = getSongByIdOrElseThrow(songId);

            if (!isAdmin) {
                songMayBeQueuedOrElseThrow(songId);
            }

            QueueEntry entry = new QueueEntry(UUID.randomUUID(), song, userId,
                    normalizedUsername, UserContext.getColorForUserId(userId), songQueue.size() + 1);
            songQueue.enqueue(entry);
        } finally {
            lock.unlock();
        }
    }

    private String normalizeUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new BusinessException("Username is required");
        }
        return username.trim();
    }

    public void removeEntry(String userId, UUID entryId, boolean isAdmin) {
        lock.lock();
        try {
            QueueEntry entry = findEntry(entryId);
            if (!isAdmin && !entry.getUserId().equals(userId)) {
                throw new AccessDeniedException("Cannot delete another user’s entry");
            }
            songQueue.removeEntry(entryId);
        } finally {
            lock.unlock();
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

            if (!isAdmin) {
                songMayBeQueuedOrElseThrow(newSongId);
            }

            songQueue.replaceSong(entryId, newSong);
        } finally {
            lock.unlock();
        }
    }

    private @NonNull Song getSongByIdOrElseThrow(UUID newSongId) {
        return catalog.findById(newSongId)
                .orElseThrow(() -> new NotFoundException("Song not found"));
    }

    private void songMayBeQueuedOrElseThrow(UUID newSongId) {
        if (songIsInQueue(newSongId)) {
            throw new BusinessException("Song already in queue");
        }

        songQueue.songNotRecentlyPlayedOrElseThrow(newSongId, Instant.now(clock), props.minIntervalMinutes());
    }

    private boolean songIsInQueue(UUID newSongId) {
        return songQueue.hasSong(newSongId);
    }

    /**
     * Called by the UltraStar engine when a song finishes
     */
    public void markFinished(UUID songId) {
        lock.lock();
        try {
            Instant now = Instant.now(clock);
            songQueue.markFinished(songId, now);
        } finally {
            lock.unlock();
        }
    }

    public String getNextSongTitle() {
        lock.lock();
        try {
            return songQueue.nextSongTitle();
        } finally {
            lock.unlock();
        }
    }

    private QueueEntry findEntry(UUID id) {
        return songQueue.findEntry(id)
                .orElseThrow(() -> new NotFoundException("Queue entry not found"));
    }

    public List<QueueEntryDto> getQueueWithEstimates(String currentUserId) {
        lock.lock();
        try {
            List<QueueEntryDto> result = new ArrayList<>();

            List<QueueEntry> queue = songQueue.entriesSnapshot();
            Instant now = Instant.now(clock);

            Instant anchor = songQueue.getQueueStartedAt();

            for (QueueEntry e : queue) {

                Instant estimate = songQueue.getRawEstimatedStart(e, anchor);

                long waitSeconds;

                if (anchor == null) {
                    // queue not started yet → pure countdown
                    waitSeconds = Duration.between(now, estimate).getSeconds();
                } else {
                    // normal + freeze-safe countdown
                    if (anchor.getEpochSecond() <= now.getEpochSecond() - songQueue.getCurrentSongLengthSeconds()) {
                        anchor = Instant.now(clock);
                    }
                    estimate = songQueue.getRawEstimatedStart(e, anchor);
                    long diff = Duration.between(now, estimate).getSeconds();

                    waitSeconds = Math.max(0, diff);
                }

                result.add(QueueEntryDto.of(
                        e,
                        estimate,
                        currentUserId,
                        waitSeconds
                ));
            }

            return result;

        } finally {
            lock.unlock();
        }
    }
    /*
    public List<QueueEntryDto> getQueueWithEstimates(String currentUserId) {
        lock.lock();
        try {
            List<QueueEntryDto> result = new ArrayList<>();

            List<QueueEntry> queue = songQueue.entriesSnapshot();
            Instant now = Instant.now(clock);
            Instant anchor = songQueue.getQueueStartedAt();

            for (QueueEntry e : queue) {
                Instant estimate = songQueue.getRawEstimatedStart(e, anchor);

                long waitSeconds;

                if (anchor == null) {
                    estimate = songQueue.getRawEstimatedStart(e, anchor);
                    waitSeconds = Duration.between(now, estimate).getSeconds();
                } else {
                    if (anchor.getEpochSecond() <= now.getEpochSecond() - queue.getFirst().getSong().getLengthSeconds()) {
                        anchor = Instant.now(clock).minusSeconds(queue.getFirst().getSong().getLengthSeconds());
                    }
                    System.out.println(anchor);
                    estimate = songQueue.getRawEstimatedStart(e, anchor);
                    long diff = Duration.between(now, estimate).getSeconds();

                    waitSeconds = Math.max(0, diff);
                }

                result.add(QueueEntryDto.of(
                        e,
                        estimate,
                        currentUserId,
                        waitSeconds
                ));
            }
            return result;
        } finally {
            lock.unlock();
        }
    }
    */

    public Optional<QueuedSongDetailsDto> getQueuedSongDetails(UUID entryId) {
        lock.lock();
        try {
            Instant now = Instant.now(clock);
            List<QueueEntry> queue = songQueue.entriesSnapshot();
            long cumulatedSec = queue.isEmpty() ? 0 : -queue.getFirst().getSong().getLengthSeconds();
            for (QueueEntry entry : queue) {
                cumulatedSec += entry.getSong().getLengthSeconds();
                if (entry.getId().equals(entryId)) {
                    Instant estimate = now.plusSeconds(cumulatedSec);
                    return Optional.of(new QueuedSongDetailsDto(
                            entry.getId(),
                            entry.getSong().id(),
                            entry.getSong().title(),
                            entry.getSong().artist(),
                            entry.getSong().language(),
                            entry.getSong().year(),
                            entry.getSong().genre(),
                            entry.getSong().getLengthSeconds(),
                            TIME_FORMAT.format(estimate),
                            entry.getUsername(),
                            entry.getUserColor(),
                            entry.getPosition()
                    ));
                }
            }
            return Optional.empty();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Resolve an incoming title+artist pair to the internal Song UUID.
     *
     * @throws NotFoundException if the song does not exist.
     */
    public UUID resolveSongId(String title, String artist) {
        return catalog.findByTitleArtist(title, artist)
                .orElseThrow(() -> new NotFoundException(
                        "Song not found in catalog: %s – %s".formatted(title, artist)))
                .id();
    }

    public void setTimeNextSongStartedToCurrentFirst(Instant time) {
        lock.lock();
        try {
            songQueue.setNextSongStarted(time);
        } finally {
            lock.unlock();
        }
    }
}
