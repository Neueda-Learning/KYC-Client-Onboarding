package service;

import java.sql.SQLException;
import repository.OfficerRepository;

/**
 * Business logic for compliance officer lookups.
 */
public class OfficerService {
    private final OfficerRepository officerRepository;

    public OfficerService() {
        this(new OfficerRepository());
    }

    public OfficerService(OfficerRepository officerRepository) {
        this.officerRepository = officerRepository;
    }

    /**
     * Lists all compliance officers available for case assignment.
     *
     * @return JSON array of officers
     * @throws SQLException when the query fails
     */
    public String listOfficers() throws SQLException {
        return "[\n" + String.join(",\n", officerRepository.listOfficers()) + "\n]";
    }

    /**
     * Looks up the full name of a compliance officer.
     *
     * @param officerId officer id
     * @return full name, or null when no matching officer exists
     * @throws SQLException when the query fails
     */
    public String getOfficerName(int officerId) throws SQLException {
        return officerRepository.getOfficerName(officerId);
    }
}
