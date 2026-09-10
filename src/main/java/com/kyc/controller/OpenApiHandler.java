package controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.HttpResponseUtil;

/**
 * Serves the static OpenAPI specification describing all API endpoints.
 */
public class OpenApiHandler implements HttpHandler {
    private static final Logger logger = LoggerFactory.getLogger(OpenApiHandler.class);
    private static final Path SPEC_PATH = Path.of("openapi.yaml");

    /**
     * Serves the OpenAPI YAML spec file.
     *
     * @param exchange current HTTP exchange
     * @throws IOException when response writing fails
     */
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            logger.warn("OpenAPI spec request rejected: method={} not allowed", exchange.getRequestMethod());
            HttpResponseUtil.sendResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
            return;
        }

        if (!Files.exists(SPEC_PATH)) {
            logger.error("OpenAPI spec not found at {}", SPEC_PATH.toAbsolutePath());
            HttpResponseUtil.sendResponse(exchange, 500, "{\"error\":\"OpenAPI spec not found\"}");
            return;
        }

        byte[] bytes = Files.readAllBytes(SPEC_PATH);
        logger.info("OpenAPI spec served: bytes={}", bytes.length);
        exchange.getResponseHeaders().set("Content-Type", "application/yaml; charset=UTF-8");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
