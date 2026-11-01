package expensemanagement.util;

import expensemanagement.exception.ValidationException;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.regex.Pattern;

/** Shared validation for form and service inputs. */
public final class InputHelper {
    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    private static final Pattern COLOR = Pattern.compile("^#[0-9A-Fa-f]{6}$");

    private InputHelper() { }

    /** Validates and returns trimmed non-empty text. */
    public static String requiredText(String value, String label, int maxLength) {
        String text = value == null ? "" : value.trim();
        if (text.isEmpty() || text.length() > maxLength) {
            throw new ValidationException(label + " is required and must be at most " + maxLength + " characters.");
        }
        return text;
    }

    /** Validates an email address and normalizes it to lowercase. */
    public static String email(String value) {
        String text = requiredText(value, "Email", 190).toLowerCase(Locale.ROOT);
        if (!EMAIL.matcher(text).matches()) {
            throw new ValidationException("Enter a valid email address.");
        }
        return text;
    }

    /** Checks that an amount is positive and fits the database DECIMAL(10,2) range. */
    public static BigDecimal positiveMoney(BigDecimal value, String label) {
        if (value == null || value.scale() > 2 || value.signum() <= 0
                || value.compareTo(new BigDecimal("99999999.99")) > 0) {
            throw new ValidationException(label + " must be a positive amount with at most two decimal places.");
        }
        return value;
    }

    /** Validates a CSS-style six-digit hex color. */
    public static String color(String value) {
        String text = requiredText(value, "Color", 7);
        if (!COLOR.matcher(text).matches()) {
            throw new ValidationException("Choose a valid six-digit hex color.");
        }
        return text.toUpperCase();
    }
}
