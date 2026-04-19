package de.hhu.fscs.ultraqueue.repository;

import de.hhu.fscs.ultraqueue.model.QueueEntry;

import java.util.List;

public interface QueueRepository {
    List<QueueEntry> findAll();
    void save(QueueEntry e);
    void delete(QueueEntry e);
}
