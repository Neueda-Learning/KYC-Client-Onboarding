package controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import service.ClientService;

import java.io.IOException;
import java.sql.SQLException;

public class ClientsHandler implements HttpHandler {
    private final ClientService clientService = new ClientService();

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
                        } catch (NumberFormatException ignored) {
                        }
                    }
                    handleListExpiringDocuments(exchange, days);
                } else if (parts.length == 4 && !parts[3].isEmpty()) {
                    try {
                        handleGetClientById(exchange, Integer.parseInt(parts[3]));
                    } catch (NumberFormatException e) {
                        KycApiServer.sendResponse(exchange, 400, "{\"error\":\"Invalid client ID\"}");
                    }
                } else {
                    handleListClients(exchange);
                }
            } else if ("POST".equalsIgnoreCase(method)) {
                handleCreateClient(exchange);
            } else {
                KycApiServer.sendResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
            }
        } catch (Exception e) {
            KycApiServer.sendResponse(exchange, 500, "{\"error\":\"" + repository.DatabaseConnection.escape(e.getMessage()) + "\"}");
        }
    }

    private void handleListExpiringDocuments(HttpExchange exchange, int days) throws IOException, SQLException {
        String json = clientService.listExpiringDocuments(days);
        KycApiServer.sendResponse(exchange, 200, json);
    }

    private void handleGetClientById(HttpExchange exchange, int id) throws IOException, SQLException {
        String json = clientService.getClientById(id);
        if (json == null) {
            KycApiServer.sendResponse(exchange, 404, "{\"error\":\"Client not found\"}");
        } else {
            KycApiServer.sendResponse(exchange, 200, json);
        }
    }

    private void handleListClients(HttpExchange exchange) throws IOException, SQLException {
        String json = clientService.listClients();
        KycApiServer.sendResponse(exchange, 200, json);
    }

    private void handleCreateClient(HttpExchange exchange) throws IOException, SQLException {
        int newId = clientService.createClient();
        KycApiServer.sendResponse(exchange, 201, "{\"message\":\"Client created successfully\",\"client_id\":" + newId + "}");
    }
}