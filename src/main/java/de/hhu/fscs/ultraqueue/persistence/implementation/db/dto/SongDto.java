package de.hhu.fscs.ultraqueue.persistence.implementation.db.dto;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Table("songs")
public record SongDto (
        @Id UUID id
)

{
}
