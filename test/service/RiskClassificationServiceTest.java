package service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class RiskClassificationServiceTest {

    private final RiskClassificationService riskClassificationService = new RiskClassificationService();

    @Test
    void individualWithLowRiskNationalityAndNoDocumentsIsLowRisk() {
        String risk = riskClassificationService.classify("INDIVIDUAL", "GB", List.of("PASSPORT", "UTILITY_BILL"));
        assertEquals(RiskClassificationService.LOW, risk);
    }

    @Test
    void corporateWithLowRiskNationalityIsMediumRisk() {
        String risk = riskClassificationService.classify("CORPORATE", "GB", List.of("CERTIFICATE_OF_INCORPORATION"));
        assertEquals(RiskClassificationService.MEDIUM, risk);
    }

    @Test
    void trustWithLowRiskNationalityIsMediumRisk() {
        String risk = riskClassificationService.classify("TRUST", "US", List.of());
        assertEquals(RiskClassificationService.MEDIUM, risk);
    }

    @Test
    void politicalClientIsAlwaysHighRisk() {
        String risk = riskClassificationService.classify("POLITICAL", "GB", List.of());
        assertEquals(RiskClassificationService.HIGH, risk);
    }

    @ParameterizedTest
    @CsvSource({"AF", "IR", "KP", "SY", "YE", "SO"})
    void individualWithHighRiskNationalityIsHighRisk(String nationality) {
        String risk = riskClassificationService.classify("INDIVIDUAL", nationality, List.of("PASSPORT"));
        assertEquals(RiskClassificationService.HIGH, risk);
    }

    @Test
    void corporateWithHighRiskDocumentIsHighRisk() {
        String risk = riskClassificationService.classify(
                "CORPORATE", "GB", List.of("CERTIFICATE_OF_INCORPORATION", "TRUST_DEED"));
        assertEquals(RiskClassificationService.HIGH, risk);
    }

    @Test
    void individualWithHighRiskDocumentIsHighRisk() {
        String risk = riskClassificationService.classify(
                "INDIVIDUAL", "GB", Set.of("PASSPORT", "PEP_BUSINESS_RATIONALE_STMT"));
        assertEquals(RiskClassificationService.HIGH, risk);
    }

    @Test
    void nullClientTypeThrows() {
        assertThrows(IllegalArgumentException.class, () -> riskClassificationService.classify(null, "GB", List.of()));
    }

    @Test
    void unsupportedClientTypeThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> riskClassificationService.classify("UNKNOWN", "GB", List.of()));
    }
}
