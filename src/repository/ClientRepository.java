package repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ClientRepository {

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
                    return generatedKeys.getInt(1);
                }
            }
        }
        throw new SQLException("Failed to create client");
    }

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
                    String json = "  {"
                            + "\"client_id\":" + rs.getInt("client_id") + ","
                            + "\"full_name\":\"" + DatabaseConnection.escape(rs.getString("full_name")) + "\","
                            + "\"client_type\":\"" + DatabaseConnection.escape(rs.getString("client_type")) + "\","
                            + "\"doc_id\":" + rs.getInt("doc_id") + ","
                            + "\"doc_type\":\"" + DatabaseConnection.escape(rs.getString("doc_type_name")) + "\","
                            + "\"expiry_date\":\"" + rs.getString("expiry_date") + "\""
                            + "}";
                    list.add(json);
                }
            }
        }
        return list;
    }

    public String getClientById(int id) throws SQLException {
        String sql = "SELECT client_id, full_name, client_type, nationality, date_of_birth, " +
                "country_of_birth, tax_residency, occupation, employer, main_source_of_funds, " +
                "annual_income_band, status, is_active FROM client WHERE client_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
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