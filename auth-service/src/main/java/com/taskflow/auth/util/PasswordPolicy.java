package com.taskflow.auth.util;

/**
 * Centralized password policy for the TaskFlow platform.
 *
 * <p>Rules:
 * <ul>
 *   <li>At least one uppercase letter</li>
 *   <li>At least one lowercase letter</li>
 *   <li>At least one digit</li>
 *   <li>At least one special character</li>
 *   <li>Minimum length of 6 characters</li>
 * </ul>
 */
public final class PasswordPolicy {

    /** Regex enforcing uppercase, lowercase, digit, special character, min length 6. */
    public static final String REGEX =
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{6,}$";

    /** Human-readable description of the password rules. */
    public static final String MESSAGE =
            "Password must be at least 6 characters and contain an uppercase letter, "
                    + "a lowercase letter, a number and a special character.";

    private PasswordPolicy() {
        // utility class
    }

    /**
     * Validates the given password against the policy.
     *
     * @param password password to validate (may be {@code null})
     * @return {@code true} if the password matches the policy, {@code false} otherwise
     */
    public static boolean isValid(String password) {
        return password != null && password.matches(REGEX);
    }
}
