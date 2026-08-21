package repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Data access layer for client entities.
 */
public class ClientRepository {
    private static final Logger logger = LoggerFactory.getLogger(ClientRepository.class);

    /**
     * Persists a new client record.
     *
     * @param fullName client full name
     * @param clientType client type
     * @param nationality nationality code
     * @param countryOfBirth country of birth code
     * @param dateOfBirth date of birth in yyyy-MM-dd format
     * @param taxResidency tax residency code
     * @param status onboarding status
     * @param isActive active flag
     * @return generated client id
     * @throws SQLException when insert fails
     */
    public int createClient(String fullName, String clientType, String nationality, String countryOfBirth, String dateOfBirth, String taxResidency, String status, boolean isActive) throws SQLException {
        String sql = "INSERT INTO client (full_name, client_type, nationality, country_of_birth, date_of_birth, tax_residency, status, is_active) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, fullName);
            ps.setString(2, clientType);
            ps.setString(3, nationality);
            ps.setString(4, countryOfBirth);
            ps.setString(5, dateOfBirth);
            ps.setString(6, taxResidency);
            ps.setString(7, status);
            ps.setBoolean(8, isActive);
            ps.executeUpdate();
            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int clientId = generatedKeys.getInt(1);
                    logger.info("Client created: clientId={} clientType={} status={}", clientId, clientType, status);
                    return clientId;
                }
            }
        }
        throw new SQLException("Failed to create client");
    }

    /**
     * Lists a summary of all clients.
     *
     * @return list of client JSON fragments
     * @throws SQLException when the query fails
     */
    public List<String> listClients() throws SQLException {
        String sql = "SELECT client_id, full_name, client_type, nationality, status, is_active FROM client";
        List<String> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String json = "  {"
                        + "\"client_id\":" + rs.getInt("client_id") + ","
                        + "\"full_name\":\"" + DatabaseConnection.escape(rs.getString("full_name")) + "\","
                        + "\"client_type\":\"" + DatabaseConnection.escape(rs.getString("client_type")) + "\","
                        + "\"nationality\":\"" + DatabaseConnection.escape(rs.getString("nationality")) + "\","
                        + "\"status\":\"" + DatabaseConnection.escape(rs.getString("status")) + "\","
                        + "\"is_active\":" + rs.getBoolean("is_active")
                        + "}";
                list.add(json);
            }
        }
        return list;
    }

    /**
     * Lists documents expiring within the given number of days.
     *
     * @param days lookahead window in days
     * @return list of expiring document JSON fragments
     * @throws SQLException when the query fails
     */
    public List<String> listExpiringDocuments(int days) throws SQLException {
        String sql = "SELECT DISTINCT c.client_id, c.full_name, c.client_type, d.doc_id, dt.doc_type_name, d.expiry_date " +
                "FROM document d " +
                "JOIN document_type dt ON d.doc_type_id = dt.doc_type_id " +
                "JOIN onboarding_case oc ON d.case_id = oc.case_id " +
                "JOIN client c ON oc.client_id = c.client_id " +
                "WHERE d.expiry_date IS NOT NULL " +
                "AND d.expiry_date BETWEEN CURDATE() AND DATE_ADD(CURDATE(), INTERVAL ? DAY)";

        List<String> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, days);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String docType = rs.getString("doc_type_name");
                    String expiryDate = rs.getString("expiry_date");
                    logger.warn("Document expiring in {} days: clientId={} docType={} expiry={}",
                            days, rs.getInt("client_id"), docType, expiryDate);
                    String json = "  {"
                            + "\"client_id\":" + rs.getInt("client_id") + ","
                            + "\"full_name\":\"" + DatabaseConnection.escape(rs.getString("full_name")) + "\","
                            + "\"client_type\":\"" + DatabaseConnection.escape(rs.getString("client_type")) + "\","
                            + "\"doc_id\":" + rs.getInt("doc_id") + ","
                            + "\"doc_type\":\"" + DatabaseConnection.escape(docType) + "\","
                            + "\"expiry_date\":\"" + expiryDate + "\""
                            + "}";
                    list.add(json);
                }
            }
        }
        return list;
    }

    /**
     * Fetches the full record for a client.
     *
     * @param id client id
     * @return client JSON representation, or null when not found
     * @throws SQLException when the query fails
     */
    public String getClientById(int id) throws SQLException {
        String sql = "SELECT client_id, full_name, client_type, nationality, date_of_birth, " +
                "country_of_birth, tax_residency, occupation, employer, main_source_of_funds, " +
                "annual_income_band, status, is_active FROM client WHERE client_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    logger.debug("Client not found: clientId={}", id);
                    return null;
                }
                return "{"
                        + "\"client_id\":" + rs.getInt("client_id") + ","
                        + "\"full_name\":\"" + DatabaseConnection.escape(rs.getString("full_name")) + "\","
                        + "\"client_type\":\"" + DatabaseConnection.escape(rs.getString("client_type")) + "\","
                        + "\"nationality\":\"" + DatabaseConnection.escape(rs.getString("nationality")) + "\","
                        + "\"date_of_birth\":\"" + rs.getString("date_of_birth") + "\","
                        + "\"country_of_birth\":\"" + DatabaseConnection.escape(rs.getString("country_of_birth")) + "\","
                        + "\"tax_residency\":\"" + DatabaseConnection.escape(rs.getString("tax_residency")) + "\","
                        + "\"occupation\":" + DatabaseConnection.jsonStringOrNull(rs.getString("occupation")) + ","
                        + "\"employer\":" + DatabaseConnection.jsonStringOrNull(rs.getString("employer")) + ","
                        + "\"main_source_of_funds\":" + DatabaseConnection.jsonStringOrNull(rs.getString("main_source_of_funds")) + ","
                        + "\"annual_income_band\":" + DatabaseConnection.jsonStringOrNull(rs.getString("annual_income_band")) + ","
                        + "\"status\":\"" + DatabaseConnection.escape(rs.getString("status")) + "\","
                        + "\"is_active\":" + rs.getBoolean("is_active")
                        + "}";
            }
        }
    }
}