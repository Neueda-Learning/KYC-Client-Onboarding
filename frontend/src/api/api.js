// Thin wrapper around the KYC relay server REST API.
import { cachedFetch } from './cache';

// Base URL can be overridden with VITE_API_BASE_URL at build time.
const BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';
// FastAPI ML/XAI service (data_analysis/app.py), called directly from the
// browser for the model diagnostic plots - not proxied by the Java backend.
const ML_BASE_URL = import.meta.env.VITE_ML_SERVICE_URL || 'http://localhost:8000';

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

async function mlRequest(path) {
  const res = await fetch(`${ML_BASE_URL}${path}`);
  const data = await res.json().catch(() => null);
  if (!res.ok) {
    throw new Error(data?.detail || `Request failed: ${res.status}`);
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
  getCases: (status, assignedOfficerId, limit, offset) => {
    const params = new URLSearchParams();
    if (status) {
      // status may be a single string or an array of statuses (OR'd together)
      const statusValue = Array.isArray(status) ? status.join(',') : status;
      if (statusValue) params.set('status', statusValue);
    }
    if (assignedOfficerId != null) params.set('assigned_officer_id', assignedOfficerId);
    if (limit != null) params.set('limit', limit);
    if (offset != null) params.set('offset', offset);
    const qs = params.toString();
    const url = `/api/onboarding/cases${qs ? `?${qs}` : ''}`;
    console.log('Fetching:', url);
    return request(url).then((response) => {
      console.log('Raw response:', response);
      // If pagination parameters were provided, return the full paginated response
      if (limit != null && offset != null) {
        // Should be in format: {total, offset, limit, cases: [...]}
        if (response && response.cases) {
          return response;
        }
        // If response is an array (old format), wrap it
        if (Array.isArray(response)) {
          return {
            total: response.length,
            offset: offset,
            limit: limit,
            cases: response
          };
        }
        // If we get here, response is unexpected
        console.warn('Unexpected pagination response format:', response);
        return {
          total: 0,
          offset: offset,
          limit: limit,
          cases: []
        };
      }
      // For backward compatibility: if no pagination params, return just the cases array
      // (or the cases array from the paginated response if it's in the new format)
      if (response && response.cases && Array.isArray(response.cases)) {
        return response.cases;
      }
      return response;
    });
  },
  getCase: (id) => request(`/api/onboarding/cases/${id}`),
  updateCaseStatus: (id, case_status) =>
    request(`/api/onboarding/cases/${id}/status`, {
      method: 'PATCH',
      body: JSON.stringify({ case_status }),
    }),
  updateClientStatus: (clientId, status) =>
    request(`/api/clients/${clientId}/status`, {
      method: 'PATCH',
      body: JSON.stringify({ status }),
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
  openCase: (payload) =>
    request('/api/onboarding/cases/open', {
      method: 'POST',
      body: JSON.stringify(payload),
    }),
};

// Model diagnostic plots (base64 PNG data URLs) served by the FastAPI ML
// service - used by AdminDashboardPage's "Model Performance" section. These
// only change when the model is retrained, so cache them for a while instead
// of re-generating/re-fetching all 6 plots every time the dashboard mounts.
const PLOT_CACHE_TTL_MS = 10 * 60 * 1000;

export const mlApi = {
  getFeatureImportancePlot: () =>
    cachedFetch('ml:feature-importance', () =>
      mlRequest('/api/v1/explain/feature-importance-plot').then((d) => d.feature_importance_plot), PLOT_CACHE_TTL_MS),
  getConfusionMatrixPlot: () =>
    cachedFetch('ml:confusion-matrix', () =>
      mlRequest('/api/v1/eval/confusion-matrix-plot').then((d) => d.confusion_matrix_plot), PLOT_CACHE_TTL_MS),
  getRocCurvePlot: () =>
    cachedFetch('ml:roc-curve', () =>
      mlRequest('/api/v1/eval/roc-curve-plot').then((d) => d.roc_curve_plot), PLOT_CACHE_TTL_MS),
  getApprovalRatePlot: () =>
    cachedFetch('ml:approval-rate', () =>
      mlRequest('/api/v1/eval/approval-rate-plot').then((d) => d.approval_rate_plot), PLOT_CACHE_TTL_MS),
  getProbabilityDistributionPlot: () =>
    cachedFetch('ml:probability-distribution', () =>
      mlRequest('/api/v1/eval/probability-distribution-plot').then((d) => d.probability_distribution_plot), PLOT_CACHE_TTL_MS),
  getTopRejectionFactorsPlot: () =>
    cachedFetch('ml:top-rejection-factors', () =>
      mlRequest('/api/v1/eval/top-rejection-factors-plot').then((d) => d.top_rejection_factors_plot), PLOT_CACHE_TTL_MS),
};
