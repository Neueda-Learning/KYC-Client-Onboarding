package service;

import java.sql.SQLException;
import java.util.List;
import repository.CaseRepository;

/**
 * Business logic for onboarding case operations.
 */
public class CaseService {
    private final CaseRepository caseRepository = new CaseRepository();

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
     * Lists onboarding cases, optionally filtered by status.
     *
     * @param statusFilter status to filter by, or null/empty for all cases
     * @return JSON array of case summaries
     * @throws SQLException when the query fails
     */
    public String listCases(String statusFilter) throws SQLException {
        List<String> cases = caseRepository.listCases(statusFilter);
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