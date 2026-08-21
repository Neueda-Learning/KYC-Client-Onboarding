package repository;

import java.sql.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Data access layer for onboarding case entities.
 */
public class CaseRepository {
    private static final Logger logger = LoggerFactory.getLogger(CaseRepository.class);

    /**
     * Persists a new onboarding case.
     *
     * @param clientId    related client id
     * @param productType product type
     * @param status      case status
     * @return generated case id
     * @throws SQLException when insert fails
     */
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
                    int caseId = generatedKeys.getInt(1);
                    logger.info("Onboarding case opened: caseId={} clientId={} type={} status={}",
                            caseId, clientId, productType, status);
                    return caseId;
                }
            }
        }
        throw new SQLException("Failed to open onboarding case");
    }

    /**
     * Persists a new document row for a case.
     *
     * @param caseId    target case id
     * @param docTypeId document type id
     * @return generated document id
     * @throws SQLException when insert fails
     */
    public int uploadDocument(int caseId, int docTypeId) throws SQLException {
        String sql = "INSERT INTO document (case_id, doc_type_id, submission_date, verified_flag) VALUES (?, ?, CURDATE(), false)";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, caseId);
            ps.setInt(2, docTypeId);
            ps.executeUpdate();
            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int docId = generatedKeys.getInt(1);
                    logger.info("Document submitted: caseId={} docId={} docType={}",
                            caseId, docId, getDocTypeName(conn, docTypeId));
                    return docId;
                }
            }
        }
        throw new SQLException("Failed to submit document");
    }

    /**
     * Marks a document as verified.
     *
     * @param caseId owning case id
     * @param docId  document id
     * @return true when a matching document was updated
     * @throws SQLException when the update fails
     */
    public boolean verifyDocument(int caseId, int docId) throws SQLException {
        String sql = "UPDATE document SET verified_flag = true WHERE doc_id = ? AND case_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, docId);
            ps.setInt(2, caseId);
            boolean updated = ps.executeUpdate() > 0;
            if (updated) {
                logger.info("Document verified: caseId={} docId={} docType={}",
                        caseId, docId, getDocTypeNameForDocument(conn, docId));
            } else {
                logger.warn("Document verification failed: caseId={} docId={} reason=not found or case mismatch",
                        caseId, docId);
            }
            return updated;
        }
    }

    /**
     * Updates the status of an onboarding case, enforcing the case state machine.
     *
     * @param caseId    target case id
     * @param newStatus requested status
     * @return true when the case existed and was updated
     * @throws SQLException                    when persistence fails
     * @throws InvalidStateTransitionException when the requested transition is not
     *                                         allowed
     */
    public boolean updateCaseStatus(int caseId, String newStatus) throws SQLException {
        String currentStatus = getCaseStatus(caseId);
        if (currentStatus == null) {
            return false;
        }
        if ("CLOSED".equalsIgnoreCase(currentStatus)) {
            logger.error("Invalid state transition: caseId={} from={} to={} \u2014 case already closed",
                    caseId, currentStatus, newStatus);
            throw new InvalidStateTransitionException("Case " + caseId + " is already closed");
        }
        if ("CLOSED".equalsIgnoreCase(newStatus) && hasUnverifiedDocuments(caseId)) {
            logger.error("Invalid state transition: caseId={} from={} to={} \u2014 documents not fully verified",
                    caseId, currentStatus, newStatus);
            throw new InvalidStateTransitionException("Case " + caseId + " has unverified documents");
        }

        String sql = "UPDATE onboarding_case SET case_status = ? WHERE case_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setInt(2, caseId);
            boolean updated = ps.executeUpdate() > 0;
            if (updated) {
                logger.info("Case status updated: caseId={} from={} to={}", caseId, currentStatus, newStatus);
            }
            return updated;
        }
    }

    /**
     * Looks up the document type name for logging purposes.
     *
     * @param conn      open connection to reuse
     * @param docTypeId document type id
     * @return document type name, or UNKNOWN when not found
     * @throws SQLException when the lookup fails
     */
    private String getDocTypeName(Connection conn, int docTypeId) throws SQLException {
        String sql = "SELECT doc_type_name FROM document_type WHERE doc_type_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, docTypeId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("doc_type_name") : "UNKNOWN";
            }
        }
    }

    /**
     * Looks up the document type name for an existing document, for logging
     * purposes.
     *
     * @param conn  open connection to reuse
     * @param docId document id
     * @return document type name, or UNKNOWN when not found
     * @throws SQLException when the lookup fails
     */
    private String getDocTypeNameForDocument(Connection conn, int docId) throws SQLException {
        String sql = "SELECT dt.doc_type_name FROM document d "
                + "JOIN document_type dt ON d.doc_type_id = dt.doc_type_id WHERE d.doc_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, docId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("doc_type_name") : "UNKNOWN";
            }
        }
    }

    /**
     * Fetches the current status of a case.
     *
     * @param caseId target case id
     * @return current case status, or null when the case does not exist
     * @throws SQLException when the lookup fails
     */
    public String getCaseStatus(int caseId) throws SQLException {
        String sql = "SELECT case_status FROM onboarding_case WHERE case_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, caseId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("case_status") : null;
            }
        }
    }

    /**
     * Checks whether a case still has unverified documents.
     *
     * @param caseId target case id
     * @return true when at least one unverified document exists
     * @throws SQLException when the lookup fails
     */
    public boolean hasUnverifiedDocuments(int caseId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM document WHERE case_id = ? AND verified_flag = false";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, caseId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    /**
     * Fetches the client type for the client associated with a case.
     *
     * @param caseId target case id
     * @return client type, or null when the case does not exist
     * @throws SQLException when the lookup fails
     */
    public String getClientTypeForCase(int caseId) throws SQLException {
        String sql = "SELECT c.client_type FROM onboarding_case oc " +
                "JOIN client c ON oc.client_id = c.client_id WHERE oc.case_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, caseId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("client_type") : null;
            }
        }
    }

    /**
     * Fetches the distinct document type names submitted for a case.
     *
     * @param caseId target case id
     * @return list of submitted document type names
     * @throws SQLException when the lookup fails
     */
    public List<String> getSubmittedDocumentTypes(int caseId) throws SQLException {
        String sql = "SELECT DISTINCT dt.doc_type_name FROM document d " +
                "JOIN document_type dt ON d.doc_type_id = dt.doc_type_id WHERE d.case_id = ?";
        List<String> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, caseId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(rs.getString("doc_type_name"));
                }
            }
        }
        return list;
    }

    /**
     * Persists a case status change without applying state machine validation.
     * Callers are responsible for validating the requested transition beforehand.
     *
     * @param caseId    target case id
     * @param newStatus status to persist
     * @return true when a matching case was updated
     * @throws SQLException when the update fails
     */
    public boolean setCaseStatus(int caseId, String newStatus) throws SQLException {
        String sql = "UPDATE onboarding_case SET case_status = ? WHERE case_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setInt(2, caseId);
            boolean updated = ps.executeUpdate() > 0;
            if (updated) {
                logger.info("Case status set: caseId={} to={}", caseId, newStatus);
            }
            return updated;
        }
    }

    public List<String> listCases(String statusFilter, Integer officerFilter) throws SQLException {
        String sql = "SELECT oc.case_id, oc.client_id, oc.opened_date, oc.product_type, oc.case_status, " +
                "oc.due_date, oc.assigned_officer_id, co.full_name AS officer_name, " +
                "c.full_name AS client_name, c.client_type " +
                "FROM onboarding_case oc JOIN client c ON oc.client_id = c.client_id " +
                "LEFT JOIN compliance_officer co ON oc.assigned_officer_id = co.officer_id";

        List<String> conditions = new ArrayList<>();
        if (statusFilter != null && !statusFilter.isEmpty()) {
            conditions.add("oc.case_status = ?");
        }
        if (officerFilter != null) {
            conditions.add("oc.assigned_officer_id = ?");
        }
        if (!conditions.isEmpty()) {
            sql += " WHERE " + String.join(" AND ", conditions);
        }

        List<String> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            int paramIndex = 1;
            if (statusFilter != null && !statusFilter.isEmpty()) {
                ps.setString(paramIndex++, statusFilter);
            }
            if (officerFilter != null) {
                ps.setInt(paramIndex++, officerFilter);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int officerId = rs.getInt("assigned_officer_id");
                    boolean hasOfficer = !rs.wasNull();
                    String json = "  {"
                            + "\"case_id\":" + rs.getInt("case_id") + ","
                            + "\"client_id\":" + rs.getInt("client_id") + ","
                            + "\"client_name\":\"" + DatabaseConnection.escape(rs.getString("client_name")) + "\","
                            + "\"client_type\":\"" + DatabaseConnection.escape(rs.getString("client_type")) + "\","
                            + "\"product_type\":\"" + DatabaseConnection.escape(rs.getString("product_type")) + "\","
                            + "\"case_status\":\"" + DatabaseConnection.escape(rs.getString("case_status")) + "\","
                            + "\"opened_date\":\"" + rs.getString("opened_date") + "\","
                            + "\"due_date\":" + DatabaseConnection.jsonStringOrNull(rs.getString("due_date")) + ","
                            + "\"assigned_officer_id\":" + (hasOfficer ? officerId : "null") + ","
                            + "\"officer_name\":" + DatabaseConnection.jsonStringOrNull(rs.getString("officer_name"))
                            + "}";
                    list.add(json);
                }
            }
        }
        return list;
    }

    /**
     * Assigns (or unassigns, when officerId is null) the compliance officer
     * handling a case.
     *
     * @param caseId    target case id
     * @param officerId officer id to assign, or null to unassign
     * @return true when a matching case was updated
     * @throws SQLException when persistence fails
     */
    public boolean assignOfficer(int caseId, Integer officerId) throws SQLException {
        String sql = "UPDATE onboarding_case SET assigned_officer_id = ? WHERE case_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            if (officerId == null) {
                ps.setNull(1, Types.INTEGER);
            } else {
                ps.setInt(1, officerId);
            }
            ps.setInt(2, caseId);
            boolean updated = ps.executeUpdate() > 0;
            if (updated) {
                logger.info("Case officer assigned: caseId={} officerId={}", caseId, officerId);
            }
            return updated;
        }
    }

    public String getCaseById(int id) throws SQLException {
        String caseSql = "SELECT oc.case_id, oc.client_id, oc.opened_date, oc.product_type, oc.case_status, " +
                "oc.due_date, oc.completed_date, oc.rejection_reason, oc.assigned_officer_id, co.full_name AS officer_name, "
                +
                "c.full_name AS client_name, c.client_type, c.date_of_birth, c.country_of_birth, c.nationality, " +
                "c.tax_residency, c.occupation, c.employer, c.main_source_of_funds, c.annual_income_band " +
                "FROM onboarding_case oc JOIN client c ON oc.client_id = c.client_id " +
                "LEFT JOIN compliance_officer co ON oc.assigned_officer_id = co.officer_id " +
                "WHERE oc.case_id = ?";
        String docSql = "SELECT d.doc_id, dt.doc_type_name, d.submission_date, " +
                "d.verified_flag, d.expiry_date, d.rejection_reason " +
                "FROM document d JOIN document_type dt ON d.doc_type_id = dt.doc_type_id " +
                "WHERE d.case_id = ?";
        String addressSql = "SELECT address_type, line1, line2, city, state, postcode, country, is_current " +
                "FROM client_address WHERE client_id = ? ORDER BY address_id";

        try (Connection conn = DatabaseConnection.getConnection()) {
            StringBuilder json = new StringBuilder();
            int clientId;

            try (PreparedStatement ps = conn.prepareStatement(caseSql)) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return null;
                    }
                    clientId = rs.getInt("client_id");
                    String dueDate = rs.getString("due_date");
                    String completedDate = rs.getString("completed_date");
                    if (dueDate != null && completedDate == null) {
                        LocalDate due = LocalDate.parse(dueDate);
                        LocalDate today = LocalDate.now();
                        if (today.isAfter(due)) {
                            logger.warn("Case overdue: caseId={} dueDate={} currentDate={} daysOverdue={}",
                                    id, due, today, ChronoUnit.DAYS.between(due, today));
                        }
                    }
                    int officerId = rs.getInt("assigned_officer_id");
                    boolean hasOfficer = !rs.wasNull();
                    json.append("{")
                            .append("\"case_id\":").append(rs.getInt("case_id")).append(",")
                            .append("\"client_id\":").append(clientId).append(",")
                            .append("\"client_name\":\"").append(DatabaseConnection.escape(rs.getString("client_name")))
                            .append("\",")
                            .append("\"client_type\":\"").append(DatabaseConnection.escape(rs.getString("client_type")))
                            .append("\",")
                            .append("\"product_type\":\"")
                            .append(DatabaseConnection.escape(rs.getString("product_type"))).append("\",")
                            .append("\"case_status\":\"").append(DatabaseConnection.escape(rs.getString("case_status")))
                            .append("\",")
                            .append("\"opened_date\":\"").append(rs.getString("opened_date")).append("\",")
                            .append("\"due_date\":").append(DatabaseConnection.jsonStringOrNull(dueDate)).append(",")
                            .append("\"completed_date\":").append(DatabaseConnection.jsonStringOrNull(completedDate))
                            .append(",")
                            .append("\"rejection_reason\":")
                            .append(DatabaseConnection.jsonStringOrNull(rs.getString("rejection_reason"))).append(",")
                            .append("\"assigned_officer_id\":").append(hasOfficer ? String.valueOf(officerId) : "null")
                            .append(",")
                            .append("\"officer_name\":")
                            .append(DatabaseConnection.jsonStringOrNull(rs.getString("officer_name"))).append(",")
                            .append("\"date_of_birth\":\"").append(rs.getString("date_of_birth")).append("\",")
                            .append("\"country_of_birth\":\"")
                            .append(DatabaseConnection.escape(rs.getString("country_of_birth"))).append("\",")
                            .append("\"nationality\":\"").append(DatabaseConnection.escape(rs.getString("nationality")))
                            .append("\",")
                            .append("\"tax_residency\":\"")
                            .append(DatabaseConnection.escape(rs.getString("tax_residency"))).append("\",")
                            .append("\"occupation\":")
                            .append(DatabaseConnection.jsonStringOrNull(rs.getString("occupation"))).append(",")
                            .append("\"employer\":")
                            .append(DatabaseConnection.jsonStringOrNull(rs.getString("employer"))).append(",")
                            .append("\"main_source_of_funds\":")
                            .append(DatabaseConnection.jsonStringOrNull(rs.getString("main_source_of_funds")))
                            .append(",")
                            .append("\"annual_income_band\":")
                            .append(DatabaseConnection.jsonStringOrNull(rs.getString("annual_income_band")));
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
                                .append("\"doc_type\":\"")
                                .append(DatabaseConnection.escape(rs.getString("doc_type_name"))).append("\",")
                                .append("\"submission_date\":\"").append(rs.getString("submission_date")).append("\",")
                                .append("\"verified\":").append(rs.getBoolean("verified_flag")).append(",")
                                .append("\"expiry_date\":")
                                .append(DatabaseConnection.jsonStringOrNull(rs.getString("expiry_date")))
                                .append(",")
                                .append("\"rejection_reason\":")
                                .append(DatabaseConnection.jsonStringOrNull(rs.getString("rejection_reason")))
                                .append("}");
                        first = false;
                    }
                }
            }
            json.append("]");

            json.append(",\"addresses\":[");
            try (PreparedStatement ps = conn.prepareStatement(addressSql)) {
                ps.setInt(1, clientId);
                try (ResultSet rs = ps.executeQuery()) {
                    boolean first = true;
                    while (rs.next()) {
                        if (!first)
                            json.append(",");
                        json.append("{")
                                .append("\"address_type\":")
                                .append(DatabaseConnection.jsonStringOrNull(rs.getString("address_type"))).append(",")
                                .append("\"line1\":\"").append(DatabaseConnection.escape(rs.getString("line1")))
                                .append("\",")
                                .append("\"line2\":").append(DatabaseConnection.jsonStringOrNull(rs.getString("line2")))
                                .append(",")
                                .append("\"city\":\"").append(DatabaseConnection.escape(rs.getString("city")))
                                .append("\",")
                                .append("\"state\":").append(DatabaseConnection.jsonStringOrNull(rs.getString("state")))
                                .append(",")
                                .append("\"postcode\":")
                                .append(DatabaseConnection.jsonStringOrNull(rs.getString("postcode"))).append(",")
                                .append("\"country\":\"").append(DatabaseConnection.escape(rs.getString("country")))
                                .append("\",")
                                .append("\"is_current\":")
                                .append(DatabaseConnection.jsonStringOrNull(rs.getString("is_current")))
                                .append("}");
                        first = false;
                    }
                }
            }
            json.append("]");

            String riskJson = new RiskClassificationRepository().getLatestForCase(id);
            json.append(",\"risk_classification\":").append(riskJson == null ? "null" : riskJson);
            json.append("}");
            return json.toString();
        }
    }
}