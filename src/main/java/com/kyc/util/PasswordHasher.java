package util;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Salted PBKDF2-HMAC-SHA256 password hashing for client/officer login credentials.
 * Stored format: {@code pbkdf2_sha256$<iterations>$<base64 salt>$<base64 hash>}.
 */
public final class PasswordHasher {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 210_000;
    private static final int KEY_LENGTH_BITS = 256;
    private static final int SALT_LENGTH_BYTES = 16;

    private PasswordHasher() {
    }

    /**
     * Hashes a plaintext password with a freshly generated random salt.
     *
     * @param plainTextPassword password to hash
     * @return encoded string containing algorithm, iteration count, salt and hash
     */
    public static String hash(String plainTextPassword) {
        byte[] salt = new byte[SALT_LENGTH_BYTES];
        new SecureRandom().nextBytes(salt);
        byte[] hash = pbkdf2(plainTextPassword.toCharArray(), salt, ITERATIONS);
        return "pbkdf2_sha256$" + ITERATIONS + "$"
                + Base64.getEncoder().encodeToString(salt) + "$"
                + Base64.getEncoder().encodeToString(hash);
    }

    /**
     * Verifies a plaintext password against a previously encoded hash.
     *
     * @param plainTextPassword candidate password
     * @param encoded stored encoded hash produced by {@link #hash(String)}
     * @return true when the password matches
     */
    public static boolean verify(String plainTextPassword, String encoded) {
        try {
            String[] parts = encoded.split("\\$");
            if (parts.length != 4 || !parts[0].equals("pbkdf2_sha256")) {
                return false;
            }
            int iterations = Integer.parseInt(parts[1]);
            if (iterations < 50_000 || iterations > 500_000) {
                return false;
            }
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expectedHash = Base64.getDecoder().decode(parts[3]);
            if (salt.length != SALT_LENGTH_BYTES || expectedHash.length != (KEY_LENGTH_BITS / 8)) {
                return false;
            }
            byte[] actualHash = pbkdf2(plainTextPassword.toCharArray(), salt, iterations);
            return constantTimeEquals(expectedHash, actualHash);
        } catch (RuntimeException e) {
            // Malformed stored hash or invalid input — treat as non-matching rather than erroring.
            return false;
        }
    }

    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, KEY_LENGTH_BITS);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
            return factory.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("Password hashing failed", e);
        } finally {
            java.util.Arrays.fill(password, '\0');
        }
    }

    private static boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a.length != b.length) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length; i++) {
            result |= a[i] ^ b[i];
        }
        return result == 0;
    }

    /**
     * Command-line helper to generate seed-data hashes: {@code java util.PasswordHasher <password> [<password2> ...]}.
     *
     * @param args plaintext passwords to hash
     */
    public static void main(String[] args) {
        for (String arg : args) {
            System.out.println(arg + " -> " + hash(arg));
        }
    }
}
