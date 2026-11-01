package expensemanagement.service;

import expensemanagement.dao.BudgetDAO;
import expensemanagement.dao.CategoryDAO;
import expensemanagement.dao.ExpenseDAO;
import expensemanagement.exception.ValidationException;
import expensemanagement.model.Budget;
import expensemanagement.model.Category;
import expensemanagement.model.Expense;
import expensemanagement.model.User;
import expensemanagement.util.InputHelper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Manages monthly budgets and calculates category usage and alert thresholds. */
public class BudgetService {
    private static final BigDecimal EIGHTY = new BigDecimal("80");
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private final BudgetDAO budgetDAO;
    private final ExpenseDAO expenseDAO;
    private final CategoryDAO categoryDAO;

    /** Creates the service using its persistence dependencies. */
    public BudgetService(BudgetDAO budgetDAO, ExpenseDAO expenseDAO, CategoryDAO categoryDAO) {
        this.budgetDAO = budgetDAO;
        this.expenseDAO = expenseDAO;
        this.categoryDAO = categoryDAO;
    }

    /** Saves or replaces a user's category limit for a calendar month. */
    public void setBudget(User user, long categoryId, BigDecimal limit, int month, int year) {
        InputHelper.positiveMoney(limit, "Monthly budget");
        if (month < 1 || month > 12 || year < 2000 || year > 9999) {
            throw new ValidationException("Choose a valid budget month and year.");
        }
        if (categoryDAO.findAll(user.id()).stream().noneMatch(category -> category.id() == categoryId)) {
            throw new ValidationException("Select a category belonging to your account.");
        }
        budgetDAO.save(user.id(), categoryId, limit, month, year);
    }

    /** Builds usage details and alert states for a selected month. */
    public List<BudgetStatus> statuses(User user, int month, int year) {
        List<Budget> budgets = budgetDAO.findAll(user.id(), month, year);
        List<Expense> expenses = expenseDAO.findAll(user.id()).stream()
                .filter(expense -> expense.expenseDate().getMonthValue() == month
                        && expense.expenseDate().getYear() == year).toList();
        List<Category> categories = categoryDAO.findAll(user.id());
        List<BudgetStatus> statuses = new ArrayList<>();
        for (Budget budget : budgets) {
            BigDecimal spent = expenses.stream()
                    .filter(expense -> expense.categoryId() == budget.categoryId())
                    .map(Expense::amount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            String name = categories.stream().filter(category -> category.id() == budget.categoryId())
                    .map(Category::name).findFirst().orElse("Unavailable category");
            statuses.add(new BudgetStatus(budget, name, spent, usagePercent(spent, budget.monthlyLimit()),
                    alertLevel(spent, budget.monthlyLimit())));
        }
        return statuses;
    }

    /** Calculates a rounded percentage, allowing values above 100 for overspending. */
    public static BigDecimal usagePercent(BigDecimal spent, BigDecimal limit) {
        if (limit == null || limit.signum() <= 0) {
            throw new ValidationException("Budget limit must be positive.");
        }
        return spent.multiply(ONE_HUNDRED).divide(limit, 2, RoundingMode.HALF_UP);
    }

    /** Returns the semantic threshold state used by alerts and the progress bar. */
    public static AlertLevel alertLevel(BigDecimal spent, BigDecimal limit) {
        BigDecimal percentage = usagePercent(spent, limit);
        if (percentage.compareTo(ONE_HUNDRED) >= 0) {
            return AlertLevel.OVER;
        }
        if (percentage.compareTo(EIGHTY) >= 0) {
            return AlertLevel.WARNING;
        }
        return AlertLevel.OK;
    }

    /** Returns total currently allocated budget for the month. */
    public BigDecimal totalBudget(User user, int month, int year) {
        return budgetDAO.findAll(user.id(), month, year).stream().map(Budget::monthlyLimit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Returns allocated monthly budget minus spending; a negative result indicates overspending. */
    public BigDecimal remainingBudget(User user, int month, int year) {
        BigDecimal spent = expenseDAO.findAll(user.id()).stream()
                .filter(expense -> expense.expenseDate().getMonthValue() == month
                        && expense.expenseDate().getYear() == year)
                .map(Expense::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        return totalBudget(user, month, year).subtract(spent);
    }

    /** Deletes the selected month/category budget. */
    public void deleteBudget(User user, long categoryId, int month, int year) {
        budgetDAO.delete(user.id(), categoryId, month, year);
    }

    /** Budget usage summary and threshold state. */
    public record BudgetStatus(Budget budget, String categoryName, BigDecimal spent,
                               BigDecimal percentage, AlertLevel alertLevel) { }

    /** Threshold states for a monthly budget. */
    public enum AlertLevel { OK, WARNING, OVER }
}
