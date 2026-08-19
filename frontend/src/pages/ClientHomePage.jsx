import { useEffect, useState } from 'react';
import { useAuth } from '../auth/AuthContext';
import { api } from '../api/api';
import { getMissingDocuments } from '../api/mockData';

export default function ClientHomePage() {
  const { user } = useAuth();
  const [caseData, setCaseData] = useState(null);
  const [clientType, setClientType] = useState(null);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;

    async function load() {
      try {
        setLoading(true);
        const [cases, client] = await Promise.all([
          api.getCases(),
          api.getClient(user.entityId),
        ]);
        const myCase = cases.find((c) => c.client_id === user.entityId);
        if (!myCase) {
          if (!cancelled) {
            setCaseData(null);
            setClientType(client.client_type);
          }
          return;
        }
        const fullCase = await api.getCase(myCase.case_id);
        if (!cancelled) {
          setCaseData(fullCase);
          setClientType(client.client_type);
        }
      } catch (err) {
        if (!cancelled) setError(err.message);
      } finally {
        if (!cancelled) setLoading(false);
      }
    }

    load();
    return () => {
      cancelled = true;
    };
  }, [user.entityId]);

  if (loading) return <div className="page">Loading your case...</div>;
  if (error) return <div className="page error">Error: {error}</div>;

  if (!caseData) {
    return (
      <div className="page">
        <h1>Welcome, {user.username}</h1>
        <p>You don't have an onboarding case yet.</p>
      </div>
    );
  }

  const submittedDocTypes = caseData.documents.map((d) => d.doc_type);
  const missingDocs = getMissingDocuments(clientType, submittedDocTypes);

  return (
    <div className="page">
      <h1>Welcome, {user.username}</h1>

      <section className="card">
        <h2>Your Case</h2>
        <dl className="detail-grid">
          <dt>Case ID</dt>
          <dd>{caseData.case_id}</dd>
          <dt>Product</dt>
          <dd>{caseData.product_type}</dd>
          <dt>Status</dt>
          <dd>
            <span className={`status-badge status-${caseData.case_status.toLowerCase()}`}>
              {caseData.case_status}
            </span>
          </dd>
          <dt>Opened</dt>
          <dd>{caseData.opened_date}</dd>
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
          <table>
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
                  <td>{doc.submission_date}</td>
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
          <ul>
            {missingDocs.map((docType) => (
              <li key={docType}>{docType}</li>
            ))}
          </ul>
        )}
      </section>
    </div>
  );
}
