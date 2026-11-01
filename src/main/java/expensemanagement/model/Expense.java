package expensemanagement.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A single expense transaction. */
public record Expense(long id, long userId, String title, BigDecimal amount, long categoryId,
                      String categoryName, LocalDate expenseDate, String notes) { }
