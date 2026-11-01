package expensemanagement.exception;

/** Wraps a database failure with a message suitable for application-level handling. */
public class DatabaseException extends RuntimeException {
    /** Creates a database error retaining the original cause. */
    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
