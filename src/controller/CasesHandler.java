package controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import service.CaseService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.stream.Collectors;

public class CasesHandler implements HttpHandler {
    private final CaseService caseService = new CaseService();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
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
                        KycApiServer.sendResponse(exchange, 400, "{\"error\":\"Invalid case ID\"}");
                    }
                } else {
                    handleCreateOnboardingCase(exchange);
                }
            } else if ("PATCH".equalsIgnoreCase(method)) {
                if (parts.length == 6 && "status".equals(parts[5])) {
                    try {
                        int caseId = Integer.parseInt(parts[4]);
                        handleUpdateCaseStatus(exchange, caseId);
                    } catch (NumberFormatException e) {
                        KycApiServer.sendResponse(exchange, 400, "{\"error\":\"Invalid case ID\"}");
                    }
                } else if (parts.length >= 8 && "documents".equals(parts[5]) && "verify".equals(parts[7])) {
                    try {
                        int caseId = Integer.parseInt(parts[4]);
                        int docId = Integer.parseInt(parts[6]);
                        handleVerifyDocument(exchange, caseId, docId);
                    } catch (NumberFormatException e) {
                        KycApiServer.sendResponse(exchange, 400, "{\"error\":\"Invalid case ID or document ID\"}");
                    }
                } else {
                    KycApiServer.sendResponse(exchange, 400, "{\"error\":\"Invalid PATCH endpoint path\"}");
                }
            } else if ("GET".equalsIgnoreCase(method)) {
                if (parts.length >= 5 && !parts[4].isEmpty()) {
                    try {
                        handleGetCaseById(exchange, Integer.parseInt(parts[4]));
                    } catch (NumberFormatException e) {
                        KycApiServer.sendResponse(exchange, 400, "{\"error\":\"Invalid case ID\"}");
                    }
                } else {
                    String statusFilter = null;
                    if (query != null && query.startsWith("status=")) {
                        statusFilter = query.substring(7);
                    }
                    handleListCases(exchange, statusFilter);
                }
            } else {
                KycApiServer.sendResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
            }
        } catch (Exception e) {
            KycApiServer.sendResponse(exchange, 500, "{\"error\":\"" + repository.DatabaseConnection.escape(e.getMessage()) + "\"}");
        }
    }

    private void handleUploadDocument(HttpExchange exchange, int caseId) throws IOException, SQLException {
        int docId = caseService.uploadDocument(caseId);
        KycApiServer.sendResponse(exchange, 201, "{\"message\":\"Document submitted successfully\",\"doc_id\":" + docId + "}");
    }

    private void handleCreateOnboardingCase(HttpExchange exchange) throws IOException, SQLException {
        int newCaseId = caseService.createOnboardingCase();
        KycApiServer.sendResponse(exchange, 201, "{\"message\":\"Onboarding case opened successfully\",\"case_id\":" + newCaseId + "}");
    }

    private void handleUpdateCaseStatus(HttpExchange exchange, int caseId) throws IOException, SQLException {
        String body;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
            body = reader.lines().collect(Collectors.joining("\n"));
        }

        String newStatus = extractJsonValue(body, "case_status");
        if (newStatus == null || newStatus.isEmpty()) {
            KycApiServer.sendResponse(exchange, 400, "{\"error\":\"Missing 'case_status' in request body\"}");
            return;
        }

        boolean updated = caseService.updateCaseStatus(caseId, newStatus);
        if (updated) {
            KycApiServer.sendResponse(exchange, 200, "{\"message\":\"Case status updated successfully\",\"case_id\":" + caseId
                    + ",\"case_status\":\"" + repository.DatabaseConnection.escape(newStatus) + "\"}");
        } else {
            KycApiServer.sendResponse(exchange, 404, "{\"error\":\"Case not found\"}");
        }
    }

    private void handleVerifyDocument(HttpExchange exchange, int caseId, int docId) throws IOException, SQLException {
        boolean verified = caseService.verifyDocument(caseId, docId);
        if (verified) {
            KycApiServer.sendResponse(exchange, 200, "{\"message\":\"Document verified successfully\",\"doc_id\":" + docId + "}");
        } else {
            KycApiServer.sendResponse(exchange, 404, "{\"error\":\"Document not found or does not match the case\"}");
        }
    }

    private void handleListCases(HttpExchange exchange, String statusFilter) throws IOException, SQLException {
        String json = caseService.listCases(statusFilter);
        KycApiServer.sendResponse(exchange, 200, json);
    }

    private void handleGetCaseById(HttpExchange exchange, int id) throws IOException, SQLException {
        String json = caseService.getCaseById(id);
        if (json == null) {
            KycApiServer.sendResponse(exchange, 404, "{\"error\":\"Case not found\"}");
        } else {
            KycApiServer.sendResponse(exchange, 200, json);
        }
    }

    private String extractJsonValue(String json, String key) {
        String searchKey = "\"" + key + "\"";
        int keyIndex = json.indexOf(searchKey);
        if (keyIndex == -1)
            return null;
        int colonIndex = json.indexOf(':', keyIndex + searchKey.length());
        if (colonIndex == -1)
            return null;
        int firstQuote = json.indexOf('"', colonIndex);
        if (firstQuote == -1)
            return null;
        int secondQuote = json.indexOf('"', firstQuote + 1);
        if (secondQuote == -1)
            return null;
        return json.substring(firstQuote + 1, secondQuote);
    }
}