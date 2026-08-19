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
import repository.DatabaseConnection;
import service.AuthService;
import util.HttpResponseUtil;

/**
 * Handles the login HTTP endpoint.
 */
public class AuthHandler implements HttpHandler {
    private static final Logger logger = LoggerFactory.getLogger(AuthHandler.class);
    private final AuthService authService = new AuthService();

    /**
     * Dispatches requests for /api/auth routes.
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

        try {
            if ("POST".equalsIgnoreCase(method) && "/api/auth/login".equals(path)) {
                handleLogin(exchange);
            } else {
                HttpResponseUtil.sendResponse(exchange, 404, "{\"error\":\"Invalid endpoint path\"}");
            }
        } catch (SQLException | RuntimeException e) {
            logger.error("Unhandled error processing {} {}", method, path, e);
            HttpResponseUtil.sendResponse(exchange, 500, "{\"error\":\"" + DatabaseConnection.escape(e.getMessage()) + "\"}");
        }
    }

    /**
     * Handles a login attempt against client, compliance officer and admin officer accounts.
     *
     * @param exchange current HTTP exchange
     * @throws IOException when response writing fails
     * @throws SQLException when the credential lookup fails
     */
    private void handleLogin(HttpExchange exchange) throws IOException, SQLException {
        String body = readBody(exchange);
        String username = extractString(body, "username");
        String password = extractString(body, "password");

        if (isBlank(username) || isBlank(password)) {
            HttpResponseUtil.sendResponse(exchange, 400, "{\"error\":\"Missing required fields: username, password\"}");
            return;
        }

        AuthService.LoginResult result = authService.login(username, password);
        if (result == null) {
            logger.warn("Login failed: username={}", username);
            // Deliberately generic message so failed lookups don't reveal whether the username exists.
            HttpResponseUtil.sendResponse(exchange, 401, "{\"error\":\"Invalid username or password\"}");
            return;
        }

        String json = "{"
                + "\"role\":\"" + result.role + "\","
                + "\"entity_id\":" + result.entityId + ","
                + "\"full_name\":" + DatabaseConnection.jsonStringOrNull(result.fullName) + ","
                + "\"username\":" + DatabaseConnection.jsonStringOrNull(result.username)
                + "}";
        HttpResponseUtil.sendResponse(exchange, 200, json);
    }

    private String readBody(HttpExchange exchange) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }

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

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
