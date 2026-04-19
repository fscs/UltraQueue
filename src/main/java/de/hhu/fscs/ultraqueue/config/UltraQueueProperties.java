package de.hhu.fscs.ultraqueue.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * All external settings are bound to this record.
 *
 * <p>Spring Boot will create the bean from the canonical constructor
 * (the only constructor that a record has). Validation annotations are
 * applied on the constructor parameters.
 *
 * <p>Configuration is read from {@code application.yml} (or other
 * Spring property sources) under the prefix {@code ultraqueue}.
 */
@Validated                         // enables JSR‑380 validation on the constructor args
@ConfigurationProperties(prefix = "ultraqueue")
public record UltraQueueProperties(

        /**
         * Absolute folder that contains the UltraStar song directories.
         * Example: {@code C:/UltraStarSongs}
         */
        @NotBlank String songFolder,

        /**
         * Default “max songs per user” (functional requirement #1).
         * Must be ≥ 0; a value of 0 means “no limit”.
         */
        @Min(0) int maxSongsPerUser,

        /**
         * Minimum number of minutes that must pass before the same song
         * can be queued again (functional requirement #2).
         */
        @Min(0) int minIntervalMinutes,

        /**
         * Pagination defaults (page size).  Nested configuration is
         * represented by a nested record.
         */
        @NotNull Pagination pagination,

        /**
         * Admin credentials.  Because the spec says “hard‑coded in
         * application.properties”, we keep them as plain strings.
         * In a real product you would hash them.
         */
        @NotNull Admin admin

) {

    /**
     * Nested record for pagination settings.
     */
    public record Pagination(
            @Min(1) int pageSize   // default is set in the configuration file
    ) {}

    /**
     * Nested record for the single admin user.
     */
    public record Admin(
            @NotBlank String username,
            @NotBlank String password
    ) {}
}