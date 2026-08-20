import { useEffect, useState } from 'react';
import { useAuth } from '../auth/AuthContext';
import { api } from '../api/api';
import { getMissingDocuments } from '../api/mockData';
import { formatDateTime } from '../utils/formatDateTime';
import SubmitDocumentModal from '../components/SubmitDocumentModal';

export default function ClientHomePage() {
  const { user } = useAuth();
  const [caseData, setCaseData] = useState(null);
  const [clientType, setClientType] = useState(null);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(true);
  const [submitDocType, setSubmitDocType] = useState(null);

  const loadCaseData = async () => {
    try {
      setLoading(true);
      const [cases, client] = await Promise.all([
        api.getCases(),
        api.getClient(user.entityId),
      ]);
      const myCase = cases.find((c) => c.client_id === user.entityId);
      if (!myCase) {
        setCaseData(null);
        setClientType(client.client_type);
        return;
      }
      const fullCase = await api.getCase(myCase.case_id);
      setCaseData(fullCase);
      setClientType(client.client_type);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadCaseData();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [user.entityId]);

  if (loading) return <div className="page">Loading your case...</div>;
  if (error) return <div className="page error">Error: {error}</div>;

  const welcomeName = user.fullName || user.username;

  if (!caseData) {
    return (
      <div className="page">
        <h1>Welcome, {welcomeName}</h1>
        <p>You don't have an onboarding case yet.</p>
        <ContactSection />
      </div>
    );
  }

  const submittedDocTypes = caseData.documents.map((d) => d.doc_type);
  const missingDocs = getMissingDocuments(clientType, submittedDocTypes);

  return (
    <div className="page">
      <h1>Welcome, {welcomeName}</h1>

      <section className="card">
        <h2>Your Case</h2>
        <dl className="detail-grid">
          <dt>Product</dt>
          <dd>{caseData.product_type}</dd>
          <dt>Status</dt>
          <dd>
            <span className={`status-badge status-${caseData.case_status.toLowerCase()}`}>
              {caseData.case_status}
            </span>
          </dd>
          <dt>Opened</dt>
          <dd>{formatDateTime(caseData.opened_date)}</dd>
          <dt>Due date</dt>
          <dd>{caseData.due_date || '—'}</dd>
          {caseData.rejection_reason && (
            <>
              <dt>Rejection reason</dt>
              <dd>{caseData.rejection_reason}</dd>
            </>
          )}
        </dl>
      </section>

      <section className="card">
        <h2>Documents you've uploaded</h2>
        {caseData.documents.length === 0 ? (
          <p>No documents uploaded yet.</p>
        ) : (
          <table className="cases-table">
            <thead>
              <tr>
                <th>Type</th>
                <th>Submitted</th>
                <th>Verified</th>
                <th>Expiry</th>
              </tr>
            </thead>
            <tbody>
              {caseData.documents.map((doc) => (
                <tr key={doc.doc_id}>
                  <td>{doc.doc_type}</td>
                  <td>{formatDateTime(doc.submission_date)}</td>
                  <td>{doc.verified ? 'Yes' : 'Pending'}</td>
                  <td>{doc.expiry_date || '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>

      <section className="card">
        <h2>Documents still needed</h2>
        {missingDocs.length === 0 ? (
          <p>All required documents have been submitted.</p>
        ) : (
          <ul className="missing-docs-list">
            {missingDocs.map((docType) => (
              <li className="missing-doc-item" key={docType}>
                <span>{docType}</span>
                <button
                  type="button"
                  className="button-secondary"
                  onClick={() => setSubmitDocType(docType)}
                >
                  Submit document
                </button>
              </li>
            ))}
          </ul>
        )}
      </section>

      <ContactSection />

      {submitDocType && (
        <SubmitDocumentModal
          caseId={caseData.case_id}
          docType={submitDocType}
          onClose={() => setSubmitDocType(null)}
          onSubmitted={loadCaseData}
        />
      )}
    </div>
  );
}

function ContactSection() {
  return (
    <section className="card">
      <h2>Contact</h2>
      <dl className="detail-grid">
        <dt>Onboarding hotline</dt>
        <dd>+1 (800) 555-0199</dd>
        <dt>Email</dt>
        <dd>onboarding-support@kycbank.example</dd>
        <dt>Hours</dt>
        <dd>Mon–Fri, 8:00–18:00</dd>
      </dl>
    </section>
  );
}