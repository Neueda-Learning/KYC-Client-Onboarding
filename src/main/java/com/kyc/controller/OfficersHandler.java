package controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.sql.SQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import service.OfficerService;
import util.HttpResponseUtil;

/**
 * Handles compliance officer lookup HTTP endpoints.
 */
public class OfficersHandler implements HttpHandler {
    private static final Logger logger = LoggerFactory.getLogger(OfficersHandler.class);
    private final OfficerService officerService = new OfficerService();

    /**
     * Dispatches requests for /api/officers routes.
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
        if (!"GET".equalsIgnoreCase(method)) {
            HttpResponseUtil.sendResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
            return;
        }
        try {
            String json = officerService.listOfficers();
            logger.info("Officers listed successfully");
            HttpResponseUtil.sendResponse(exchange, 200, json);
        } catch (SQLException | RuntimeException e) {
            logger.error("Unhandled error processing {} {}", method, exchange.getRequestURI().getPath(), e);
            HttpResponseUtil.sendResponse(exchange, 500, "{\"error\":\"" + repository.DatabaseConnection.escape(e.getMessage()) + "\"}");
        }
    }
}
