package de.hhu.fscs.ultraqueue.exception;

/**
 * Thrown when a user‑visible business rule is violated.
 *
 * <p>Typical use‑cases in UltraQueue:
 * <ul>
 *   <li>The user already has a queued song (max‑songs‑per‑user).</li>
 *   <li>The requested song is already in the queue.</li>
 *   <li>The song was played less than {@code min‑interval‑minutes} ago.</li>
 *   <li>Any other rule that should be reported to the client as a 400‑Bad‑Request.</li>
 * </ul>
 *
 * <p>The exception is a {@link RuntimeException} because it is unchecked
 * – service methods do not have to declare it in the signature, yet
 * Spring’s {@code @ExceptionHandler} can catch it globally.
 */
public class BusinessException extends RuntimeException {

    /** Simple constructor that just stores the message. */
    public BusinessException(String message) {
        super(message);
    }

    /** Optional constructor that also stores a cause (e.g. a wrapped validation error). */
    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}