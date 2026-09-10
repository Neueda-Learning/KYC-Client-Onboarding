package service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DocumentChecklistServiceTest {

    private final DocumentChecklistService documentChecklistService = new DocumentChecklistService();

    @Test
    void individualChecklistRequiresPersonalDocuments() {
        List<String> required = documentChecklistService.getRequiredDocuments("INDIVIDUAL");
        assertTrue(required.contains("PASSPORT"));
        assertTrue(required.contains("UTILITY_BILL"));
        assertFalse(required.contains("CERTIFICATE_OF_INCORPORATION"));
    }

    @Test
    void corporateChecklistRequiresCorporateDocuments() {
        List<String> required = documentChecklistService.getRequiredDocuments("CORPORATE");
        assertTrue(required.contains("CERTIFICATE_OF_INCORPORATION"));
        assertTrue(required.contains("UBO_DECLARATION_FORM"));
        assertFalse(required.contains("PASSPORT"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"individual", "Individual", "INDIVIDUAL"})
    void clientTypeLookupIsCaseInsensitive(String clientType) {
        assertTrue(documentChecklistService.getRequiredDocuments(clientType).contains("PASSPORT"));
    }

    @Test
    void isRequiredDocumentReturnsTrueForMatchingType() {
        assertTrue(documentChecklistService.isRequiredDocument("INDIVIDUAL", "PASSPORT"));
        assertFalse(documentChecklistService.isRequiredDocument("INDIVIDUAL", "CERTIFICATE_OF_INCORPORATION"));
    }

    @Test
    void checklistCompleteWhenAllRequiredDocumentsSubmitted() {
        Set<String> submitted = Set.of("PASSPORT", "UTILITY_BILL", "KYC_APPLICATION_FORM", "TAX_SELF_CERT_FATCA_CRS");
        assertTrue(documentChecklistService.isChecklistComplete("INDIVIDUAL", submitted));
    }

    @Test
    void checklistIncompleteWhenDocumentsMissing() {
        Set<String> submitted = Set.of("PASSPORT");
        assertFalse(documentChecklistService.isChecklistComplete("INDIVIDUAL", submitted));
    }

    @Test
    void checklistIncompleteWhenSubmittedSetIsNull() {
        assertFalse(documentChecklistService.isChecklistComplete("INDIVIDUAL", null));
    }

    @Test
    void unsupportedClientTypeThrows() {
        assertThrows(IllegalArgumentException.class, () -> documentChecklistService.getRequiredDocuments("TRUST"));
    }

    @Test
    void nullClientTypeThrows() {
        assertThrows(IllegalArgumentException.class, () -> documentChecklistService.getRequiredDocuments(null));
    }

    @Test
    void checklistCompleteReturnsFalseInsteadOfThrowing() {
        assertThrows(IllegalArgumentException.class,
                () -> documentChecklistService.isChecklistComplete("UNKNOWN", Set.of()));
    }
}
