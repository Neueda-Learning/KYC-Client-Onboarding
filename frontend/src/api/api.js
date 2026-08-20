// Thin wrapper around the KYC relay server REST API.
// Base URL can be overridden with VITE_API_BASE_URL at build time.
const BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

async function request(path, options = {}) {
  const res = await fetch(`${BASE_URL}${path}`, {
    headers: { 'Content-Type': 'application/json' },
    ...options,
  });
  const data = await res.json().catch(() => null);
  if (!res.ok) {
    throw new Error(data?.error || `Request failed: ${res.status}`);
  }
  return data;
}

export const api = {
  login: (username, password) =>
    request('/api/auth/login', {
      method: 'POST',
      body: JSON.stringify({ username, password }),
    }),
  getClients: () => request('/api/clients'),
  getClient: (id) => request(`/api/clients/${id}`),
  getCases: (status, assignedOfficerId) => {
    const params = new URLSearchParams();
    if (status) params.set('status', status);
    if (assignedOfficerId != null) params.set('assigned_officer_id', assignedOfficerId);
    const qs = params.toString();
    return request(`/api/onboarding/cases${qs ? `?${qs}` : ''}`);
  },
  getCase: (id) => request(`/api/onboarding/cases/${id}`),
  updateCaseStatus: (id, case_status) =>
    request(`/api/onboarding/cases/${id}/status`, {
      method: 'PATCH',
      body: JSON.stringify({ case_status }),
    }),
  verifyDocument: (caseId, docId) =>
    request(`/api/onboarding/cases/${caseId}/documents/${docId}/verify`, { method: 'PATCH' }),
  getOfficers: () => request('/api/officers'),
  assignOfficer: (caseId, officerId) =>
    request(`/api/onboarding/cases/${caseId}/officer`, {
      method: 'PATCH',
      body: JSON.stringify({ officer_id: officerId }),
    }),
  updateRiskClassification: (caseId, riskLevel, rationale, officerId) =>
    request(`/api/onboarding/cases/${caseId}/risk-classification`, {
      method: 'PATCH',
      body: JSON.stringify({ risk_level: riskLevel, rationale, officer_id: officerId }),
    }),
      getDocumentTypes: () => request('/api/document-types'),
  submitDocument: (caseId, docTypeId) =>
    request(`/api/onboarding/cases/${caseId}/documents`, {
      method: 'POST',
      body: JSON.stringify({ doc_type_id: docTypeId }),
    }),
};
