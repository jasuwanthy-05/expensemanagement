package expensemanagement.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Unit tests for salted password hashing. */
class PasswordUtilTest {
    @Test
    void hashVerifiesOriginalPassword() {
        String hash = PasswordUtil.hash("correct-horse");
        assertTrue(PasswordUtil.verify("correct-horse", hash));
    }

    @Test
    void differentPasswordsDoNotVerify() {
        String hash = PasswordUtil.hash("correct-horse");
        assertFalse(PasswordUtil.verify("incorrect-horse", hash));
    }

    @Test
    void eachHashUsesAUniqueSalt() {
        assertFalse(PasswordUtil.hash("same-password").equals(PasswordUtil.hash("same-password")));
    }
}
