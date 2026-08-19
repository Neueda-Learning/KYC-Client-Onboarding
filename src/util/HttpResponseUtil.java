package util;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Shared helper for writing HTTP responses.
 */
public class HttpResponseUtil {

    private HttpResponseUtil() {
    }

    /**
     * Writes a JSON response body with the given HTTP status.
     *
     * @param exchange current HTTP exchange
     * @param status   HTTP status code
     * @param body     JSON response body
     * @throws IOException when writing the response fails
     */
    public static void sendResponse(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes("UTF-8");
        addCorsHeaders(exchange);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    /**
     * Adds permissive CORS headers so the frontend (served from a different
     * origin/port during development) can call this API from the browser.
     *
     * @param exchange current HTTP exchange
     */
    public static void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PATCH, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
    }

    /**
     * Handles CORS preflight (OPTIONS) requests. Call this first in a handler's
     * {@code handle} method and return immediately if it reports the request
     * was handled.
     *
     * @param exchange current HTTP exchange
     * @return true if the request was an OPTIONS preflight and has been fully handled
     * @throws IOException when writing the response fails
     */
    public static boolean handlePreflight(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange);
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
            return true;
        }
        return false;
    }
}