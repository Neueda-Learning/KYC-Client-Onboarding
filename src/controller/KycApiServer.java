package controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lightweight HTTP API server for the KYC Client Onboarding system.
 */
public class KycApiServer {
    private static final Logger logger = LoggerFactory.getLogger(KycApiServer.class);

    /**
     * Starts the HTTP API server.
     *
     * @param args unused command-line arguments
     * @throws Exception when the server fails to start
     */
    public static void main(String[] args) throws Exception {
        int port = 8080;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/api/clients", new ClientsHandler());
        server.createContext("/api/onboarding/cases", new CasesHandler());
        server.setExecutor(null);

        logger.info("KYC API Server started on port {}", port);
        server.start();
    }

    /**
     * Writes a JSON response body with the given HTTP status.
     *
     * @param exchange current HTTP exchange
     * @param status HTTP status code
     * @param body JSON response body
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