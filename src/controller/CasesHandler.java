package controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repository.InvalidStateTransitionException;
import service.CaseService;
import util.HttpResponseUtil;

/**
 * Handles onboarding case and case document HTTP endpoints.
 */
public class CasesHandler implements HttpHandler {
    private static final Logger logger = LoggerFactory.getLogger(CasesHandler.class);
    private final CaseService caseService = new CaseService();
    private final service.OfficerService officerService = new service.OfficerService();

    /**
     * Dispatches requests for /api/onboarding/cases routes.
     *
     * @param exchange current HTTP exchange
     * @throws IOException when response writing fails
     */
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (HttpResponseUtil.handlePreflight(exchange)) {
            return;
        }
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        String query = exchange.getRequestURI().getQuery();
        String[] parts = path.split("/");

        try {
            if ("POST".equalsIgnoreCase(method)) {
                if (parts.length == 6 && "documents".equals(parts[5])) {
                    try {
                        handleUploadDocument(exchange, Integer.parseInt(parts[4]));
                    } catch (NumberFormatException e) {
                        HttpResponseUtil.sendResponse(exchange, 400, "{\"error\":\"Invalid case ID: " + repository.DatabaseConnection.escape(e.getMessage()) + "\"}");
                    }
                } else if (parts.length == 4 && "cases".equals(parts[3])) {
                    handleCreateOnboardingCase(exchange);
                } else {
                    HttpResponseUtil.sendResponse(exchange, 404, "{\"error\":\"Invalid POST endpoint path\"}");
                }
            } else if ("PATCH".equalsIgnoreCase(method)) {
                if (parts.length == 6 && "status".equals(parts[5])) {
                    try {
                        int caseId = Integer.parseInt(parts[4]);
                        handleUpdateCaseStatus(exchange, caseId);
                    } catch (NumberFormatException e) {
                        HttpResponseUtil.sendResponse(exchange, 400, "{\"error\":\"Invalid case ID: " + repository.DatabaseConnection.escape(e.getMessage()) + "\"}");
                    }
                } else if (parts.length == 6 && "officer".equals(parts[5])) {
                    try {
                        int caseId = Integer.parseInt(parts[4]);
                        handleAssignOfficer(exchange, caseId);
                    } catch (NumberFormatException e) {
                        HttpResponseUtil.sendResponse(exchange, 400, "{\"error\":\"Invalid case ID: " + repository.DatabaseConnection.escape(e.getMessage()) + "\"}");
                    }
                } else if (parts.length == 8 && "documents".equals(parts[5]) && "verify".equals(parts[7])) {
                    try {
                        int caseId = Integer.parseInt(parts[4]);
                        int docId = Integer.parseInt(parts[6]);
                        handleVerifyDocument(exchange, caseId, docId);
                    } catch (NumberFormatException e) {
                        HttpResponseUtil.sendResponse(exchange, 400, "{\"error\":\"Invalid case ID or document ID: " + repository.DatabaseConnection.escape(e.getMessage()) + "\"}");
                    }
                } else if (parts.length == 6 && "risk-classification".equals(parts[5])) {
                    try {
                        int caseId = Integer.parseInt(parts[4]);
                        handleUpdateRiskClassification(exchange, caseId);
                    } catch (NumberFormatException e) {
                        HttpResponseUtil.sendResponse(exchange, 400, "{\"error\":\"Invalid case ID: " + repository.DatabaseConnection.escape(e.getMessage()) + "\"}");
                    }
                } else {
                    HttpResponseUtil.sendResponse(exchange, 400, "{\"error\":\"Invalid PATCH endpoint path\"}");
                }
            } else if ("GET".equalsIgnoreCase(method)) {
                if (parts.length == 5 && !parts[4].isEmpty()) {
                    try {
                        handleGetCaseById(exchange, Integer.parseInt(parts[4]));
                    } catch (NumberFormatException e) {
                        HttpResponseUtil.sendResponse(exchange, 400, "{\"error\":\"Invalid case ID: " + repository.DatabaseConnection.escape(e.getMessage()) + "\"}");
                    }
                } else if (parts.length == 4 && "cases".equals(parts[3])) {
                    String statusFilter = parseQueryParam(query, "status");
                    String officerParam = parseQueryParam(query, "assigned_officer_id");
                    Integer officerFilter;
                    try {
                        officerFilter = officerParam == null ? null : Integer.valueOf(officerParam);
                    } catch (NumberFormatException e) {
                        HttpResponseUtil.sendResponse(exchange, 400, "{\"error\":\"Invalid assigned_officer_id\"}");
                        return;
                    }
                    handleListCases(exchange, statusFilter, officerFilter);
                } else {
                    HttpResponseUtil.sendResponse(exchange, 404, "{\"error\":\"Invalid GET endpoint path\"}");
                }
            } else {
                HttpResponseUtil.sendResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
            }
        } catch (InvalidStateTransitionException e) {
            logger.error("Case status update rejected: {}", e.getMessage());
            HttpResponseUtil.sendResponse(exchange, 409, "{\"error\":\"" + repository.DatabaseConnection.escape(e.getMessage()) + "\"}");
        } catch (SQLException | RuntimeException e) {
            logger.error("Unhandled error processing {} {}", method, path, e);
            HttpResponseUtil.sendResponse(exchange, 500, "{\"error\":\"" + repository.DatabaseConnection.escape(e.getMessage()) + "\"}");
        }
    }

    /**
     * Handles submission of a new document for a case.
     *
     * @param exchange current HTTP exchange
     * @param caseId owning case id
     * @throws IOException when response writing fails
     * @throws SQLException when persistence fails
     */
    private void handleUploadDocument(HttpExchange exchange, int caseId) throws IOException, SQLException {
        String body = readBody(exchange);
        Integer docTypeId = extractInt(body, "doc_type_id");
        if (docTypeId == null) {
            HttpResponseUtil.sendResponse(exchange, 400, "{\"error\":\"Missing required field: doc_type_id\"}");
            return;
        }

        int docId = caseService.uploadDocument(caseId, docTypeId);
        HttpResponseUtil.sendResponse(exchange, 201, "{\"message\":\"Document submitted successfully\",\"doc_id\":" + docId + "}");
    }

    /**
     * Handles creation of a new onboarding case.
     *
     * @param exchange current HTTP exchange
     * @throws IOException when response writing fails
     * @throws SQLException when persistence fails
     */
    private void handleCreateOnboardingCase(HttpExchange exchange) throws IOException, SQLException {
        String body = readBody(exchange);
        Integer clientId = extractInt(body, "client_id");
        String productType = extractString(body, "product_type");
        String caseStatus = extractString(body, "case_status");

        if (clientId == null || isBlank(productType) || isBlank(caseStatus)) {
            HttpResponseUtil.sendResponse(exchange, 400,
                    "{\"error\":\"Missing required fields: client_id, product_type, case_status\"}");
            return;
        }

        int newCaseId = caseService.createOnboardingCase(clientId, productType, caseStatus);
        HttpResponseUtil.sendResponse(exchange, 201, "{\"message\":\"Onboarding case opened successfully\",\"case_id\":" + newCaseId + "}");
    }

    /**
     * Handles a case status update request.
     *
     * @param exchange current HTTP exchange
     * @param caseId target case id
     * @throws IOException when response writing fails
     * @throws SQLException when persistence fails
     */
    private void handleUpdateCaseStatus(HttpExchange exchange, int caseId) throws IOException, SQLException {
        String body = readBody(exchange);
        String newStatus = extractString(body, "case_status");
        if (newStatus == null || newStatus.isEmpty()) {
            HttpResponseUtil.sendResponse(exchange, 400, "{\"error\":\"Missing 'case_status' in request body\"}");
            return;
        }

        boolean updated = caseService.updateCaseStatus(caseId, newStatus);
        if (updated) {
            HttpResponseUtil.sendResponse(exchange, 200, "{\"message\":\"Case status updated successfully\",\"case_id\":" + caseId
                    + ",\"case_status\":\"" + repository.DatabaseConnection.escape(newStatus) + "\"}");
        } else {
            logger.warn("Case status update failed: caseId={} reason=case not found", caseId);
            HttpResponseUtil.sendResponse(exchange, 404, "{\"error\":\"Case not found\"}");
        }
    }

    /**
     * Handles a case officer assignment request.
     *
     * @param exchange current HTTP exchange
     * @param caseId target case id
     * @throws IOException when response writing fails
     * @throws SQLException when persistence fails
     */
    private void handleAssignOfficer(HttpExchange exchange, int caseId) throws IOException, SQLException {
        String body = readBody(exchange);
        Integer officerId = extractInt(body, "officer_id");

        boolean updated = caseService.assignOfficer(caseId, officerId);
        if (updated) {
            String officerName = officerId == null ? null : officerService.getOfficerName(officerId);
            HttpResponseUtil.sendResponse(exchange, 200, "{\"message\":\"Case officer assigned successfully\",\"case_id\":" + caseId
                    + ",\"assigned_officer_id\":" + (officerId == null ? "null" : officerId)
                    + ",\"officer_name\":" + repository.DatabaseConnection.jsonStringOrNull(officerName) + "}");
        } else {
            logger.warn("Case officer assignment failed: caseId={} reason=case not found", caseId);
            HttpResponseUtil.sendResponse(exchange, 404, "{\"error\":\"Case not found\"}");
        }
    }

    /**
     * Handles a risk classification update, recording a new classification entry.
     *
     * @param exchange current HTTP exchange
     * @param caseId target case id
     * @throws IOException when response writing fails
     * @throws SQLException when persistence fails
     */
    private void handleUpdateRiskClassification(HttpExchange exchange, int caseId) throws IOException, SQLException {
        String body = readBody(exchange);
        String riskLevel = extractString(body, "risk_level");
        String rationale = extractString(body, "rationale");
        Integer officerId = extractInt(body, "officer_id");

        if (isBlank(riskLevel) || isBlank(rationale)) {
            HttpResponseUtil.sendResponse(exchange, 400, "{\"error\":\"Missing required fields: risk_level, rationale\"}");
            return;
        }

        try {
            caseService.updateRiskClassification(caseId, riskLevel, rationale, officerId);
            HttpResponseUtil.sendResponse(exchange, 200, "{\"message\":\"Risk classification updated successfully\",\"case_id\":" + caseId + "}");
        } catch (IllegalArgumentException e) {
            HttpResponseUtil.sendResponse(exchange, 400, "{\"error\":\"" + repository.DatabaseConnection.escape(e.getMessage()) + "\"}");
        }
    }

    /**
     * Handles verification of a document belonging to a case.
     *
     * @param exchange current HTTP exchange
     * @param caseId owning case id
     * @param docId document id
     * @throws IOException when response writing fails
     * @throws SQLException when persistence fails
     */
    private void handleVerifyDocument(HttpExchange exchange, int caseId, int docId) throws IOException, SQLException {
        boolean verified = caseService.verifyDocument(caseId, docId);
        if (verified) {
            HttpResponseUtil.sendResponse(exchange, 200, "{\"message\":\"Document verified successfully\",\"doc_id\":" + docId + "}");
        } else {
            HttpResponseUtil.sendResponse(exchange, 404, "{\"error\":\"Document not found or does not match the case\"}");
        }
    }

    /**
     * Handles listing of onboarding cases, optionally filtered by status and/or assigned officer.
     *
     * @param exchange current HTTP exchange
     * @param statusFilter status to filter by, or null for all cases
     * @param officerFilter assigned officer id to filter by, or null for all officers
     * @throws IOException when response writing fails
     * @throws SQLException when the query fails
     */
    private void handleListCases(HttpExchange exchange, String statusFilter, Integer officerFilter) throws IOException, SQLException {
        String json = caseService.listCases(statusFilter, officerFilter);
        HttpResponseUtil.sendResponse(exchange, 200, json);
    }

    /**
     * Extracts a single query-string parameter value.
     *
     * @param query raw query string, or null
     * @param key parameter name to look up
     * @return decoded value, or null when absent
     */
    private String parseQueryParam(String query, String key) {
        if (query == null || query.isEmpty()) {
            return null;
        }
        for (String pair : query.split("&")) {
            int eq = pair.indexOf('=');
            String pairKey = eq == -1 ? pair : pair.substring(0, eq);
            if (pairKey.equals(key)) {
                return eq == -1 ? "" : pair.substring(eq + 1);
            }
        }
        return null;
    }

    /**
     * Handles fetching full details of a case by id.
     *
     * @param exchange current HTTP exchange
     * @param id case id
     * @throws IOException when response writing fails
     * @throws SQLException when the query fails
     */
    private void handleGetCaseById(HttpExchange exchange, int id) throws IOException, SQLException {
        String json = caseService.getCaseById(id);
        if (json == null) {
            logger.warn("Case lookup failed: caseId={} reason=not found", id);
            HttpResponseUtil.sendResponse(exchange, 404, "{\"error\":\"Case not found\"}");
        } else {
            HttpResponseUtil.sendResponse(exchange, 200, json);
        }
    }

    /**
     * Checks whether a string is null or contains only whitespace.
     *
     * @param value string to validate
     * @return true when blank
     */
    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    /**
     * Reads the request body using UTF-8.
     *
     * @param exchange current HTTP exchange
     * @return body content
     * @throws IOException when body reading fails
     */
    private String readBody(HttpExchange exchange) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }

    /**
     * Extracts a string field from a flat JSON object.
     *
     * @param json source JSON
     * @param key field key
     * @return value or null when missing
     */
    private String extractString(String json, String key) {
        String searchKey = "\"" + key + "\"";
        int keyIndex = json.indexOf(searchKey);
        if (keyIndex == -1) {
            return null;
        }
        int colonIndex = json.indexOf(':', keyIndex + searchKey.length());
        if (colonIndex == -1) {
            return null;
        }
        int firstQuote = json.indexOf('"', colonIndex);
        if (firstQuote == -1) {
            return null;
        }
        int secondQuote = json.indexOf('"', firstQuote + 1);
        if (secondQuote == -1) {
            return null;
        }
        return json.substring(firstQuote + 1, secondQuote);
    }

    /**
     * Extracts an integer field from a flat JSON object.
     *
     * @param json source JSON
     * @param key field key
     * @return parsed integer or null when missing/invalid
     */
    private Integer extractInt(String json, String key) {
        String searchKey = "\"" + key + "\"";
        int keyIndex = json.indexOf(searchKey);
        if (keyIndex == -1) {
            return null;
        }
        int colonIndex = json.indexOf(':', keyIndex + searchKey.length());
        if (colonIndex == -1) {
            return null;
        }
        int valueStart = colonIndex + 1;
        while (valueStart < json.length() && Character.isWhitespace(json.charAt(valueStart))) {
            valueStart++;
        }

        int valueEnd = valueStart;
        while (valueEnd < json.length() && (Character.isDigit(json.charAt(valueEnd)) || json.charAt(valueEnd) == '-')) {
            valueEnd++;
        }

        if (valueStart == valueEnd) {
            return null;
        }

        try {
            return Integer.parseInt(json, valueStart, valueEnd, 10);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}