package service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Skeleton for delivering client notifications (e.g. login credentials on
 * onboarding). Currently only logs the message that would be sent; replace
 * with a real email/SMS provider integration when one is available.
 */
public class NotificationService {
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    /**
     * "Sends" newly generated login credentials to a client. This is a stub —
     * it currently only logs the credentials instead of delivering them —
     * so the created account can be verified during development. Replace the
     * logging with a real delivery channel (email/SMS) before going live.
     *
     * @param username generated login username
     * @param temporaryPassword generated plaintext temporary password
     */
    public void sendLoginCredentials(String username, String temporaryPassword) {
        // TODO: replace with a real email/SMS delivery integration.
        logger.info("Client login credentials generated (delivery not yet implemented): username={} temporaryPassword={}",
                username, temporaryPassword);
    }
}
