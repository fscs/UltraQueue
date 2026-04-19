package de.hhu.fscs.ultraqueue.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a song currently waiting in the global UltraQueue.
 * The object is **mutable only for the position** – everything else is final.
 */
public final class QueueEntry {

    private final UUID   id;        // primary key of this queue element
    private Song   song;      // immutable reference to catalogue song
    private final String userId;    // UUID stored in the user‑id cookie
    private final Instant queuedAt; // when the entry was created
    private int position;           // 1‑based index inside the queue; mutable

    public QueueEntry(UUID id, Song song, String userId, Instant queuedAt, int position) {
        this.id = Objects.requireNonNull(id);
        this.song = Objects.requireNonNull(song);
        this.userId = Objects.requireNonNull(userId);
        this.queuedAt = Objects.requireNonNull(queuedAt);
        this.position = position;
    }

    // -----------------------------------------------------------------
    // Getters and Setters
    // -----------------------------------------------------------------
    public UUID getId()          { return id; }
    public Song getSong()        { return song; }

    public void setSong(Song song) {
        this.song = Objects.requireNonNull(song);
    }

    public String getUserId()    { return userId; }
    public Instant getQueuedAt(){ return queuedAt; }
    public int getPosition()     { return position; }

    // Only the queue service may change the ordering.
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
                        song.getTitle(),
                        song.getArtist(),
                        userId,
                        position);
    }
}