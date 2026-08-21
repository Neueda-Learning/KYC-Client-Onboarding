import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { api } from '../api/api';
import { useAuth } from '../auth/AuthContext';
import { formatDateTime } from '../utils/formatDateTime';
import { getMissingDocuments } from '../api/mockData';
import DocumentViewModal from '../components/DocumentViewModal';

const NEXT_STATUS_OPTIONS = [
  'OPEN',
  'AWAITING_DOCUMENTS',
  'IN_REVIEW',
  'APPROVED',
  'REJECTED',
];

const RISK_LEVELS = ['LOW', 'MEDIUM', 'HIGH'];

export default function CaseDetailPage() {
  const { caseId } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();

  const [caseData, setCaseData] = useState(null);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(true);
  const [statusUpdateError, setStatusUpdateError] = useState(null);

  const [riskLevel, setRiskLevel] = useState('LOW');
  const [rationale, setRationale] = useState('');
  const [riskSaving, setRiskSaving] = useState(false);
  const [riskError, setRiskError] = useState(null);

  const [closing, setClosing] = useState(false);
  const [closeError, setCloseError] = useState(null);

  const [viewingDoc, setViewingDoc] = useState(null);

  const loadCase = () => {
    setLoading(true);
    api
      .getCase(caseId)
      .then((data) => {
        setCaseData(data);
        if (data.risk_classification) {
          setRiskLevel(data.risk_classification.risk_level);
        }
      })
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  };

  useEffect(loadCase, [caseId]);

  if (loading) return <div className="page">Loading case...</div>;
  if (error) return <div className="page error">Error: {error}</div>;
  if (!caseData) return null;

  const submittedDocTypes = caseData.documents.map((d) => d.doc_type);
  const missingDocs = getMissingDocuments(caseData.client_type, submittedDocTypes);
  const isClosed = caseData.case_status === 'CLOSED' || caseData.case_status === 'REJECTED' || caseData.case_status === 'APPROVED';

  const allDocumentsVerified = caseData.documents.length > 0 && caseData.documents.every((d) => d.verified);
  const documentsComplete = missingDocs.length === 0 && allDocumentsVerified;
  const hasRiskClassification = !!caseData.risk_classification;
  const canCloseCase = documentsComplete;
  const canApproveOrReject = documentsComplete && hasRiskClassification;

  const handleStatusChange = async (e) => {
    const newStatus = e.target.value;
    if ((newStatus === 'APPROVED') && !canApproveOrReject) {
      setStatusUpdateError(
        `This case must have all required documents verified and a risk classification before it can be ${newStatus.toLowerCase()}.`
      );
      return;
    }
    setStatusUpdateError(null);
    try {
      await api.updateCaseStatus(caseId, newStatus);
      loadCase();
    } catch (err) {
      setStatusUpdateError(err.message);
    }
  };

  const handleRiskSubmit = async (e) => {
    e.preventDefault();
    setRiskSaving(true);
    setRiskError(null);
    try {
      await api.updateRiskClassification(caseId, riskLevel, rationale, user.entityId);
      setRationale('');
      loadCase();
    } catch (err) {
      setRiskError(err.message);
    } finally {
      setRiskSaving(false);
    }
  };

  const handleCloseCase = async () => {
    setClosing(true);
    setCloseError(null);
    try {
      await api.updateCaseStatus(caseId, 'CLOSED');
      loadCase();
    } catch (err) {
      setCloseError(err.message);
    } finally {
      setClosing(false);
    }
  };

  const handleVerify = async (docId) => {
    await api.verifyDocument(caseId, docId);
    loadCase();
  };

  return (
    <div className="page">
      <header className="page-header">
        <button className="link-button" onClick={() => navigate(-1)}>
          &larr; Back to case list
        </button>
        <h1>Case #{caseData.case_id}</h1>
      </header>
      
      <section className="card">
        <dl className="detail-grid">
          <dt>Client</dt>
          <dd>{caseData.client_name}</dd>
          <dt>Client type</dt>
          <dd>{caseData.client_type}</dd>
          <dt>Product</dt>
          <dd>{caseData.product_type}</dd>
          <dt>Case status</dt>
          <dd>
            <span className={`status-badge status-${caseData.case_status.toLowerCase()}`}>
              {caseData.case_status}
            </span>
          </dd>
          <dt>Assigned officer</dt>
          <dd>{caseData.officer_name || 'Unassigned'}</dd>
          <dt>Opened</dt>
          <dd>{formatDateTime(caseData.opened_date)}</dd>
          <dt>Due date</dt>
          <dd>{caseData.due_date || '—'}</dd>
          <dt>Completed</dt>
          <dd>{caseData.completed_date ? formatDateTime(caseData.completed_date) : '—'}</dd>
          {caseData.rejection_reason && (
            <>
              <dt>Rejection reason</dt>
              <dd>{caseData.rejection_reason}</dd>
            </>
          )}
        </dl>

        <label>
          Status
          <select value={caseData.case_status} onChange={handleStatusChange} disabled={isClosed}>
            {NEXT_STATUS_OPTIONS.map((s) => {
              const blocked = (s === 'APPROVED') && !canApproveOrReject;
              return (
                <option key={s} value={s} disabled={blocked}>
                  {s}
                  {blocked ? ' (requirements not met)' : ''}
                </option>
              );
            })}
          </select>
        </label>
        {!isClosed && !canApproveOrReject && (
          <p className="hint">
            Approving requires all required documents to be submitted and verified,
            and a risk classification on record.
          </p>
        )}
        {statusUpdateError && <p className="error">{statusUpdateError}</p>}
      </section>

      <section className="card">
        <h2>Client Details</h2>
        <dl className="detail-grid">
          <dt>Date of birth</dt>
          <dd>{caseData.date_of_birth}</dd>
          <dt>Country of birth</dt>
          <dd>{caseData.country_of_birth}</dd>
          <dt>Nationality</dt>
          <dd>{caseData.nationality}</dd>
          <dt>Tax residency</dt>
          <dd>{caseData.tax_residency}</dd>
          <dt>Occupation</dt>
          <dd>{caseData.occupation || '—'}</dd>
          <dt>Employer</dt>
          <dd>{caseData.employer || '—'}</dd>
          <dt>Main source of funds</dt>
          <dd>{caseData.main_source_of_funds || '—'}</dd>
          <dt>Annual income band</dt>
          <dd>{caseData.annual_income_band || '—'}</dd>
        </dl>

        <h2>Addresses</h2>
        {caseData.addresses.length === 0 ? (
          <p>No addresses on file.</p>
        ) : (
          <table className="cases-table">
            <thead>
              <tr>
                <th>Type</th>
                <th>Line 1</th>
                <th>Line 2</th>
                <th>City</th>
                <th>State</th>
                <th>Postcode</th>
                <th>Country</th>
                <th>Current</th>
              </tr>
            </thead>
            <tbody>
              {caseData.addresses.map((addr, idx) => (
                <tr key={idx}>
                  <td>{addr.address_type || '—'}</td>
                  <td>{addr.line1}</td>
                  <td>{addr.line2 || '—'}</td>
                  <td>{addr.city}</td>
                  <td>{addr.state || '—'}</td>
                  <td>{addr.postcode || '—'}</td>
                  <td>{addr.country}</td>
                  <td>{addr.is_current}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>

      <section className="card">
        <h2>Risk Classification</h2>
        {caseData.risk_classification ? (
          <dl className="detail-grid">
            <dt>Current level</dt>
            <dd>
              <span className={`status-badge status-${caseData.risk_classification.risk_level.toLowerCase()}`}>
                {caseData.risk_classification.risk_level}
              </span>
            </dd>
            <dt>Rationale</dt>
            <dd>{caseData.risk_classification.rationale || '—'}</dd>
            <dt>Assessed by</dt>
            <dd>{caseData.risk_classification.assessor_name || '—'}</dd>
            <dt>Classified on</dt>
            <dd>{formatDateTime(caseData.risk_classification.classification_date)}</dd>
            <dt>Next review</dt>
            <dd>{caseData.risk_classification.next_review_date || '—'}</dd>
          </dl>
        ) : (
          <p>No risk classification recorded yet.</p>
        )}

        {!isClosed && (
          <form onSubmit={handleRiskSubmit}>
            <label>
              New risk level
              <select value={riskLevel} onChange={(e) => setRiskLevel(e.target.value)}>
                {RISK_LEVELS.map((level) => (
                  <option key={level} value={level}>
                    {level}
                  </option>
                ))}
              </select>
            </label>
            <label>
              Reason
              <textarea
                rows={3}
                value={rationale}
                onChange={(e) => setRationale(e.target.value)}
                required
              />
            </label>
            {riskError && <p className="error">{riskError}</p>}
            <button type="submit" disabled={riskSaving}>
              {riskSaving ? 'Saving...' : 'Update risk classification'}
            </button>
          </form>
        )}

        {/* <div className="modal-actions" style={{ justifyContent: 'flex-start', marginTop: 16 }}>
          {isClosed ? (
            <span className="case-closed-text">Case closed</span>
          ) : (
            <button
              type="button"
              className="button-danger"
              onClick={handleCloseCase}
              disabled={closing || !canCloseCase}
              title={!canCloseCase ? 'All required documents must be submitted and verified before closing' : undefined}
            >
              {closing ? 'Closing...' : 'Close case'}
            </button>
          )}
        </div>
        {!isClosed && !canCloseCase && (
          <p className="hint">
            All required documents must be submitted and verified before this case can be closed.
          </p>
        )}
        {closeError && <p className="error">{closeError}</p>} */}
      </section>

      <section className="card">
        <h2>Documents still needed</h2>
        {missingDocs.length === 0 ? (
          <p>All required documents have been submitted.</p>
        ) : (
          <ul>
            {missingDocs.map((docType) => (
              <li key={docType}>{docType}</li>
            ))}
          </ul>
        )}
      </section>

      <section className="card">
        <h2>Document Verification</h2>
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
                <th></th>
              </tr>
            </thead>
            <tbody>
              {caseData.documents.map((doc) => (
                <tr key={doc.doc_id}>
                  <td>{doc.doc_type}</td>
                  <td>{formatDateTime(doc.submission_date)}</td>
                  <td>{doc.verified ? 'Yes' : 'Pending'}</td>
                  <td>{doc.expiry_date || '—'}</td>
                  <td>{doc.rejection_reason || '—'}</td>
                  <td>
                    <button type="button" className="button-secondary" onClick={() => setViewingDoc(doc)}>
                      View
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>

      {viewingDoc && (
        <DocumentViewModal
          doc={viewingDoc}
          onVerify={handleVerify}
          onClose={() => setViewingDoc(null)}
        />
      )}
    </div>
  );
}