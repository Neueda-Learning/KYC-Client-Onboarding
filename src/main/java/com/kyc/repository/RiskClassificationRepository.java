package repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Data access layer for case risk classification entries.
 */
public class RiskClassificationRepository {
    private static final Logger logger = LoggerFactory.getLogger(RiskClassificationRepository.class);

    /**
     * Inserts a new risk classification entry for a case.
     *
     * @param caseId case id being classified
     * @param riskLevel LOW / MEDIUM / HIGH
     * @param rationale reason for the classification
     * @param assessedBy officer id performing the classification, or null
     * @param nextReviewDate next review date in yyyy-MM-dd format
     * @return generated classification id
     * @throws SQLException when insert fails
     */
    public int addClassification(int caseId, String riskLevel, String rationale, Integer assessedBy,
                                  String nextReviewDate) throws SQLException {
        String sql = "INSERT INTO risk_classification (case_id, risk_level, classification_date, assessed_by, rationale, next_review_date) "
                + "VALUES (?, ?, CURRENT_TIMESTAMP, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, caseId);
            ps.setString(2, riskLevel);
            if (assessedBy == null) {
                ps.setNull(3, Types.INTEGER);
            } else {
                ps.setInt(3, assessedBy);
            }
            ps.setString(4, rationale);
            ps.setString(5, nextReviewDate);
            ps.executeUpdate();
            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int classificationId = generatedKeys.getInt(1);
                    logger.info("Risk classification recorded: caseId={} classificationId={} riskLevel={} assessedBy={}",
                            caseId, classificationId, riskLevel, assessedBy);
                    return classificationId;
                }
            }
        }
        throw new SQLException("Failed to record risk classification");
    }

    /**
     * Fetches the most recent risk classification for a case, if any.
     *
     * @param caseId target case id
     * @return JSON fragment for the latest classification, or null when none exists
     * @throws SQLException when the query fails
     */
    public String getLatestForCase(int caseId) throws SQLException {
        String sql = "SELECT rc.risk_level, rc.classification_date, rc.rationale, rc.next_review_date, "
                + "rc.assessed_by, co.full_name AS assessor_name "
                + "FROM risk_classification rc LEFT JOIN compliance_officer co ON rc.assessed_by = co.officer_id "
                + "WHERE rc.case_id = ? ORDER BY rc.classification_date DESC LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, caseId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    logger.debug("No risk classification found: caseId={}", caseId);
                    return null;
                }
                int assessedBy = rs.getInt("assessed_by");
                boolean hasAssessor = !rs.wasNull();
                return "{"
                        + "\"risk_level\":\"" + DatabaseConnection.escape(rs.getString("risk_level")) + "\","
                        + "\"classification_date\":\"" + rs.getString("classification_date") + "\","
                        + "\"rationale\":" + DatabaseConnection.jsonStringOrNull(rs.getString("rationale")) + ","
                        + "\"next_review_date\":" + DatabaseConnection.jsonStringOrNull(rs.getString("next_review_date")) + ","
                        + "\"assessed_by\":" + (hasAssessor ? String.valueOf(assessedBy) : "null") + ","
                        + "\"assessor_name\":" + DatabaseConnection.jsonStringOrNull(rs.getString("assessor_name"))
                        + "}";
            }
        }
    }
}