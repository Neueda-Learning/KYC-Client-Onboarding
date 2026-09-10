package service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.SQLException;
import java.time.Duration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repository.CaseRepository;
import repository.DatabaseConnection;

/**
 * Calls the external FastAPI ML/XAI service (see data_analysis/app.py) to
 * predict case approval and, for predicted rejections, generate DiCE
 * counterfactual recommendations, then persists the result on the case.
 * Triggered on: case open, document submission, document verification (see
 * OnboardingService/CaseService). Prediction is best-effort: any failure
 * (case not found, ML service unreachable, malformed response) is logged and
 * swallowed so it never blocks the underlying onboarding operation.
 */
public class MLPredictionService {
    private static final Logger logger = LoggerFactory.getLogger(MLPredictionService.class);

    private static final String ML_SERVICE_URL = System.getenv().getOrDefault(
            "ML_SERVICE_URL", "http://localhost:8000/api/v1/predict-and-explain");

    private final CaseRepository caseRepository;
    private final HttpClient httpClient;

    public MLPredictionService() {
        this(new CaseRepository());
    }

    public MLPredictionService(CaseRepository caseRepository) {
        this.caseRepository = caseRepository;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    /**
     * Runs a prediction for a case and persists the result. Never throws -
     * failures are logged and skipped so callers can invoke this fire-and-forget.
     *
     * @param caseId target case id
     */
    public void predictAndSave(int caseId) {
        try {
            CaseRepository.MlFeatures features = caseRepository.getMlFeatures(caseId);
            if (features == null) {
                logger.warn("ML prediction skipped: caseId={} reason=case not found", caseId);
                return;
            }

            String jsonBody = toJson(features);
            logger.debug("ML prediction request body: caseId={} payload={}", caseId, jsonBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(ML_SERVICE_URL))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                logger.warn("ML prediction request failed: caseId={} status={} body={}", caseId,
                        response.statusCode(), response.body());
                return;
            }

            String body = response.body();
            logger.info("ML prediction response: caseId={} body={}", caseId, body);
            String decision = extractString(body, "decision");
            Double probability = extractDouble(body, "approval_probability");
            String recommendations = extractArrayOrNull(body, "recommendations");
            if (decision == null || probability == null) {
                logger.warn("ML prediction response malformed: caseId={} body={}", caseId, body);
                return;
            }

            logger.info("ML prediction parsed: caseId={} decision={} probability={} recommendations={}", 
                    caseId, decision, probability, recommendations);
            caseRepository.saveMlPrediction(caseId, decision, probability, recommendations);
            logger.info("ML prediction computed: caseId={} decision={} probability={}", caseId, decision,
                    probability);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("ML prediction interrupted: caseId={}", caseId);
        } catch (SQLException | IOException | RuntimeException e) {
            logger.warn("ML prediction failed: caseId={} reason={}", caseId, e.getMessage());
        }
    }

    public void predictAllCases() {
        try {
            List<Integer> allCaseIds = caseRepository.getAllCaseIds();
            logger.info("Starting batch prediction for {} cases...", allCaseIds.size());
            for (int caseId : allCaseIds) {
                predictAndSave(caseId);
            }
            logger.info("Batch prediction finished.");
        } catch (SQLException e) {
            logger.warn("Batch prediction failed: reason={}", e.getMessage());
        }
    }

    /**
     * Predicts every case not yet CLOSED. Run once on server startup so
     * predictions are already populated for officers instead of only
     * appearing after a case-mutating action (open/document submit/verify).
     */
    public void predictOpenCases() {
        try {
            List<Integer> openCaseIds = caseRepository.getOpenCaseIds();
            logger.info("Starting startup batch prediction for {} non-CLOSED cases...", openCaseIds.size());
            for (int caseId : openCaseIds) {
                predictAndSave(caseId);
            }
            logger.info("Startup batch prediction finished.");
        } catch (SQLException e) {
            logger.warn("Startup batch prediction failed: reason={}", e.getMessage());
        }
    }

    /**
     * Serializes case features into the JSON body expected by the FastAPI
     * /api/v1/predict-and-explain endpoint.
     *
     * @param f gathered case/client/document attributes
     * @return JSON request body
     */
    private String toJson(CaseRepository.MlFeatures f) {
        return "{"
                + "\"client_type\":" + DatabaseConnection.jsonStringOrNull(f.clientType) + ","
                + "\"nationality\":" + DatabaseConnection.jsonStringOrNull(f.nationality) + ","
                + "\"jurisdiction_risk\":" + DatabaseConnection.jsonStringOrNull(f.jurisdictionRisk) + ","
                + "\"age\":" + f.age + ","
                + "\"annual_income_band\":" + DatabaseConnection.jsonStringOrNull(f.annualIncomeBand) + ","
                + "\"main_source_of_funds\":" + DatabaseConnection.jsonStringOrNull(f.mainSourceOfFunds) + ","
                + "\"is_pep\":" + (f.isPep ? 1 : 0) + ","
                + "\"adverse_media_hits\":" + f.adverseMediaHits + ","
                + "\"is_cross_border\":" + (f.isCrossBorder ? 1 : 0) + ","
                + "\"product_type\":" + DatabaseConnection.jsonStringOrNull(f.productType) + ","
                + "\"total_docs_submitted\":" + f.totalDocsSubmitted + ","
                + "\"verified_docs_count\":" + f.verifiedDocsCount + ","
                + "\"expired_docs_count\":" + f.expiredDocsCount + ","
                + "\"has_unverified_docs\":" + f.hasUnverifiedDocs
                + "}";
    }

    /**
     * Extracts a flat string field value from a JSON object.
     *
     * @param json source JSON
     * @param key  field key
     * @return value, or null when missing
     */
    private String extractString(String json, String key) {
        String searchKey = "\"" + key + "\"";
        int keyIndex = json.indexOf(searchKey);
        if (keyIndex == -1) {
            return null;
        }
        int colonIndex = json.indexOf(':', keyIndex + searchKey.length());
        if (colonIndex == -1) {
            return null;
        }
        int firstQuote = json.indexOf('"', colonIndex);
        if (firstQuote == -1) {
            return null;
        }
        int secondQuote = json.indexOf('"', firstQuote + 1);
        if (secondQuote == -1) {
            return null;
        }
        return json.substring(firstQuote + 1, secondQuote);
    }

    /**
     * Extracts a flat numeric field value from a JSON object.
     *
     * @param json source JSON
     * @param key  field key
     * @return value, or null when missing/unparsable
     */
    private Double extractDouble(String json, String key) {
        String searchKey = "\"" + key + "\"";
        int keyIndex = json.indexOf(searchKey);
        if (keyIndex == -1) {
            return null;
        }
        int colonIndex = json.indexOf(':', keyIndex + searchKey.length());
        if (colonIndex == -1) {
            return null;
        }
        int start = colonIndex + 1;
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) {
            start++;
        }
        int end = start;
        while (end < json.length()
                && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '.' || json.charAt(end) == '-')) {
            end++;
        }
        try {
            return Double.parseDouble(json.substring(start, end));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Extracts a JSON array field verbatim (for embedding as-is elsewhere), or
     * null when the field is absent/JSON null.
     *
     * @param json source JSON
     * @param key  field key
     * @return the array substring including brackets, or null
     */
    private String extractArrayOrNull(String json, String key) {
        String searchKey = "\"" + key + "\"";
        int keyIndex = json.indexOf(searchKey);
        if (keyIndex == -1) {
            return null;
        }
        int colonIndex = json.indexOf(':', keyIndex + searchKey.length());
        if (colonIndex == -1) {
            return null;
        }
        int start = colonIndex + 1;
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) {
            start++;
        }
        if (start >= json.length() || json.charAt(start) != '[') {
            return null;
        }
        int depth = 0;
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '[') {
                depth++;
            } else if (c == ']') {
                depth--;
                if (depth == 0) {
                    return json.substring(start, i + 1);
                }
            }
        }
        return null;
    }
}
