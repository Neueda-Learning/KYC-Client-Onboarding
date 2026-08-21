package service;

import java.sql.SQLException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repository.ClientRepository;

/**
 * Business logic for client operations.
 */
public class ClientService {
    private static final Logger logger = LoggerFactory.getLogger(ClientService.class);
    private final ClientRepository clientRepository = new ClientRepository();

    /**
     * Creates a client record using request-provided attributes.
     *
     * @param fullName client full name
     * @param clientType client type
     * @param nationality nationality code
     * @param countryOfBirth country of birth code
     * @param dateOfBirth date of birth in yyyy-MM-dd format
     * @param taxResidency tax residency code
     * @param status onboarding status
     * @param isActive active flag
     * @return newly created client id
     * @throws SQLException when persistence fails
     */
    public int createClient(String fullName, String clientType, String nationality, String countryOfBirth,
                            String dateOfBirth, String taxResidency, String status, boolean isActive)
            throws SQLException {
        int clientId = clientRepository.createClient(fullName, clientType, nationality, countryOfBirth, dateOfBirth,
                taxResidency, status, isActive);
        logger.info("Client created: clientId={} clientType={} status={}", clientId, clientType, status);
        return clientId;
    }

    /**
     * Lists a summary of all clients.
     *
     * @return JSON array of client summaries
     * @throws SQLException when the query fails
     */
    public String listClients() throws SQLException {
        List<String> clients = clientRepository.listClients();
        logger.debug("Listed clients: count={}", clients.size());
        return "[\n" + String.join(",\n", clients) + "\n]";
    }

    /**
     * Lists documents expiring within the given number of days.
     *
     * @param days lookahead window in days
     * @return JSON array of expiring documents
     * @throws SQLException when the query fails
     */
    public String listExpiringDocuments(int days) throws SQLException {
        List<String> docs = clientRepository.listExpiringDocuments(days);
        logger.debug("Listed expiring documents: days={} count={}", days, docs.size());
        return "[\n" + String.join(",\n", docs) + "\n]";
    }

    /**
     * Fetches the full record for a client.
     *
     * @param id client id
     * @return client JSON representation, or null when not found
     * @throws SQLException when the query fails
     */
    public String getClientById(int id) throws SQLException {
        String json = clientRepository.getClientById(id);
        if (json == null) {
            logger.warn("Client lookup failed: clientId={} reason=not found", id);
        }
        return json;
    }
}