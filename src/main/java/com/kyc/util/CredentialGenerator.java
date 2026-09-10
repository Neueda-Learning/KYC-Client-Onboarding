package util;

import java.security.SecureRandom;

/**
 * Generates login usernames and temporary passwords for client self-service accounts
 * created automatically when an onboarding case is opened.
 */
public final class CredentialGenerator {
    private static final String PASSWORD_CHARS =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%^&*";
    private static final int PASSWORD_LENGTH = 14;
    private static final SecureRandom RANDOM = new SecureRandom();

    private CredentialGenerator() {
    }

    /**
     * Derives a login username from a full name: lowercased with whitespace runs
     * collapsed into single dots, e.g. {@code "Jane   Doe"} becomes {@code "jane.doe"}.
     *
     * @param fullName client's full name
     * @return derived username
     */
    public static String generateUsername(String fullName) {
        return fullName.trim().toLowerCase().replaceAll("\\s+", ".");
    }

    /**
     * Generates a random temporary password for a first-login/change-password flow.
     *
     * @return randomly generated plaintext password
     */
    public static String generateTemporaryPassword() {
        StringBuilder sb = new StringBuilder(PASSWORD_LENGTH);
        for (int i = 0; i < PASSWORD_LENGTH; i++) {
            sb.append(PASSWORD_CHARS.charAt(RANDOM.nextInt(PASSWORD_CHARS.length())));
        }
        return sb.toString();
    }
}
