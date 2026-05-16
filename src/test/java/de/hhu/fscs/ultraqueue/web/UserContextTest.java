package de.hhu.fscs.ultraqueue.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserContextTest {

    private static final String SECRET = "unit-test-cookie-secret";

    @Test
    @DisplayName("signed cookie keeps user id + username and validates signature")
    void signedCookieRoundTrip() {
        String signed = UserContext.buildSignedCookieValue("user-123", "A|B", SECRET);

        assertThat(UserContext.hasValidSignature(signed, SECRET)).isTrue();

        String payload = UserContext.extractPayloadFromSignedCookieValue(signed);
        assertThat(UserContext.extractUserIdFromCookieValue(payload)).isEqualTo("user-123");
        assertThat(UserContext.extractUsernameFromCookieValue(payload)).isEqualTo("A|B");
    }

    @Test
    @DisplayName("tampered cookie signature is rejected")
    void tamperedCookieRejected() {
        String signed = UserContext.buildSignedCookieValue("user-123", "Alice", SECRET);
        String tampered = signed.replace("Alice", "Mallory");

        // Keep the tamper deterministic without depending on encoded text shape.
        if (tampered.equals(signed)) {
            tampered = signed.substring(0, signed.length() - 1)
                    + (signed.endsWith("A") ? "B" : "A");
        }

        assertThat(UserContext.hasValidSignature(tampered, SECRET)).isFalse();
    }
}

