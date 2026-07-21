package com.taskflow.auth.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordPolicyTest {

    @Test
    void rejectsNullAndShortPasswords() {
        assertFalse(PasswordPolicy.isValid(null));
        assertFalse(PasswordPolicy.isValid(""));
        assertFalse(PasswordPolicy.isValid("Aa1!"));
    }

    @Test
    void rejectsPasswordsMissingClass() {
        assertFalse(PasswordPolicy.isValid("alllower1!"));   // no uppercase
        assertFalse(PasswordPolicy.isValid("ALLUPPER1!"));   // no lowercase
        assertFalse(PasswordPolicy.isValid("NoDigits!!"));   // no digit
        assertFalse(PasswordPolicy.isValid("NoSpecial1"));   // no special char
    }

    @Test
    void acceptsCompliantPasswords() {
        assertTrue(PasswordPolicy.isValid("Abcd1!"));
        assertTrue(PasswordPolicy.isValid("StrongP@ss1"));
    }
}
