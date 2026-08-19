package service;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repository.CaseRepository;
import repository.InvalidStateTransitionException;

/**
 * Business logic for onboarding case operations.
 */
public class CaseService {
    private static final Logger logger = LoggerFactory.getLogger(CaseService.class);

    private final CaseRepository caseRepository;
    private final DocumentChecklistService documentChecklistService;

    /** Allowed case status transitions for the onboarding case state machine. */
    private static final Map<String, Set<String>> ALLOWED_TRANSITIONS = Map.of(
            "OPEN", Set.of("AWAITING_DOCUMENTS"),
            "AWAITING_DOCUMENTS", Set.of("IN_REVIEW"),
            "IN_REVIEW", Set.of("APPROVED", "REJECTED"),
            "REJECTED", Set.of("AWAITING_DOCUMENTS"),
            "APPROVED", Set.of()
    );

    public CaseService() {
        this(new CaseRepository(), new DocumentChecklistService());
    }

    public CaseService(CaseRepository caseRepository, DocumentChecklistService documentChecklistService) {
        this.caseRepository = caseRepository;
        this.documentChecklistService = documentChecklistService;
    }

    /**
     * Transitions a case to a new status, enforcing the onboarding case state machine:
     * OPEN -&gt; AWAITING_DOCUMENTS -&gt; IN_REVIEW -&gt; APPROVED/REJECTED, with REJECTED
     * cases allowed to reopen back to AWAITING_DOCUMENTS.
     *
     * @param caseId target case id
     * @param newStatus requested status
     * @return true when the case existed and was updated
     * @throws SQLException when persistence fails
     * @throws InvalidStateTransitionException when the requested transition is not allowed,
     *         or the case has unverified documents and the requested status is APPROVED
     */
    public boolean transitionCaseStatus(int caseId, String newStatus) throws SQLException {
        String currentStatus = caseRepository.getCaseStatus(caseId);
        if (currentStatus == null) {
            logger.warn("Case status transition failed: caseId={} reason=case not found", caseId);
            return false;
        }

        String from = currentStatus.toUpperCase();
        String to = newStatus.toUpperCase();
        Set<String> allowedNext = ALLOWED_TRANSITIONS.getOrDefault(from, Set.of());
        if (!allowedNext.contains(to)) {
            logger.warn("Case status transition rejected: caseId={} from={} to={} reason=disallowed transition",
                    caseId, currentStatus, newStatus);
            throw new InvalidStateTransitionException(
                    "Cannot transition case " + caseId + " from " + currentStatus + " to " + newStatus);
        }

        if ("APPROVED".equals(to) && caseRepository.hasUnverifiedDocuments(caseId)) {
            logger.warn("Case status transition rejected: caseId={} from={} to={} reason=unverified documents",
                    caseId, currentStatus, newStatus);
            throw new InvalidStateTransitionException(
                    "Case " + caseId + " has unverified documents and cannot be approved");
        }

        boolean updated = caseRepository.setCaseStatus(caseId, to);
        logger.info("Case status transitioned: caseId={} from={} to={}", caseId, currentStatus, to);
        return updated;
    }

    /**
     * Submits a document against a case's checklist, only accepting the document
     * when its type is required for the client's type.
     *
     * @param caseId target case id
     * @param clientType client type used to look up the required checklist
     * @param docTypeName submitted document type name
     * @param docTypeId submitted document type id, used for persistence
     * @return true when the document type matched the checklist and was recorded
     * @throws SQLException when persistence fails
     */
    public boolean submitDocumentForChecklist(int caseId, String clientType, String docTypeName, int docTypeId)
            throws SQLException {
        if (!documentChecklistService.isRequiredDocument(clientType, docTypeName)) {
            logger.warn("Document submission rejected: caseId={} clientType={} docType={} reason=not on checklist",
                    caseId, clientType, docTypeName);
            return false;
        }
        caseRepository.uploadDocument(caseId, docTypeId);
        logger.info("Checklist document accepted: caseId={} clientType={} docType={}",
                caseId, clientType, docTypeName);
        return true;
    }

    /**
     * Creates a new onboarding case with request-provided attributes.
     *
     * @param clientId related client id
     * @param productType requested product type
     * @param status initial case status
     * @return newly created case id
     * @throws SQLException when persistence fails
     */
    public int createOnboardingCase(int clientId, String productType, String status) throws SQLException {
        return caseRepository.createOnboardingCase(clientId, productType, status);
    }

    /**
     * Uploads a document entry for a case.
     *
     * @param caseId target case id
     * @param docTypeId document type id
     * @return newly created document id
     * @throws SQLException when persistence fails
     */
    public int uploadDocument(int caseId, int docTypeId) throws SQLException {
        return caseRepository.uploadDocument(caseId, docTypeId);
    }

    /**
     * Verifies a document belonging to a case.
     *
     * @param caseId owning case id
     * @param docId document id
     * @return true when the document was found and verified
     * @throws SQLException when persistence fails
     */
    public boolean verifyDocument(int caseId, int docId) throws SQLException {
        return caseRepository.verifyDocument(caseId, docId);
    }

    /**
     * Updates the status of an onboarding case.
     *
     * @param caseId target case id
     * @param newStatus requested status
     * @return true when the case existed and was updated
     * @throws SQLException when persistence fails
     * @throws repository.InvalidStateTransitionException when the requested transition is not allowed
     */
    public boolean updateCaseStatus(int caseId, String newStatus) throws SQLException {
        return caseRepository.updateCaseStatus(caseId, newStatus);
    }

    /**
     * Assigns (or unassigns, when officerId is null) the compliance officer handling a case.
     *
     * @param caseId target case id
     * @param officerId officer id to assign, or null to unassign
     * @return true when the case existed and was updated
     * @throws SQLException when persistence fails
     */
    public boolean assignOfficer(int caseId, Integer officerId) throws SQLException {
        return caseRepository.assignOfficer(caseId, officerId);
    }

    /**
     * Lists onboarding cases, optionally filtered by status and/or assigned officer.
     *
     * @param statusFilter status to filter by, or null/empty for all cases
     * @param officerFilter assigned officer id to filter by, or null for all officers
     * @return JSON array of case summaries
     * @throws SQLException when the query fails
     */
    public String listCases(String statusFilter, Integer officerFilter) throws SQLException {
        List<String> cases = caseRepository.listCases(statusFilter, officerFilter);
        return "[\n" + String.join(",\n", cases) + "\n]";
    }

    /**
     * Fetches full case details, including submitted documents.
     *
     * @param id case id
     * @return case JSON representation, or null when not found
     * @throws SQLException when the query fails
     */
    public String getCaseById(int id) throws SQLException {
        return caseRepository.getCaseById(id);
    }
}