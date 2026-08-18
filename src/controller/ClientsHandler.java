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
import service.ClientService;
import util.HttpResponseUtil;

/**
 * Handles client-related HTTP endpoints.
 */
public class ClientsHandler implements HttpHandler {
    private static final Logger logger = LoggerFactory.getLogger(ClientsHandler.class);
    private final ClientService clientService = new ClientService();

    /**
     * Dispatches requests for /api/clients routes.
     *
     * @param exchange current HTTP exchange
     * @throws IOException when response writing fails
     */
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        String query = exchange.getRequestURI().getQuery();
        String[] parts = path.split("/");

        try {
            if ("GET".equalsIgnoreCase(method)) {
                if (parts.length == 4 && "expiring-documents".equals(parts[3])) {
                    int days = 30;
                    if (query != null && query.startsWith("days=")) {
                        try {
                            days = Integer.parseInt(query.substring(5));
                        } catch (NumberFormatException e) {
                            HttpResponseUtil.sendResponse(exchange, 400, "{\"error\":\"Invalid days parameter: " + repository.DatabaseConnection.escape(e.getMessage()) + "\"}");
                            return;
                        }
                    }
                    handleListExpiringDocuments(exchange, days);
                } else if (parts.length == 4 && !parts[3].isEmpty()) {
                    try {
                        handleGetClientById(exchange, Integer.parseInt(parts[3]));
                    } catch (NumberFormatException e) {
                        HttpResponseUtil.sendResponse(exchange, 400, "{\"error\":\"Invalid client ID: " + repository.DatabaseConnection.escape(e.getMessage()) + "\"}");
                    }
                } else if (parts.length == 3 && "clients".equals(parts[2])) {
                    handleListClients(exchange);
                } else {
                    HttpResponseUtil.sendResponse(exchange, 404, "{\"error\":\"Invalid GET endpoint path\"}");
                }
            } else if ("POST".equalsIgnoreCase(method)) {
                if (parts.length == 3 && "clients".equals(parts[2])) {
                    handleCreateClient(exchange);
                } else {
                    HttpResponseUtil.sendResponse(exchange, 404, "{\"error\":\"Invalid POST endpoint path\"}");
                }
            } else {
                HttpResponseUtil.sendResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
            }
        } catch (SQLException | RuntimeException e) {
            logger.error("Unhandled error processing {} {}", method, path, e);
            HttpResponseUtil.sendResponse(exchange, 500, "{\"error\":\"" + repository.DatabaseConnection.escape(e.getMessage()) + "\"}");
        }
    }

    /**
     * Handles listing documents that are expiring within the given window.
     *
     * @param exchange current HTTP exchange
     * @param days lookahead window in days
     * @throws IOException when response writing fails
     * @throws SQLException when the query fails
     */
    private void handleListExpiringDocuments(HttpExchange exchange, int days) throws IOException, SQLException {
        String json = clientService.listExpiringDocuments(days);
        HttpResponseUtil.sendResponse(exchange, 200, json);
    }

    /**
     * Handles fetching the full record for a client by id.
     *
     * @param exchange current HTTP exchange
     * @param id client id
     * @throws IOException when response writing fails
     * @throws SQLException when the query fails
     */
    private void handleGetClientById(HttpExchange exchange, int id) throws IOException, SQLException {
        String json = clientService.getClientById(id);
        if (json == null) {
            logger.warn("Client lookup failed: clientId={} reason=not found", id);
            HttpResponseUtil.sendResponse(exchange, 404, "{\"error\":\"Client not found\"}");
        } else {
            HttpResponseUtil.sendResponse(exchange, 200, json);
        }
    }

    /**
     * Handles listing a summary of all clients.
     *
     * @param exchange current HTTP exchange
     * @throws IOException when response writing fails
     * @throws SQLException when the query fails
     */
    private void handleListClients(HttpExchange exchange) throws IOException, SQLException {
        String json = clientService.listClients();
        HttpResponseUtil.sendResponse(exchange, 200, json);
    }

    /**
     * Handles creation of a new client.
     *
     * @param exchange current HTTP exchange
     * @throws IOException when response writing fails
     * @throws SQLException when persistence fails
     */
    private void handleCreateClient(HttpExchange exchange) throws IOException, SQLException {
        String body = readBody(exchange);

        String fullName = extractString(body, "full_name");
        String clientType = extractString(body, "client_type");
        String nationality = extractString(body, "nationality");
        String countryOfBirth = extractString(body, "country_of_birth");
        String dateOfBirth = extractString(body, "date_of_birth");
        String taxResidency = extractString(body, "tax_residency");
        String status = extractString(body, "status");
        Boolean isActive = extractBoolean(body, "is_active");

        if (isBlank(fullName) || isBlank(clientType) || isBlank(nationality) || isBlank(countryOfBirth)
                || isBlank(dateOfBirth) || isBlank(taxResidency) || isBlank(status) || isActive == null) {
            HttpResponseUtil.sendResponse(exchange, 400,
                    "{\"error\":\"Missing required fields: full_name, client_type, nationality, country_of_birth, date_of_birth, tax_residency, status, is_active\"}");
            return;
        }

        int newId = clientService.createClient(fullName, clientType, nationality, countryOfBirth, dateOfBirth,
                taxResidency, status, isActive);
        HttpResponseUtil.sendResponse(exchange, 201, "{\"message\":\"Client created successfully\",\"client_id\":" + newId + "}");
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
     * Extracts a boolean field from a flat JSON object.
     *
     * @param json source JSON
     * @param key field key
     * @return parsed boolean or null when missing/invalid
     */
    private Boolean extractBoolean(String json, String key) {
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

        if (json.regionMatches(true, valueStart, "true", 0, 4)) {
            return Boolean.TRUE;
        }
        if (json.regionMatches(true, valueStart, "false", 0, 5)) {
            return Boolean.FALSE;
        }
        return null;
    }
}