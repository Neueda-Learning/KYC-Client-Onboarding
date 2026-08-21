package service;

import java.sql.SQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repository.AuthRepository;
import repository.AuthRepository.Credential;
import util.PasswordHasher;

/**
 * Authenticates users against the client, compliance_officer and admin_officer tables.
 */
public class AuthService {
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    public static final String ROLE_CLIENT = "CLIENT";
    public static final String ROLE_COMPLIANCE_OFFICER = "COMPLIANCE_OFFICER";
    public static final String ROLE_ADMIN_COMPLIANCE_OFFICER = "ADMIN_COMPLIANCE_OFFICER";

    private final AuthRepository authRepository = new AuthRepository();

    /**
     * Result of a successful login.
     */
    public static class LoginResult {
        public final String role;
        public final int entityId;
        public final String fullName;
        public final String username;

        LoginResult(String role, int entityId, String fullName, String username) {
            this.role = role;
            this.entityId = entityId;
            this.fullName = fullName;
            this.username = username;
        }
    }

    /**
     * Verifies a username/password against client, then compliance officer, then admin
     * officer records, in that order.
     *
     * @param username login username
     * @param password plaintext password
     * @return the matched login result, or null when no account matches
     * @throws SQLException when a lookup query fails
     */
    public LoginResult login(String username, String password) throws SQLException {
        LoginResult result = tryRole(ROLE_CLIENT, authRepository.findClientByUsername(username), password);
        if (result == null) {
            result = tryRole(ROLE_COMPLIANCE_OFFICER, authRepository.findOfficerByUsername(username), password);
        }
        if (result == null) {
            result = tryRole(ROLE_ADMIN_COMPLIANCE_OFFICER, authRepository.findAdminByUsername(username), password);
        }
        if (result == null) {
            logger.warn("Login failed: username={} reason=no matching credentials", username);
        } else {
            logger.info("Login succeeded: username={} role={} entityId={}", username, result.role, result.entityId);
        }
        return result;
    }

    private LoginResult tryRole(String role, Credential credential, String password) {
        if (credential == null || credential.passwordHash == null) {
            return null;
        }
        if (!PasswordHasher.verify(password, credential.passwordHash)) {
            return null;
        }
        return new LoginResult(role, credential.id, credential.fullName, credential.username);
    }
}
