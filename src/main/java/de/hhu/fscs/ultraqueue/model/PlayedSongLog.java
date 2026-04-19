package de.hhu.fscs.ultraqueue.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable record of a song that has already been performed.
 * The class is deliberately tiny – we only need the song identifier
 * and the moment it finished.  More fields (score, player, etc.) can
 * be added later without touching the queue logic.
 *
 * @param songId   the Song that was played
 * @param playedAt when UltraStar reported the finish
 */
public record PlayedSongLog(UUID songId, Instant playedAt) {
}