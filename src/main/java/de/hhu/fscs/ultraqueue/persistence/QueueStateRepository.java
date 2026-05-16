package de.hhu.fscs.ultraqueue.persistence;

import de.hhu.fscs.ultraqueue.model.PlayedSongLog;
import de.hhu.fscs.ultraqueue.model.QueueEntry;

import java.util.List;

/**
 * Abstraction for persisting and loading queue state.
 * Implementations can be in-memory, database, or event-sourced.
 */
public interface QueueStateRepository {

    /**
     * Load the current queue and played song log from storage.
     */
    QueueState loadQueue();

    /**
     * Persist the current queue and played song log.
     */
    void saveQueue(QueueState state);

    /**
     * Snapshot of queue state at a point in time.
     */
    record QueueState(List<QueueEntry> queue, List<PlayedSongLog> playedLog) {
    }
}

