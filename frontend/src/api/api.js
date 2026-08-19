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
  getCases: (status) => request(`/api/onboarding/cases${status ? `?status=${status}` : ''}`),
  getCase: (id) => request(`/api/onboarding/cases/${id}`),
  updateCaseStatus: (id, case_status) =>
    request(`/api/onboarding/cases/${id}/status`, {
      method: 'PATCH',
      body: JSON.stringify({ case_status }),
    }),
  verifyDocument: (caseId, docId) =>
    request(`/api/onboarding/cases/${caseId}/documents/${docId}/verify`, { method: 'PATCH' }),
};
