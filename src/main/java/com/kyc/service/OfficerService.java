package service;

import java.sql.SQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repository.OfficerRepository;

/**
 * Business logic for compliance officer lookups.
 */
public class OfficerService {
    private static final Logger logger = LoggerFactory.getLogger(OfficerService.class);
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
        java.util.List<String> officers = officerRepository.listOfficers();
        logger.debug("Listed compliance officers: count={}", officers.size());
        return "[\n" + String.join(",\n", officers) + "\n]";
    }

    /**
     * Looks up the full name of a compliance officer.
     *
     * @param officerId officer id
     * @return full name, or null when no matching officer exists
     * @throws SQLException when the query fails
     */
    public String getOfficerName(int officerId) throws SQLException {
        String name = officerRepository.getOfficerName(officerId);
        if (name == null) {
            logger.warn("Officer lookup failed: officerId={} reason=not found", officerId);
        }
        return name;
    }
}
