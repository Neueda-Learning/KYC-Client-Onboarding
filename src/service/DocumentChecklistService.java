package service;

import java.util.List;
import java.util.Set;

/**
 * Determines which document types are required for a checklist based on client type.
 */
public class DocumentChecklistService {

    private static final List<String> INDIVIDUAL_REQUIRED_DOCS = List.of(
            "PASSPORT",
            "UTILITY_BILL",
            "KYC_APPLICATION_FORM",
            "TAX_SELF_CERT_FATCA_CRS"
    );

    private static final List<String> CORPORATE_REQUIRED_DOCS = List.of(
            "CERTIFICATE_OF_INCORPORATION",
            "MEMORANDUM_ARTICLES_ASSOCIATION",
            "UBO_DECLARATION_FORM",
            "BOARD_RESOLUTION_ACCOUNT_OPENING",
            "AUTHORIZED_SIGNATORY_LIST",
            "KYC_APPLICATION_FORM"
    );

    /**
     * Returns the required document type names for the given client type.
     *
     * @param clientType client type: INDIVIDUAL or CORPORATE
     * @return immutable list of required document type names
     * @throws IllegalArgumentException when the client type has no known checklist
     */
    public List<String> getRequiredDocuments(String clientType) {
        if (clientType == null) {
            throw new IllegalArgumentException("clientType must not be null");
        }
        switch (clientType.toUpperCase()) {
            case "INDIVIDUAL":
                return INDIVIDUAL_REQUIRED_DOCS;
            case "CORPORATE":
                return CORPORATE_REQUIRED_DOCS;
            default:
                throw new IllegalArgumentException("Unsupported client type: " + clientType);
        }
    }

    /**
     * Checks whether a document type is part of the required checklist for a client type.
     *
     * @param clientType client type
     * @param documentType document type name to check
     * @return true when the document type is required for the client type
     */
    public boolean isRequiredDocument(String clientType, String documentType) {
        return getRequiredDocuments(clientType).contains(documentType);
    }

    /**
     * Checks whether all required documents have been submitted for a client type.
     *
     * @param clientType client type
     * @param submittedDocumentTypes document type names already submitted
     * @return true when every required document type has been submitted
     */
    public boolean isChecklistComplete(String clientType, Set<String> submittedDocumentTypes) {
        if (submittedDocumentTypes == null) {
            return false;
        }
        return submittedDocumentTypes.containsAll(getRequiredDocuments(clientType));
    }
}
