package expensemanagement.util;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

/** Date formatting and period helpers used by views and reports. */
public final class DateUtil {
    private static final DateTimeFormatter DISPLAY = DateTimeFormatter.ofPattern("MMM d, yyyy");

    private DateUtil() { }

    /** Formats a date for user-facing tables. */
    public static String display(LocalDate date) {
        return date == null ? "" : date.format(DISPLAY);
    }

    /** Returns the current month range, inclusive. */
    public static LocalDate[] currentMonthRange() {
        YearMonth month = YearMonth.now();
        return new LocalDate[]{month.atDay(1), month.atEndOfMonth()};
    }
}
