package de.hhu.fscs.ultraqueue.repository;

public interface QueueRepository {
    List<QueueEntry> findAll();
    void save(QueueEntry e);
    void delete(QueueEntry e);
}
