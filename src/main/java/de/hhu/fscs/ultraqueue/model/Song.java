package de.hhu.fscs.ultraqueue.model;

import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable value object that holds the meta‑information of a single UltraStar song.
 * Instances are created once during the start‑up scan and never modified.
 */
public final class Song { // TODO transform to record

    private final UUID id;
    private final String title;
    private final String artist;
    private final String language;   // optional, may be empty
    private final Integer year;      // optional, may be null
    private final Duration length;   // length of the song in seconds (rounded)
    private final String genre;

    private Song(Builder builder) {
        this.id = Objects.requireNonNull(builder.id);
        this.title = Objects.requireNonNull(builder.title);
        this.artist = Objects.requireNonNull(builder.artist);
        this.language = builder.language;
        this.year = builder.year;
        this.length = Objects.requireNonNull(builder.length);
        this.genre = builder.genre;
    }

    /**
     * Builder pattern – makes construction from the parser tidy.
     */
    public static class Builder {
        private final UUID id = UUID.randomUUID();
        private String title;
        private String artist;
        private String language;
        private Integer year;
        private Duration length;
        private String genre;

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder artist(String artist) {
            this.artist = artist;
            return this;
        }

        public Builder language(String language) {
            this.language = language;
            return this;
        }

        public Builder year(Integer year) {
            this.year = year;
            return this;
        }

        public Builder length(Duration length) {
            this.length = length;
            return this;
        }

        public Builder genre(String genre) {
            this.genre = genre;
            return this;
        }

        public Song build() {
            return new Song(this);
        }
    }

    // -----------------------------------------------------------------
    // Getters (no setters – immutable)
    // -----------------------------------------------------------------
    public UUID getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public String getLanguage() {
        return language;
    }

    public Integer getYear() {
        return year;
    }

    public Duration getLength() {
        return length;
    }

    public String getGenre() {
        return genre;
    }

    // -----------------------------------------------------------------
    // Convenience methods for UI / API
    // -----------------------------------------------------------------
    public long getLengthSeconds() {
        return length.getSeconds();
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof Song s) && id.equals(s.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Song{id=" + id + ", title='" + title + '\'' +
                ", artist='" + artist + '\'' + '}';
    }
}
