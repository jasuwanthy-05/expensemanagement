package expensemanagement.service;

import expensemanagement.exception.ValidationException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Unit tests for monthly budget usage and alert thresholds. */
class BudgetServiceTest {
    @Test
    void usagePercentCalculatesRoundedPercentage() {
        assertEquals(new BigDecimal("37.50"),
                BudgetService.usagePercent(new BigDecimal("75"), new BigDecimal("200")));
    }

    @Test
    void eightyPercentIsWarning() {
        assertEquals(BudgetService.AlertLevel.WARNING,
                BudgetService.alertLevel(new BigDecimal("80"), new BigDecimal("100")));
    }

    @Test
    void belowEightyPercentIsOnTrack() {
        assertEquals(BudgetService.AlertLevel.OK,
                BudgetService.alertLevel(new BigDecimal("79.99"), new BigDecimal("100")));
    }

    @Test
    void exactlyOneHundredPercentIsOverLimit() {
        assertEquals(BudgetService.AlertLevel.OVER,
                BudgetService.alertLevel(new BigDecimal("100"), new BigDecimal("100")));
    }

    @Test
    void spendingAboveLimitHasPercentageAboveOneHundred() {
        assertEquals(new BigDecimal("125.00"),
                BudgetService.usagePercent(new BigDecimal("125"), new BigDecimal("100")));
    }

    @Test
    void zeroBudgetIsRejected() {
        assertThrows(ValidationException.class,
                () -> BudgetService.usagePercent(new BigDecimal("10"), BigDecimal.ZERO));
    }
}
