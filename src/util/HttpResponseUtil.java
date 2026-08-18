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
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}