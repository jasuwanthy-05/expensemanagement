package expensemanagement.service;

import expensemanagement.dao.CategoryDAO;
import expensemanagement.dao.ExpenseDAO;
import expensemanagement.dao.RecurringExpenseDAO;
import expensemanagement.exception.ValidationException;
import expensemanagement.model.Category;
import expensemanagement.model.Expense;
import expensemanagement.model.RecurringExpense;
import expensemanagement.model.User;
import expensemanagement.util.InputHelper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/** Applies validation and ownership rules to expense and recurring-expense operations. */
public class ExpenseService {
    private final ExpenseDAO expenseDAO;
    private final CategoryDAO categoryDAO;
    private final RecurringExpenseDAO recurringDAO;

    /** Creates the service using its persistence dependencies. */
    public ExpenseService(ExpenseDAO expenseDAO, CategoryDAO categoryDAO, RecurringExpenseDAO recurringDAO) {
        this.expenseDAO = expenseDAO;
        this.categoryDAO = categoryDAO;
        this.recurringDAO = recurringDAO;
    }

    /** Returns expenses matching all provided optional filters. */
    public List<Expense> search(User user, String keyword, Long categoryId, LocalDate from, LocalDate to,
                                BigDecimal minAmount, BigDecimal maxAmount) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new ValidationException("Start date must not be after end date.");
        }
        if (minAmount != null && maxAmount != null && minAmount.compareTo(maxAmount) > 0) {
            throw new ValidationException("Minimum amount must not exceed maximum amount.");
        }
        return expenseDAO.findFiltered(user.id(), keyword, categoryId, from, to, minAmount, maxAmount);
    }

    /** Returns every expense for the current user. */
    public List<Expense> findAll(User user) {
        return expenseDAO.findAll(user.id());
    }

    /** Creates a validated expense. */
    public Expense add(User user, String title, BigDecimal amount, long categoryId,
                       LocalDate date, String notes) {
        Expense expense = validate(user, 0, title, amount, categoryId, date, notes);
        long id = expenseDAO.create(expense);
        return new Expense(id, expense.userId(), expense.title(), expense.amount(), expense.categoryId(),
                categoryName(user.id(), categoryId), expense.expenseDate(), expense.notes());
    }

    /** Updates a validated expense owned by the current user. */
    public void update(User user, long expenseId, String title, BigDecimal amount, long categoryId,
                       LocalDate date, String notes) {
        if (expenseDAO.findById(user.id(), expenseId).isEmpty()) {
            throw new ValidationException("The selected expense is no longer available.");
        }
        expenseDAO.update(validate(user, expenseId, title, amount, categoryId, date, notes));
    }

    /** Deletes a transaction belonging to the current user. */
    public void delete(User user, long expenseId) {
        expenseDAO.delete(user.id(), expenseId);
    }

    /** Creates a monthly recurring template after validating its due day and data. */
    public long addRecurring(User user, String title, BigDecimal amount, long categoryId,
                             int dueDay, String notes) {
        Expense validated = validate(user, 0, title, amount, categoryId, LocalDate.now(), notes);
        if (dueDay < 1 || dueDay > 31) {
            throw new ValidationException("Due day must be between 1 and 31.");
        }
        return recurringDAO.create(new RecurringExpense(0, user.id(), validated.title(), validated.amount(),
                categoryId, dueDay, validated.notes(), true));
    }

    /** Returns the user's recurring expense templates. */
    public List<RecurringExpense> findRecurring(User user) {
        return recurringDAO.findAll(user.id());
    }

    /** Removes a recurring expense template belonging to the current user. */
    public void deleteRecurring(User user, long id) {
        recurringDAO.delete(user.id(), id);
    }

    /** Creates all recurring transactions that have reached their due day this month. */
    public int generateDueRecurring(User user, LocalDate today) {
        return recurringDAO.generateDue(user.id(), today);
    }

    private Expense validate(User user, long id, String title, BigDecimal amount, long categoryId,
                             LocalDate date, String notes) {
        String cleanTitle = InputHelper.requiredText(title, "Title", 160);
        BigDecimal cleanAmount = InputHelper.positiveMoney(amount, "Amount");
        if (date == null) {
            throw new ValidationException("Choose an expense date.");
        }
        if (notes != null && notes.length() > 1000) {
            throw new ValidationException("Notes must be 1000 characters or fewer.");
        }
        String category = categoryName(user.id(), categoryId);
        return new Expense(id, user.id(), cleanTitle, cleanAmount, categoryId, category, date,
                notes == null || notes.isBlank() ? null : notes.trim());
    }

    private String categoryName(long userId, long categoryId) {
        return categoryDAO.findAll(userId).stream()
                .filter(category -> category.id() == categoryId)
                .map(Category::name)
                .findFirst()
                .orElseThrow(() -> new ValidationException("Select a category belonging to your account."));
    }
}
