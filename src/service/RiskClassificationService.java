package service;

import java.util.Collection;
import java.util.Set;

/**
 * Determines the KYC risk level (LOW / MEDIUM / HIGH) for an onboarding case
 * based on client type, nationality, and submitted document types.
 */
public class RiskClassificationService {

    public static final String LOW = "LOW";
    public static final String MEDIUM = "MEDIUM";
    public static final String HIGH = "HIGH";

    /** ISO country codes treated as high risk jurisdictions (sanctions / FATF grey-black list). */
    private static final Set<String> HIGH_RISK_NATIONALITIES = Set.of("AF", "IR", "KP", "SY", "YE", "SO");

    /** Document types that indicate an enhanced due-diligence scenario when submitted. */
    private static final Set<String> HIGH_RISK_DOCUMENT_TYPES =
            Set.of("PEP_BUSINESS_RATIONALE_STMT", "TRUST_DEED", "LETTER_OF_WISHES");

    /**
     * Classifies the risk level of a case.
     *
     * @param clientType client type: INDIVIDUAL / CORPORATE / TRUST / POLITICAL
     * @param nationality ISO country code of the client's nationality
     * @param submittedDocumentTypes document type names submitted for the case
     * @return one of {@link #LOW}, {@link #MEDIUM}, {@link #HIGH}
     */
    public String classify(String clientType, String nationality, Collection<String> submittedDocumentTypes) {
        if (clientType == null) {
            throw new IllegalArgumentException("clientType must not be null");
        }
        String type = clientType.toUpperCase();

        // Political exposure is always treated as high risk.
        if ("POLITICAL".equals(type)) {
            return HIGH;
        }

        boolean highRiskNationality = nationality != null && HIGH_RISK_NATIONALITIES.contains(nationality.toUpperCase());
        boolean hasHighRiskDocument = submittedDocumentTypes != null
                && submittedDocumentTypes.stream().anyMatch(HIGH_RISK_DOCUMENT_TYPES::contains);

        if (highRiskNationality || hasHighRiskDocument) {
            return HIGH;
        }

        if ("CORPORATE".equals(type) || "TRUST".equals(type)) {
            return MEDIUM;
        }

        if ("INDIVIDUAL".equals(type)) {
            return LOW;
        }

        throw new IllegalArgumentException("Unsupported client type: " + clientType);
    }
}
