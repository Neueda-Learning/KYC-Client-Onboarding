package controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repository.DatabaseConnection;

/**
 * Reports whether the service can accept traffic, based on database connectivity.
 */
public class HealthHandler implements HttpHandler {
    private static final Logger logger = LoggerFactory.getLogger(HealthHandler.class);

    /**
     * Handles readiness check requests.
     *
     * @param exchange current HTTP exchange
     * @throws IOException when response writing fails
     */
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            logger.warn("Health check request rejected: method={} not allowed", exchange.getRequestMethod());
            KycApiServer.sendResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            if (conn.isValid(2)) {
                logger.info("Readiness check passed: database=UP");
                KycApiServer.sendResponse(exchange, 200, "{\"status\":\"UP\",\"database\":\"UP\"}");
            } else {
                logger.warn("Readiness check failed: database connection is not valid");
                KycApiServer.sendResponse(exchange, 503, "{\"status\":\"DOWN\",\"database\":\"DOWN\"}");
            }
        } catch (SQLException e) {
            logger.error("Readiness check failed: database unreachable", e);
            KycApiServer.sendResponse(exchange, 503,
                    "{\"status\":\"DOWN\",\"database\":\"DOWN\",\"error\":\""
                            + DatabaseConnection.escape(e.getMessage()) + "\"}");
        }
    }
}
