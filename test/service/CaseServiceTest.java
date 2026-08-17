package service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import repository.CaseRepository;
import repository.InvalidStateTransitionException;

class CaseServiceTest {

    private CaseRepository caseRepository;
    private CaseService caseService;

    @BeforeEach
    void setUp() {
        caseRepository = mock(CaseRepository.class);
        caseService = new CaseService(caseRepository, new DocumentChecklistService());
    }

    // ---- Case state machine (mocked repository) ----

    @Test
    void openCaseTransitionsToAwaitingDocuments() throws SQLException {
        when(caseRepository.getCaseStatus(1)).thenReturn("OPEN");
        when(caseRepository.setCaseStatus(1, "AWAITING_DOCUMENTS")).thenReturn(true);

        boolean result = caseService.transitionCaseStatus(1, "AWAITING_DOCUMENTS");

        assertTrue(result);
        verify(caseRepository).setCaseStatus(1, "AWAITING_DOCUMENTS");
    }

    @Test
    void awaitingDocumentsTransitionsToInReview() throws SQLException {
        when(caseRepository.getCaseStatus(1)).thenReturn("AWAITING_DOCUMENTS");
        when(caseRepository.setCaseStatus(1, "IN_REVIEW")).thenReturn(true);

        boolean result = caseService.transitionCaseStatus(1, "IN_REVIEW");

        assertTrue(result);
        verify(caseRepository).setCaseStatus(1, "IN_REVIEW");
    }

    @Test
    void inReviewTransitionsToApprovedWhenDocumentsVerified() throws SQLException {
        when(caseRepository.getCaseStatus(1)).thenReturn("IN_REVIEW");
        when(caseRepository.hasUnverifiedDocuments(1)).thenReturn(false);
        when(caseRepository.setCaseStatus(1, "APPROVED")).thenReturn(true);

        boolean result = caseService.transitionCaseStatus(1, "APPROVED");

        assertTrue(result);
        verify(caseRepository).setCaseStatus(1, "APPROVED");
    }

    @Test
    void inReviewTransitionsToRejected() throws SQLException {
        when(caseRepository.getCaseStatus(1)).thenReturn("IN_REVIEW");
        when(caseRepository.setCaseStatus(1, "REJECTED")).thenReturn(true);

        boolean result = caseService.transitionCaseStatus(1, "REJECTED");

        assertTrue(result);
        verify(caseRepository).setCaseStatus(1, "REJECTED");
    }

    @Test
    void rejectedCaseCanReopenToAwaitingDocuments() throws SQLException {
        when(caseRepository.getCaseStatus(1)).thenReturn("REJECTED");
        when(caseRepository.setCaseStatus(1, "AWAITING_DOCUMENTS")).thenReturn(true);

        boolean result = caseService.transitionCaseStatus(1, "AWAITING_DOCUMENTS");

        assertTrue(result);
        verify(caseRepository).setCaseStatus(1, "AWAITING_DOCUMENTS");
    }

    @Test
    void transitionReturnsFalseWhenCaseDoesNotExist() throws SQLException {
        when(caseRepository.getCaseStatus(99)).thenReturn(null);

        boolean result = caseService.transitionCaseStatus(99, "AWAITING_DOCUMENTS");

        assertFalse(result);
        verify(caseRepository, never()).setCaseStatus(anyInt(), org.mockito.ArgumentMatchers.anyString());
    }

    @ParameterizedTest
    @CsvSource({
            "OPEN, IN_REVIEW",
            "APPROVED, IN_REVIEW",
            "APPROVED, AWAITING_DOCUMENTS",
            "REJECTED, IN_REVIEW",
            "REJECTED, APPROVED"
    })
    void invalidTransitionsAreRejected(String currentStatus, String requestedStatus) throws SQLException {
        when(caseRepository.getCaseStatus(1)).thenReturn(currentStatus);

        assertThrows(InvalidStateTransitionException.class,
                () -> caseService.transitionCaseStatus(1, requestedStatus));
        verify(caseRepository, never()).setCaseStatus(anyInt(), org.mockito.ArgumentMatchers.anyString());
    }

    // ---- Parameterised: premature approval is blocked (wrong state or unverified documents) ----

    @ParameterizedTest
    @CsvSource({
            "OPEN, false",
            "AWAITING_DOCUMENTS, false",
            "IN_REVIEW, true"
    })
    void prematureApprovalIsBlocked(String currentStatus, boolean hasUnverifiedDocuments) throws SQLException {
        when(caseRepository.getCaseStatus(1)).thenReturn(currentStatus);
        if ("IN_REVIEW".equals(currentStatus)) {
            when(caseRepository.hasUnverifiedDocuments(1)).thenReturn(hasUnverifiedDocuments);
        }

        assertThrows(InvalidStateTransitionException.class, () -> caseService.transitionCaseStatus(1, "APPROVED"));
        verify(caseRepository, never()).setCaseStatus(anyInt(), org.mockito.ArgumentMatchers.anyString());
    }

    // ---- Document submission against checklist (mocked repository) ----

    @Test
    void submittingCorrectDocumentTypeUpdatesChecklistAndRepository() throws SQLException {
        boolean accepted = caseService.submitDocumentForChecklist(1, "INDIVIDUAL", "PASSPORT", 5);

        assertTrue(accepted);
        verify(caseRepository, times(1)).uploadDocument(1, 5);
    }

    @ParameterizedTest
    @CsvSource({
            "INDIVIDUAL, CERTIFICATE_OF_INCORPORATION",
            "CORPORATE, PASSPORT"
    })
    void submittingWrongDocumentTypeIsRejectedAndNotPersisted(String clientType, String wrongDocType)
            throws SQLException {
        boolean accepted = caseService.submitDocumentForChecklist(1, clientType, wrongDocType, 5);

        assertFalse(accepted);
        verify(caseRepository, never()).uploadDocument(anyInt(), anyInt());
    }
}
