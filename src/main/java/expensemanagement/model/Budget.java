package expensemanagement.model;

import java.math.BigDecimal;

/** A category spending limit for a calendar month. */
public record Budget(long id, long userId, long categoryId, BigDecimal monthlyLimit, int month, int year) { }
