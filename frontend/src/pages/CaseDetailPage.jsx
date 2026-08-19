import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { api } from '../api/api';

const NEXT_STATUS_OPTIONS = [
  'OPEN',
  'AWAITING_DOCUMENTS',
  'IN_REVIEW',
  'APPROVED',
  'REJECTED',
];

export default function CaseDetailPage() {
  const { caseId } = useParams();
  const navigate = useNavigate();
  const [caseData, setCaseData] = useState(null);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(true);
  const [statusUpdateError, setStatusUpdateError] = useState(null);

  const loadCase = () => {
    setLoading(true);
    api
      .getCase(caseId)
      .then(setCaseData)
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  };

  useEffect(loadCase, [caseId]);

  const handleStatusChange = async (e) => {
    const newStatus = e.target.value;
    setStatusUpdateError(null);
    try {
      await api.updateCaseStatus(caseId, newStatus);
      loadCase();
    } catch (err) {
      setStatusUpdateError(err.message);
    }
  };

  if (loading) return <div className="page">Loading case...</div>;
  if (error) return <div className="page error">Error: {error}</div>;
  if (!caseData) return null;

  return (
    <div className="page">
      <button className="link-button" onClick={() => navigate(-1)}>
        &larr; Back to case list
      </button>
      <h1>Case #{caseData.case_id}</h1>

      <section className="card">
        <dl className="detail-grid">
          <dt>Client</dt>
          <dd>{caseData.client_name}</dd>
          <dt>Client type</dt>
          <dd>{caseData.client_type}</dd>
          <dt>Product</dt>
          <dd>{caseData.product_type}</dd>
          <dt>Opened</dt>
          <dd>{caseData.opened_date}</dd>
          <dt>Due date</dt>
          <dd>{caseData.due_date || '—'}</dd>
          <dt>Completed</dt>
          <dd>{caseData.completed_date || '—'}</dd>
          {caseData.rejection_reason && (
            <>
              <dt>Rejection reason</dt>
              <dd>{caseData.rejection_reason}</dd>
            </>
          )}
        </dl>

        <label>
          Status
          <select value={caseData.case_status} onChange={handleStatusChange}>
            {NEXT_STATUS_OPTIONS.map((s) => (
              <option key={s} value={s}>
                {s}
              </option>
            ))}
          </select>
        </label>
        {statusUpdateError && <p className="error">{statusUpdateError}</p>}
      </section>

      <section className="card">
        <h2>Documents</h2>
        {caseData.documents.length === 0 ? (
          <p>No documents submitted yet.</p>
        ) : (
          <table className="cases-table">
            <thead>
              <tr>
                <th>Type</th>
                <th>Submitted</th>
                <th>Verified</th>
                <th>Expiry</th>
                <th>Rejection reason</th>
              </tr>
            </thead>
            <tbody>
              {caseData.documents.map((doc) => (
                <tr key={doc.doc_id}>
                  <td>{doc.doc_type}</td>
                  <td>{doc.submission_date}</td>
                  <td>{doc.verified ? 'Yes' : 'Pending'}</td>
                  <td>{doc.expiry_date || '—'}</td>
                  <td>{doc.rejection_reason || '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>
    </div>
  );
}
