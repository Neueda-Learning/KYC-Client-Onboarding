import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import service.DocumentExpiryScheduledJob;
import controller.ClientsHandler;
import controller.CasesHandler;
import controller.HealthHandler;
import controller.OpenApiHandler;

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
        server.createContext("/health", new HealthHandler());
        server.createContext("/openapi.yaml", new OpenApiHandler());
        server.setExecutor(null);

        new DocumentExpiryScheduledJob().start();

        logger.info("KYC API Server started on port {}", port);
        server.start();
    }
}