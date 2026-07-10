package de.hhu.fscs.ultraqueue.persistence.implementation.db.dto;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Duration;
import java.util.UUID;

@Table("songs")
public record SongDto (
        @Id UUID songId,
        String title,
        String artist,
        String language, // optional, may be empty
        Integer year,    // optional, may be null
        int length, // length of the song in seconds (rounded)
        String genre,
        String titleArtist
)

{
}
