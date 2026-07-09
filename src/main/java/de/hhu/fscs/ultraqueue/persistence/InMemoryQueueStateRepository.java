package de.hhu.fscs.ultraqueue.persistence;

import de.hhu.fscs.ultraqueue.model.PlayedSongLog;
import de.hhu.fscs.ultraqueue.model.QueueEntry;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * In-memory implementation of queue state persistence.
 * State is lost on application restart.
 */
@Primary
@Repository
public class InMemoryQueueStateRepository implements QueueStateRepository {

    private List<QueueEntry> queue = new LinkedList<>();
    private List<PlayedSongLog> playedLog = new ArrayList<>();

    @Override
    public QueueState loadQueue() {
        // On first load, return empty state; subsequent calls return current state
        return new QueueState(
                new LinkedList<>(queue),
                new ArrayList<>(playedLog)
        );
    }

    @Override
    public void saveQueue(QueueState state) {
        this.queue = new LinkedList<>(state.queue());
        this.playedLog = new ArrayList<>(state.playedLog());
    }
}

