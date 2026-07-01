package de.hhu.fscs.ultraqueue.model;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable value object that holds the meta‑information of a single UltraStar song.
 * Instances are created once during the start‑up scan and never modified.
 */
public record Song(
        UUID id,
        String title,
        String artist,
        String language, // optional, may be empty
        Integer year,    // optional, may be null
        Duration length, // length of the song in seconds (rounded)
        String genre,
        String coverPath

) {

    public Song {
        Objects.requireNonNull(id);
        Objects.requireNonNull(title);
        Objects.requireNonNull(artist);
        Objects.requireNonNull(length);
    }

    public static class Builder {
        private final UUID id = UUID.randomUUID();
        private String title;
        private String artist;
        private String language;
        private Integer year;
        private Duration length;
        private String genre;
        private String coverPath;

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

        public Builder coverPath(String coverPath) {
            this.coverPath = coverPath;
            return this;
        }

        public Song build() {
            return new Song(id, title, artist, language, year, length, genre, coverPath);
        }
    }

    // -----------------------------------------------------------------
    // Convenience methods for UI / API
    // -----------------------------------------------------------------
    public long getLengthSeconds() {
        return length.getSeconds();
    }

    @Override
    public String toString() {
        return "%s %s %s %s %d %s".formatted(
                title, artist, language != null ? language : "", 
                genre != null ? genre : "", year != null ? year : 0, 
                id.toString());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Song song)) return false;
        return Objects.equals(id, song.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
