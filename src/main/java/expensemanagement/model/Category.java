package expensemanagement.model;

/** A user-owned expense category. */
public record Category(long id, String name, long userId, String color) { }
