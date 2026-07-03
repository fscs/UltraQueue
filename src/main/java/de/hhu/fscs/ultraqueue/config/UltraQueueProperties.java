package de.hhu.fscs.ultraqueue.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "ultraqueue")
public record UltraQueueProperties(
        @NotBlank String songFolder,
        boolean onlyOneSongPerUser,
        @Min(0) int minIntervalMinutes,
        @NotNull Pagination pagination,
        @NotNull Admin admin,
        @NotNull Cookie cookie,
        @NotNull PrivilegedUser privilegedUser
) {

    public record Pagination(
            @Min(1) int pageSize
    ) {}

    public record Admin(
            @NotBlank String username,
            @NotBlank String password
    ) {}

    public record Cookie(
            @NotBlank String signingSecret
    ) {}

    public record PrivilegedUser(
            @NotBlank String username,
            @NotBlank String password
    ) {}
}