package repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CaseRepository {

    public int createOnboardingCase(int clientId, String productType, String status) throws SQLException {
        String sql = "INSERT INTO onboarding_case (client_id, opened_date, product_type, case_status) VALUES (?, CURRENT_TIMESTAMP, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, clientId);
            ps.setString(2, productType);
            ps.setString(3, status);
            ps.executeUpdate();
            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }
        }
        throw new SQLException("Failed to open onboarding case");
    }

    public int uploadDocument(int caseId) throws SQLException {
        String sql = "INSERT INTO document (case_id, doc_type_id, submission_date, verified_flag) VALUES (?, 1, CURDATE(), false)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, caseId);
            ps.executeUpdate();
            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }
        }
        throw new SQLException("Failed to submit document");
    }

    public boolean verifyDocument(int caseId, int docId) throws SQLException {
        String sql = "UPDATE document SET verified_flag = true WHERE doc_id = ? AND case_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, docId);
            ps.setInt(2, caseId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean updateCaseStatus(int caseId, String newStatus) throws SQLException {
        String sql = "UPDATE onboarding_case SET case_status = ? WHERE case_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setInt(2, caseId);
            return ps.executeUpdate() > 0;
        }
    }

    public List<String> listCases(String statusFilter) throws SQLException {
        String sql = "SELECT oc.case_id, oc.client_id, oc.opened_date, oc.product_type, oc.case_status, " +
                "c.full_name AS client_name, c.client_type " +
                "FROM onboarding_case oc JOIN client c ON oc.client_id = c.client_id";

        if (statusFilter != null && !statusFilter.isEmpty()) {
            sql += " WHERE oc.case_status = ?";
        }

        List<String> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            if (statusFilter != null && !statusFilter.isEmpty()) {
                ps.setString(1, statusFilter);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String json = "  {"
                            + "\"case_id\":" + rs.getInt("case_id") + ","
                            + "\"client_id\":" + rs.getInt("client_id") + ","
                            + "\"client_name\":\"" + DatabaseConnection.escape(rs.getString("client_name")) + "\","
                            + "\"client_type\":\"" + DatabaseConnection.escape(rs.getString("client_type")) + "\","
                            + "\"product_type\":\"" + DatabaseConnection.escape(rs.getString("product_type")) + "\","
                            + "\"case_status\":\"" + DatabaseConnection.escape(rs.getString("case_status")) + "\","
                            + "\"opened_date\":\"" + rs.getString("opened_date") + "\""
                            + "}";
                    list.add(json);
                }
            }
        }
        return list;
    }

    public String getCaseById(int id) throws SQLException {
        String caseSql = "SELECT oc.case_id, oc.client_id, oc.opened_date, oc.product_type, oc.case_status, " +
                "oc.due_date, oc.completed_date, oc.rejection_reason, " +
                "c.full_name AS client_name, c.client_type " +
                "FROM onboarding_case oc JOIN client c ON oc.client_id = c.client_id " +
                "WHERE oc.case_id = ?";
        String docSql = "SELECT d.doc_id, dt.doc_type_name, d.submission_date, " +
                "d.verified_flag, d.expiry_date, d.rejection_reason " +
                "FROM document d JOIN document_type dt ON d.doc_type_id = dt.doc_type_id " +
                "WHERE d.case_id = ?";

        try (Connection conn = DatabaseConnection.getConnection()) {
            StringBuilder json = new StringBuilder();

            try (PreparedStatement ps = conn.prepareStatement(caseSql)) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return null;
                    }
                    json.append("{")
                            .append("\"case_id\":").append(rs.getInt("case_id")).append(",")
                            .append("\"client_id\":").append(rs.getInt("client_id")).append(",")
                            .append("\"client_name\":\"").append(DatabaseConnection.escape(rs.getString("client_name"))).append("\",")
                            .append("\"client_type\":\"").append(DatabaseConnection.escape(rs.getString("client_type"))).append("\",")
                            .append("\"product_type\":\"").append(DatabaseConnection.escape(rs.getString("product_type"))).append("\",")
                            .append("\"case_status\":\"").append(DatabaseConnection.escape(rs.getString("case_status"))).append("\",")
                            .append("\"opened_date\":\"").append(rs.getString("opened_date")).append("\",")
                            .append("\"due_date\":").append(DatabaseConnection.jsonStringOrNull(rs.getString("due_date"))).append(",")
                            .append("\"completed_date\":").append(DatabaseConnection.jsonStringOrNull(rs.getString("completed_date")))
                            .append(",")
                            .append("\"rejection_reason\":").append(DatabaseConnection.jsonStringOrNull(rs.getString("rejection_reason")));
                }
            }

            json.append(",\"documents\":[");
            try (PreparedStatement ps = conn.prepareStatement(docSql)) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    boolean first = true;
                    while (rs.next()) {
                        if (!first)
                            json.append(",");
                        json.append("{")
                                .append("\"doc_id\":").append(rs.getInt("doc_id")).append(",")
                                .append("\"doc_type\":\"").append(DatabaseConnection.escape(rs.getString("doc_type_name"))).append("\",")
                                .append("\"submission_date\":\"").append(rs.getString("submission_date")).append("\",")
                                .append("\"verified\":").append(rs.getBoolean("verified_flag")).append(",")
                                .append("\"expiry_date\":").append(DatabaseConnection.jsonStringOrNull(rs.getString("expiry_date")))
                                .append(",")
                                .append("\"rejection_reason\":")
                                .append(DatabaseConnection.jsonStringOrNull(rs.getString("rejection_reason")))
                                .append("}");
                        first = false;
                    }
                }
            }
            json.append("]}");
            return json.toString();
        }
    }
}