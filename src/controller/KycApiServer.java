package controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

/**
 * Lightweight HTTP API server for the KYC Client Onboarding system.
 */
public class KycApiServer {

    public static void main(String[] args) throws Exception {
        int port = 8080;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/api/clients", new ClientsHandler());
        server.createContext("/api/onboarding/cases", new CasesHandler());
        server.setExecutor(null);

        System.out.println("KYC API Server started on port " + port);
        System.out.println("  GET/POST http://localhost:" + port + "/api/clients");
        System.out.println("  GET http://localhost:" + port + "/api/clients/{id}");
        System.out.println("  GET http://localhost:" + port + "/api/clients/expiring-documents?days={days}");
        System.out.println("  POST http://localhost:" + port + "/api/onboarding/cases");
        System.out.println("  GET http://localhost:" + port + "/api/onboarding/cases/{id}");
        System.out.println("  GET http://localhost:" + port + "/api/onboarding/cases?status={status}");
        server.start();
    }

    public static void sendResponse(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes("UTF-8");
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}