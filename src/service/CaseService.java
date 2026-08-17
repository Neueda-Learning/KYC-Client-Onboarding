package service;

import repository.CaseRepository;
import java.sql.SQLException;
import java.util.List;

public class CaseService {
    private final CaseRepository caseRepository = new CaseRepository();

    public int createOnboardingCase() throws SQLException {
        return caseRepository.createOnboardingCase(11, "STANDARD_ACCOUNT", "PENDING");
    }

    public int uploadDocument(int caseId) throws SQLException {
        return caseRepository.uploadDocument(caseId);
    }

    public boolean verifyDocument(int caseId, int docId) throws SQLException {
        return caseRepository.verifyDocument(caseId, docId);
    }

    public boolean updateCaseStatus(int caseId, String newStatus) throws SQLException {
        return caseRepository.updateCaseStatus(caseId, newStatus);
    }

    public String listCases(String statusFilter) throws SQLException {
        List<String> cases = caseRepository.listCases(statusFilter);
        return "[\n" + String.join(",\n", cases) + "\n]";
    }

    public String getCaseById(int id) throws SQLException {
        return caseRepository.getCaseById(id);
    }
}