package expensemanagement.model;

import java.time.LocalDateTime;

/** An authenticated application user. */
public record User(long id, String name, String email, String passwordHash, LocalDateTime createdAt) { }
