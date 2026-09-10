// Skeleton dashboard - model plots are live, case metrics below are computed
// client-side from GET /api/onboarding/cases (case_status, client_status,
// ml_prediction).
import { useEffect, useMemo, useState } from 'react';
import { api, mlApi } from '../api/api';
import { cachedFetch, invalidateCache } from '../api/cache';

const DASHBOARD_CASES_CACHE_KEY = 'dashboard:cases';
const DASHBOARD_CASES_CACHE_TTL_MS = 60 * 1000;

const CASE_STATUS_COLORS = { OPEN: '#3b82f6', PENDING: '#f59e0b', CLOSED: '#6b7280' };
const CLIENT_STATUS_COLORS = { PENDING: '#f59e0b', ACTIVE: '#22c55e', SUSPENDED: '#a855f7', REJECTED: '#ef4444' };

// Model diagnostic plots reproduced from analysis.ipynb, served live by the
// FastAPI ML service (data_analysis/app.py). Descriptions are written for
// compliance officers, not data scientists. Each card fetches its own image
// independently so one failing/offline plot doesn't block the rest.
const MODEL_PLOTS = [
  {
    title: 'What drives approval decisions',
    description:
      'The client and case details that most influence whether an application is approved or rejected.',
    fetchPlot: mlApi.getFeatureImportancePlot,
  },
  {
    title: 'How often the AI gets it right',
    description:
      "A check of the AI's decisions against the true outcome for a sample of past cases - the diagonal shows correct decisions.",
    fetchPlot: mlApi.getConfusionMatrixPlot,
  },
  {
    title: 'How reliable is the model',
    description:
      'How well the AI tells good applications apart from risky ones. The closer the orange curve bends toward the top-left corner, the more reliable it is.',
    fetchPlot: mlApi.getRocCurvePlot,
  },
  {
    title: 'Overall approval rate',
    description: 'Share of past cases that were approved versus rejected.',
    fetchPlot: mlApi.getApprovalRatePlot,
  },
  {
    title: 'AI confidence levels',
    description:
      'How confident the AI is across all cases. Decisions near 0% or 100% are high-confidence; cases near the middle are worth a second look by an officer.',
    fetchPlot: mlApi.getProbabilityDistributionPlot,
  },
  {
    title: 'Most common reasons for rejection',
    description:
      'The details most frequently flagged as needing improvement for rejected applications, based on the AI\'s recommendations.',
    fetchPlot: mlApi.getTopRejectionFactorsPlot,
  },
];

function ModelPlotCard({ title, description, fetchPlot }) {
  const [image, setImage] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    let cancelled = false;
    fetchPlot()
      .then((dataUrl) => {
        if (!cancelled) setImage(dataUrl);
      })
      .catch((err) => {
        if (!cancelled) setError(err.message || 'Failed to load plot');
      });
    return () => {
      cancelled = true;
    };
  }, [fetchPlot]);

  return (
    <section className="card dashboard-card dashboard-card-wide">
      <h2>{title}</h2>
      <p className="hint">{description}</p>
      {error && <div className="chart-placeholder">ML service unavailable</div>}
      {!error && !image && <div className="chart-placeholder">Loading…</div>}
      {image && <img src={image} alt={title} className="dashboard-plot" />}
    </section>
  );
}

// Lightweight dependency-free horizontal bar chart for the case metrics
// below, which come from live case data rather than a pre-rendered image.
function SimpleBarChart({ data }) {
  const max = Math.max(...data.map((d) => d.value), 1);
  return (
    <div className="simple-bar-chart">
      {data.map((d) => (
        <div className="simple-bar-row" key={d.label}>
          <span className="simple-bar-label">{d.label}</span>
          <div className="simple-bar-track">
            <div
              className="simple-bar-fill"
              style={{ width: `${(d.value / max) * 100}%`, backgroundColor: d.color }}
            />
          </div>
          <span className="simple-bar-value">{d.value}</span>
        </div>
      ))}
    </div>
  );
}

function useCountsByField(cases, field, colorMap) {
  return useMemo(() => {
    if (!cases) return null;
    const counts = cases.reduce((acc, c) => {
      const key = c[field] || 'UNKNOWN';
      acc[key] = (acc[key] || 0) + 1;
      return acc;
    }, {});
    return Object.entries(counts).map(([label, value]) => ({
      label,
      value,
      color: colorMap[label] || '#94a3b8',
    }));
  }, [cases, field, colorMap]);
}

function CaseMetricCard({ title, description, data, error, emptyMessage }) {
  return (
    <section className="card dashboard-card dashboard-card-wide">
      <h2>{title}</h2>
      <p className="hint">{description}</p>
      {error && <div className="chart-placeholder">Unable to load case data</div>}
      {!error && !data && <div className="chart-placeholder">Loading…</div>}
      {!error && data && data.length === 0 && <div className="chart-placeholder">{emptyMessage}</div>}
      {!error && data && data.length > 0 && <SimpleBarChart data={data} />}
    </section>
  );
}

function CaseMetricsSection({ refreshToken }) {
  const [cases, setCases] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    let cancelled = false;
    cachedFetch(DASHBOARD_CASES_CACHE_KEY, () => api.getCases(), DASHBOARD_CASES_CACHE_TTL_MS)
      .then((data) => {
        if (!cancelled) setCases(Array.isArray(data) ? data : data?.cases || []);
      })
      .catch((err) => {
        if (!cancelled) setError(err.message || 'Failed to load cases');
      });
    return () => {
      cancelled = true;
    };
  }, [refreshToken]);

  const caseStatusData = useCountsByField(cases, 'case_status', CASE_STATUS_COLORS);
  const clientStatusData = useCountsByField(cases, 'client_status', CLIENT_STATUS_COLORS);

  const outcomeData = useMemo(() => {
    if (!cases) return null;
    let matches = 0;
    let mismatches = 0;
    cases.forEach((c) => {
      if (!c.ml_prediction || (c.client_status !== 'ACTIVE' && c.client_status !== 'REJECTED')) return;
      const actual = c.client_status === 'ACTIVE' ? 'APPROVED' : 'REJECTED';
      if (c.ml_prediction === actual) matches += 1;
      else mismatches += 1;
    });
    return [
      { label: 'AI matched final outcome', value: matches, color: '#22c55e' },
      { label: 'AI missed final outcome', value: mismatches, color: '#ef4444' },
    ].filter((d) => d.value > 0);
  }, [cases]);

  return (
    <>
      <h2>Case metrics</h2>
      <div className="dashboard-grid-stacked">
        <CaseMetricCard
          title="Case volume by status"
          description="How many cases are currently open, pending review, or closed."
          data={caseStatusData}
          error={error}
          emptyMessage="No cases yet"
        />
        <CaseMetricCard
          title="Client status breakdown"
          description="Where clients stand overall: pending review, active, suspended, or rejected."
          data={clientStatusData}
          error={error}
          emptyMessage="No clients yet"
        />
        <CaseMetricCard
          title="Predicted vs actual outcome"
          description="How often the AI's decision matched the client's final approved/rejected outcome, for cases that have been decided."
          data={outcomeData}
          error={error}
          emptyMessage="No finalised cases with a prediction yet"
        />
      </div>
    </>
  );
}

export default function AdminDashboardPage() {
  const [refreshToken, setRefreshToken] = useState(0);

  const handleRefresh = () => {
    invalidateCache(); // clears all cached plots/case data so the next fetch is fresh
    setRefreshToken((t) => t + 1);
  };

  return (
    <div className="page">
      <div className="page-header">
        <h1>Dashboard</h1>
        <button className="link-button" onClick={handleRefresh}>
          Refresh data
        </button>
      </div>

      <h2>Model performance & explainability</h2>
      <div className="dashboard-grid-stacked">
        {MODEL_PLOTS.map((plot) => (
          <ModelPlotCard key={`${plot.title}-${refreshToken}`} {...plot} />
        ))}
      </div>

      <CaseMetricsSection refreshToken={refreshToken} />
    </div>
  );
}
