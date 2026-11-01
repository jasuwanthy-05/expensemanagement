package expensemanagement.exception;

/** Raised when user-provided data does not meet application rules. */
public class ValidationException extends RuntimeException {
    /** Creates a validation error with a user-readable message. */
    public ValidationException(String message) {
        super(message);
    }
}
