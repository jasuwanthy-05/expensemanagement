package expensemanagement.model;

import java.math.BigDecimal;

/** A monthly expense template that is generated once per due month. */
public record RecurringExpense(long id, long userId, String title, BigDecimal amount, long categoryId,
                               int dueDay, String notes, boolean active) { }
