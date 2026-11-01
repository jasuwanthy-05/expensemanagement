package expensemanagement.util;

import expensemanagement.model.Expense;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Writes expense report data as UTF-8 CSV. */
public final class CSVExporter {
    private CSVExporter() { }

    /** Exports expenses with a header, escaping quotes and delimiters as required by CSV. */
    public static void exportExpenses(Path destination, List<Expense> expenses) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(destination, StandardCharsets.UTF_8)) {
            writer.write("Date,Title,Category,Amount,Notes");
            writer.newLine();
            for (Expense expense : expenses) {
                writer.write(String.join(",",
                        quote(expense.expenseDate().toString()),
                        quote(expense.title()),
                        quote(expense.categoryName()),
                        quote(expense.amount().toPlainString()),
                        quote(expense.notes() == null ? "" : expense.notes())));
                writer.newLine();
            }
        }
    }

    private static String quote(String value) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
