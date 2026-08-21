package service;

import java.sql.SQLException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repository.OnboardingRepository;
import util.CredentialGenerator;
import util.PasswordHasher;

/**
 * Business logic for opening a new onboarding case together with its client and address.
 */
public class OnboardingService {
    private static final Logger logger = LoggerFactory.getLogger(OnboardingService.class);

    private final OnboardingRepository onboardingRepository;
    private final NotificationService notificationService;

    public OnboardingService() {
        this(new OnboardingRepository(), new NotificationService());
    }

    public OnboardingService(OnboardingRepository onboardingRepository) {
        this(onboardingRepository, new NotificationService());
    }

    public OnboardingService(OnboardingRepository onboardingRepository, NotificationService notificationService) {
        this.onboardingRepository = onboardingRepository;
        this.notificationService = notificationService;
    }

    /**
     * Creates a client, its address, and an onboarding case in one atomic operation,
     * recording any documents already provided and optionally assigning an officer.
     * A login username (derived from the client's full name) and a random temporary
     * password are generated automatically; only the password's hash is persisted,
     * and delivery of the credentials to the client is handed off to
     * {@link NotificationService} (currently a logging stub).
     *
     * @param client      new client attributes
     * @param address     new client's address attributes
     * @param productType requested product type
     * @param dueDate     case due date in yyyy-MM-dd format, or null
     * @param officerId   compliance officer to assign, or null to leave unassigned
     * @param docTypeIds  document type ids already provided at onboarding, or null
     * @return the generated client id and case id
     * @throws SQLException when persistence fails
     */
    public OnboardingRepository.OpenCaseResult openCase(OnboardingRepository.ClientInput client,
            OnboardingRepository.AddressInput address, String productType, String dueDate, Integer officerId,
            List<Integer> docTypeIds) throws SQLException {
        String username = CredentialGenerator.generateUsername(client.fullName);
        String temporaryPassword = CredentialGenerator.generateTemporaryPassword();
        client.username = username;
        client.passwordHash = PasswordHasher.hash(temporaryPassword);

        OnboardingRepository.OpenCaseResult result = onboardingRepository.openCase(client, address, productType,
                dueDate, officerId, docTypeIds);
        logger.info("Case opened from onboarding form: clientId={} caseId={} username={}", result.clientId,
                result.caseId, username);
        notificationService.sendLoginCredentials(username, temporaryPassword);
        return result;
    }
}

