package expensemanagement.service;

import expensemanagement.dao.ExpenseDAO;
import expensemanagement.model.Expense;
import expensemanagement.model.User;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Builds dashboard and report summaries from a user's expenses. */
public class ReportService {
    private final ExpenseDAO expenseDAO;

    /** Creates the service using its persistence dependency. */
    public ReportService(ExpenseDAO expenseDAO) {
        this.expenseDAO = expenseDAO;
    }

    /** Returns all expenses for a requested month. */
    public List<Expense> monthlyExpenses(User user, YearMonth month) {
        return expenseDAO.findFiltered(user.id(), null, null, month.atDay(1), month.atEndOfMonth(),
                null, null);
    }

    /** Returns all expenses for a requested calendar year. */
    public List<Expense> yearlyExpenses(User user, int year) {
        return expenseDAO.findFiltered(user.id(), null, null, LocalDate.of(year, 1, 1),
                LocalDate.of(year, 12, 31), null, null);
    }

    /** Totals expenses by category for a period. */
    public Map<String, BigDecimal> categoryTotals(List<Expense> expenses) {
        Map<String, BigDecimal> totals = new LinkedHashMap<>();
        expenses.stream().sorted((a, b) -> a.categoryName().compareToIgnoreCase(b.categoryName()))
                .forEach(expense -> totals.merge(expense.categoryName(), expense.amount(), BigDecimal::add));
        return totals;
    }

    /** Returns a compact monthly report summary. */
    public Summary summary(List<Expense> expenses) {
        BigDecimal total = expenses.stream().map(Expense::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        String topCategory = categoryTotals(expenses).entrySet().stream()
                .max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse("No spending");
        return new Summary(total, expenses.size(), topCategory);
    }

    /** Returns totals for the current month and the preceding five months. */
    public Map<YearMonth, BigDecimal> lastSixMonths(User user) {
        YearMonth current = YearMonth.now();
        LocalDate start = current.minusMonths(5).atDay(1);
        List<Expense> expenses = expenseDAO.findFiltered(user.id(), null, null, start, current.atEndOfMonth(),
                null, null);
        Map<YearMonth, BigDecimal> totals = new LinkedHashMap<>();
        for (int offset = 5; offset >= 0; offset--) {
            totals.put(current.minusMonths(offset), BigDecimal.ZERO);
        }
        for (Expense expense : expenses) {
            YearMonth month = YearMonth.from(expense.expenseDate());
            totals.merge(month, expense.amount(), BigDecimal::add);
        }
        return totals;
    }

    /** Recent activity, limited to the requested number of rows. */
    public List<Expense> recentExpenses(User user, int limit) {
        if (limit < 1) {
            throw new IllegalArgumentException("Limit must be positive.");
        }
        return expenseDAO.findAll(user.id()).stream().limit(limit).toList();
    }

    /** Aggregate report metrics. */
    public record Summary(BigDecimal total, int transactionCount, String topCategory) { }
}
