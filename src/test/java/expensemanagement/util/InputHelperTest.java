package expensemanagement.util;

import expensemanagement.exception.ValidationException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Unit tests for common form validation. */
class InputHelperTest {
    @Test
    void emailIsTrimmedAndNormalized() {
        assertEquals("person@example.com", InputHelper.email(" Person@Example.com "));
    }

    @Test
    void malformedEmailIsRejected() {
        assertThrows(ValidationException.class, () -> InputHelper.email("not-an-email"));
    }

    @Test
    void moneyWithMoreThanTwoDecimalsIsRejected() {
        assertThrows(ValidationException.class,
                () -> InputHelper.positiveMoney(new BigDecimal("1.234"), "Amount"));
    }

    @Test
    void negativeMoneyIsRejected() {
        assertThrows(ValidationException.class,
                () -> InputHelper.positiveMoney(new BigDecimal("-1.00"), "Amount"));
    }
}
