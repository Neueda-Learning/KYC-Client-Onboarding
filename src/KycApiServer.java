import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class KycApiServer {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/kyc_db";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "n3u3da!";

    public static void main(String[] args) throws Exception {
        int port = 8080;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        
        // Define endpoint /api/clients
        server.createContext("/api/clients", new ClientsHandler());
        server.setExecutor(null);
        
        System.out.println("KYC API Relay Server started on port " + port);
        System.out.println("Access clients at: http://localhost:8080/api/clients");
        server.start();
    }

    static class ClientsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            StringBuilder response = new StringBuilder();
            response.append("[\n");

            try {
                // Connect to MySQL database
                Class.forName("com.mysql.cj.jdbc.Driver");
                Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT client_id, full_name, client_type, status FROM client");

                boolean first = true;
                while (rs.next()) {
                    if (!first) {
                        response.append(",\n");
                    }
                    response.append("  {\n");
                    response.append("    \"client_id\": ").append(rs.getInt("client_id")).append(",\n");
                    response.append("    \"full_name\": \"").append(rs.getString("full_name")).append("\",\n");
                    response.append("    \"client_type\": \"").append(rs.getString("client_type")).append("\",\n");
                    response.append("    \"status\": \"").append(rs.getString("status")).append("\"\n");
                    response.append("  }");
                    first = false;
                }

                rs.close();
                stmt.close();
                conn.close();
            } catch (Exception e) {
                response = new StringBuilder("{\"error\": \"" + e.getMessage() + "\"}");
            }

            response.append("\n]");

            byte[] responseBytes = response.toString().getBytes("UTF-8");
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(200, responseBytes.length);
            
            OutputStream os = exchange.getResponseBody();
            os.write(responseBytes);
            os.close();
        }
    }
}