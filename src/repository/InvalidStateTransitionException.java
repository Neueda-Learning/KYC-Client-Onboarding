package repository;

/**
 * Thrown when a case status change violates the onboarding case state machine.
 */
public class InvalidStateTransitionException extends RuntimeException {

    public InvalidStateTransitionException(String message) {
        super(message);
    }
}
