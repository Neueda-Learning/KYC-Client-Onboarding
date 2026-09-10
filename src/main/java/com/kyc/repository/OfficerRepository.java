package repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Data access layer for compliance officer lookups.
 */
public class OfficerRepository {
    private static final Logger logger = LoggerFactory.getLogger(OfficerRepository.class);

    /**
     * Lists all compliance officers available for case assignment.
     *
     * @return JSON array entries of officer_id/full_name pairs
     * @throws SQLException when the query fails
     */
    public List<String> listOfficers() throws SQLException {
        String sql = "SELECT officer_id, full_name FROM compliance_officer ORDER BY full_name";
        List<String> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add("{"
                        + "\"officer_id\":" + rs.getInt("officer_id") + ","
                        + "\"full_name\":\"" + DatabaseConnection.escape(rs.getString("full_name")) + "\""
                        + "}");
            }
        }
        logger.debug("Fetched compliance officers: count={}", list.size());
        return list;
    }

    /**
     * Looks up the full name of a compliance officer.
     *
     * @param officerId officer id
     * @return full name, or null when no matching officer exists
     * @throws SQLException when the query fails
     */
    public String getOfficerName(int officerId) throws SQLException {
        String sql = "SELECT full_name FROM compliance_officer WHERE officer_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, officerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    logger.warn("Officer name lookup failed: officerId={} reason=not found", officerId);
                    return null;
                }
                return rs.getString("full_name");
            }
        }
    }
}
