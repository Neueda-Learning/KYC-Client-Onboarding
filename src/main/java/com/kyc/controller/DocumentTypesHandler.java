package controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.sql.SQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import service.DocumentTypeService;
import util.HttpResponseUtil;

/**
 * Handles the document types lookup HTTP endpoint.
 */
public class DocumentTypesHandler implements HttpHandler {
    private static final Logger logger = LoggerFactory.getLogger(DocumentTypesHandler.class);
    private final DocumentTypeService documentTypeService = new DocumentTypeService();

    /**
     * Dispatches requests for /api/document-types routes.
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
            String json = documentTypeService.listDocumentTypes();
            logger.info("Document types listed successfully");
            HttpResponseUtil.sendResponse(exchange, 200, json);
        } catch (SQLException | RuntimeException e) {
            logger.error("Unhandled error processing {} {}", method, exchange.getRequestURI().getPath(), e);
            HttpResponseUtil.sendResponse(exchange, 500, "{\"error\":\"" + repository.DatabaseConnection.escape(e.getMessage()) + "\"}");
        }
    }
}