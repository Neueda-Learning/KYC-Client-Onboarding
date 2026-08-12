import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.sql.*;

/**
 * Lightweight HTTP API server for the KYC Client Onboarding system.
 *
 * <p>Uses the JDK built-in {@code com.sun.net.httpserver} — no external framework required
 * beyond the MySQL JDBC driver. Credentials are read from environment variables
 * ({@code MYSQL_USER}, {@code MYSQL_PASSWORD}); see {@code .env} at the repo root.
 *
 * <p>Endpoints:
 * <pre>
 *   GET /api/clients             – list all clients (summary fields)
 *   GET /api/clients/{id}        – full client record by internal ID
 *   GET /api/onboarding/cases/{id} – case details with submitted document checklist
 * </pre>
 *
 * <p>Compile and run from the {@code src/} directory:
 * <pre>
 *   javac -cp "lib/mysql-connector-j-8.3.0.jar" KycApiServer.java
 *   java  -cp ".;lib/mysql-connector-j-8.3.0.jar" KycApiServer   # Windows
 *   java  -cp ".:lib/mysql-connector-j-8.3.0.jar" KycApiServer   # Linux/macOS
 * </pre>
 */
public class KycApiServer {

    private static final String DB_URL      = "jdbc:mysql://localhost:3306/kyc_db";
    private static final String DB_USER     = System.getenv().getOrDefault("MYSQL_USER", "root");
    private static final String DB_PASSWORD = System.getenv().getOrDefault("MYSQL_PASSWORD", "");

    public static void main(String[] args) throws Exception {
        int port = 8080;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/api/clients",           new ClientsHandler());
        server.createContext("/api/onboarding/cases",  new CasesHandler());
        server.setExecutor(null);

        System.out.println("KYC API Server started on port " + port);
        System.out.println("  GET http://localhost:" + port + "/api/clients");
        System.out.println("  GET http://localhost:" + port + "/api/clients/{id}");
        System.out.println("  GET http://localhost:" + port + "/api/onboarding/cases/{id}");
        server.start();
    }

    // --- Handlers ---

    /** Routes {@code /api/clients} and {@code /api/clients/{id}} to the appropriate handler. */
    static class ClientsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
                return;
            }
            String[] parts = exchange.getRequestURI().getPath().split("/");
            // ["", "api", "clients"] or ["", "api", "clients", "42"]
            try {
                if (parts.length == 4 && !parts[3].isEmpty()) {
                    handleGetClientById(exchange, Integer.parseInt(parts[3]));
                } else {
                    handleListClients(exchange);
                }
            } catch (NumberFormatException e) {
                sendResponse(exchange, 400, "{\"error\":\"Invalid client ID\"}");
            }
        }
    }

    /** Routes {@code /api/onboarding/cases/{id}} — case ID is mandatory. */
    static class CasesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
                return;
            }
            String[] parts = exchange.getRequestURI().getPath().split("/");
            // ["", "api", "onboarding", "cases", "42"]
            if (parts.length < 5 || parts[4].isEmpty()) {
                sendResponse(exchange, 400, "{\"error\":\"Case ID required: /api/onboarding/cases/{id}\"}");
                return;
            }
            try {
                handleGetCaseById(exchange, Integer.parseInt(parts[4]));
            } catch (NumberFormatException e) {
                sendResponse(exchange, 400, "{\"error\":\"Invalid case ID\"}");
            }
        }
    }

    // --- Query methods ---

    /** Returns a summary list of all clients (id, name, type, nationality, status, is_active). */
    static void handleListClients(HttpExchange exchange) throws IOException {
        String sql = "SELECT client_id, full_name, client_type, nationality, status, is_active FROM client";
        StringBuilder json = new StringBuilder("[\n");
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            boolean first = true;
            while (rs.next()) {
                if (!first) json.append(",\n");
                json.append("  {")
                    .append("\"client_id\":").append(rs.getInt("client_id")).append(",")
                    .append("\"full_name\":\"").append(escape(rs.getString("full_name"))).append("\",")
                    .append("\"client_type\":\"").append(escape(rs.getString("client_type"))).append("\",")
                    .append("\"nationality\":\"").append(escape(rs.getString("nationality"))).append("\",")
                    .append("\"status\":\"").append(escape(rs.getString("status"))).append("\",")
                    .append("\"is_active\":").append(rs.getBoolean("is_active"))
                    .append("}");
                first = false;
            }
        } catch (SQLException e) {
            sendResponse(exchange, 500, "{\"error\":\"" + escape(e.getMessage()) + "\"}");
            return;
        }
        json.append("\n]");
        sendResponse(exchange, 200, json.toString());
    }

    /** Returns the full client record for {@code id}, or 404 if not found. */
    static void handleGetClientById(HttpExchange exchange, int id) throws IOException {
        String sql = "SELECT client_id, full_name, client_type, nationality, date_of_birth, " +
                     "country_of_birth, tax_residency, occupation, employer, main_source_of_funds, " +
                     "annual_income_band, status, is_active FROM client WHERE client_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    sendResponse(exchange, 404, "{\"error\":\"Client not found\"}");
                    return;
                }
                String json = "{"
                    + "\"client_id\":"            + rs.getInt("client_id")                              + ","
                    + "\"full_name\":\""           + escape(rs.getString("full_name"))          + "\","
                    + "\"client_type\":\""         + escape(rs.getString("client_type"))        + "\","
                    + "\"nationality\":\""         + escape(rs.getString("nationality"))        + "\","
                    + "\"date_of_birth\":\""       + rs.getString("date_of_birth")              + "\","
                    + "\"country_of_birth\":\""    + escape(rs.getString("country_of_birth"))   + "\","
                    + "\"tax_residency\":\""       + escape(rs.getString("tax_residency"))      + "\","
                    + "\"occupation\":"            + jsonStringOrNull(rs.getString("occupation"))        + ","
                    + "\"employer\":"              + jsonStringOrNull(rs.getString("employer"))          + ","
                    + "\"main_source_of_funds\":"  + jsonStringOrNull(rs.getString("main_source_of_funds")) + ","
                    + "\"annual_income_band\":"    + jsonStringOrNull(rs.getString("annual_income_band")) + ","
                    + "\"status\":\""              + escape(rs.getString("status"))             + "\","
                    + "\"is_active\":"             + rs.getBoolean("is_active")
                    + "}";
                sendResponse(exchange, 200, json);
            }
        } catch (SQLException e) {
            sendResponse(exchange, 500, "{\"error\":\"" + escape(e.getMessage()) + "\"}");
        }
    }

    /**
     * Returns case details joined with client info, plus all documents submitted for the case.
     * Documents are fetched from the {@code document} and {@code document_type} tables.
     * Returns 404 if the case ID does not exist.
     */
    static void handleGetCaseById(HttpExchange exchange, int id) throws IOException {
        String caseSql =
            "SELECT oc.case_id, oc.client_id, oc.opened_date, oc.product_type, oc.case_status, " +
            "oc.due_date, oc.completed_date, oc.rejection_reason, " +
            "c.full_name AS client_name, c.client_type " +
            "FROM onboarding_case oc JOIN client c ON oc.client_id = c.client_id " +
            "WHERE oc.case_id = ?";
        String docSql =
            "SELECT d.doc_id, dt.doc_type_name, d.submission_date, " +
            "d.verified_flag, d.expiry_date, d.rejection_reason " +
            "FROM document d JOIN document_type dt ON d.doc_type_id = dt.doc_type_id " +
            "WHERE d.case_id = ?";

        try (Connection conn = getConnection()) {
            StringBuilder json = new StringBuilder();

            try (PreparedStatement ps = conn.prepareStatement(caseSql)) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        sendResponse(exchange, 404, "{\"error\":\"Case not found\"}");
                        return;
                    }
                    json.append("{")
                        .append("\"case_id\":").append(rs.getInt("case_id")).append(",")
                        .append("\"client_id\":").append(rs.getInt("client_id")).append(",")
                        .append("\"client_name\":\"").append(escape(rs.getString("client_name"))).append("\",")
                        .append("\"client_type\":\"").append(escape(rs.getString("client_type"))).append("\",")
                        .append("\"product_type\":\"").append(escape(rs.getString("product_type"))).append("\",")
                        .append("\"case_status\":\"").append(escape(rs.getString("case_status"))).append("\",")
                        .append("\"opened_date\":\"").append(rs.getString("opened_date")).append("\",")
                        .append("\"due_date\":").append(jsonStringOrNull(rs.getString("due_date"))).append(",")
                        .append("\"completed_date\":").append(jsonStringOrNull(rs.getString("completed_date"))).append(",")
                        .append("\"rejection_reason\":").append(jsonStringOrNull(rs.getString("rejection_reason")));
                }
            }

            json.append(",\"documents\":[");
            try (PreparedStatement ps = conn.prepareStatement(docSql)) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    boolean first = true;
                    while (rs.next()) {
                        if (!first) json.append(",");
                        json.append("{")
                            .append("\"doc_id\":").append(rs.getInt("doc_id")).append(",")
                            .append("\"doc_type\":\"").append(escape(rs.getString("doc_type_name"))).append("\",")
                            .append("\"submission_date\":\"").append(rs.getString("submission_date")).append("\",")
                            .append("\"verified\":").append(rs.getBoolean("verified_flag")).append(",")
                            .append("\"expiry_date\":").append(jsonStringOrNull(rs.getString("expiry_date"))).append(",")
                            .append("\"rejection_reason\":").append(jsonStringOrNull(rs.getString("rejection_reason")))
                            .append("}");
                        first = false;
                    }
                }
            }
            json.append("]}");
            sendResponse(exchange, 200, json.toString());

        } catch (SQLException e) {
            sendResponse(exchange, 500, "{\"error\":\"" + escape(e.getMessage()) + "\"}");
        }
    }

    // --- Helpers ---

    /** Opens a new JDBC connection using the configured DB URL and credentials. */
    static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    /** Writes a JSON response with the given HTTP status code. */
    static void sendResponse(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes("UTF-8");
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    // Escapes characters that would break a JSON string value
    static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }

    static String jsonStringOrNull(String s) {
        return s == null ? "null" : "\"" + escape(s) + "\"";
    }
}
