package de.hhu.fscs.ultraqueue.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a requested resource (song, queue entry, etc.) does not exist.
 *
 * <p>It is a {@link RuntimeException} (unchecked) because callers in the
 * service layer do not have to declare it in their method signatures.
 *
 * <p>The {@link ResponseStatus} annotation tells Spring MVC to translate the
 * exception into an HTTP 404 response automatically.
 *
 * <p>If you later need a machine‑readable error code, you can add a field
 * and a corresponding getter – the class is deliberately tiny so you can
 * extend it without breaking existing code.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class NotFoundException extends RuntimeException {

    /** Simple constructor – only a message is required. */
    public NotFoundException(String message) {
        super(message);
    }

    /** Constructor that also stores a cause (e.g. a downstream IOException). */
    public NotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}