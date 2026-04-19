package de.hhu.fscs.ultraqueue.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Incoming JSON from UltraStar:
 *   {"title":"My Song","artist":"The Band"}
 */
public record SongFinishedDto(
        @NotBlank String title,
        @NotBlank String artist) {
}
