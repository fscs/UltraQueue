package de.hhu.fscs.ultraqueue.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Represents a song currently waiting in the global UltraQueue.
 */
public final class QueueEntry {

    private final UUID id;        // primary key of this queue element
    private Song song;              // reference to catalogue song, can be replaced
    private final String userId;    // UUID stored in the user‑id cookie
    private int position;           // 1‑based index inside the queue; mutable

    public QueueEntry(UUID id, Song song, String userId, int position) {
        this.id = Objects.requireNonNull(id);
        this.song = Objects.requireNonNull(song);
        this.userId = Objects.requireNonNull(userId);
        this.position = position;
    }

    public UUID getId() {
        return id;
    }

    public Song getSong() {
        return song;
    }

    public void setSong(Song song) {
        this.song = Objects.requireNonNull(song);
    }

    public String getUserId() {
        return userId;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        if (position < 1) {
            throw new IllegalArgumentException("Position must be >= 1");
        }
        this.position = position;
    }

    // -----------------------------------------------------------------
    // Equals / hashCode – based on the immutable primary key
    // -----------------------------------------------------------------
    @Override
    public boolean equals(Object o) {
        return (o instanceof QueueEntry qe) && id.equals(qe.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "QueueEntry{id=%s, title='%s', artist='%s', user=%s, pos=%d}"
                .formatted(id,
                        song.title(),
                        song.artist(),
                        userId,
                        position);
    }
}