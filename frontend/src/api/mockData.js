// The backend does not yet expose compliance officers or an assign-officer
// endpoint, so this data/behaviour is mocked on the frontend for the skeleton UI.

export const MOCK_OFFICERS = [
  { officer_id: 1, full_name: 'John Smith' },
  { officer_id: 2, full_name: 'Anna Novak' },
  { officer_id: 3, full_name: 'Robert Taylor' },
];

// Required document types per client type, mirrors service.DocumentChecklistService.
export const REQUIRED_DOCS_BY_CLIENT_TYPE = {
  INDIVIDUAL: ['PASSPORT', 'UTILITY_BILL', 'KYC_APPLICATION_FORM', 'TAX_SELF_CERT_FATCA_CRS'],
  CORPORATE: [
    'CERTIFICATE_OF_INCORPORATION',
    'MEMORANDUM_ARTICLES_ASSOCIATION',
    'UBO_DECLARATION_FORM',
    'BOARD_RESOLUTION_ACCOUNT_OPENING',
    'AUTHORIZED_SIGNATORY_LIST',
    'KYC_APPLICATION_FORM',
  ],
};

export function getMissingDocuments(clientType, submittedDocTypes) {
  const required = REQUIRED_DOCS_BY_CLIENT_TYPE[clientType] || [];
  return required.filter((docType) => !submittedDocTypes.includes(docType));
}
