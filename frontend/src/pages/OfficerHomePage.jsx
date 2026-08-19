import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { api } from '../api/api';

export default function OfficerHomePage() {
  const { user } = useAuth();
  const [cases, setCases] = useState([]);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;

    api
      .getCases(undefined, user.entityId)
      .then((all) => {
        if (!cancelled) {
          setCases(all);
        }
      })
      .catch((err) => !cancelled && setError(err.message))
      .finally(() => !cancelled && setLoading(false));

    return () => {
      cancelled = true;
    };
  }, [user.entityId]);

  if (loading) return <div className="page">Loading your cases...</div>;
  if (error) return <div className="page error">Error: {error}</div>;

  return (
    <div className="page">
      <h1>My Cases</h1>

      {cases.length === 0 ? (
        <p>No cases are currently assigned to you.</p>
      ) : (
        <table className="cases-table">
          <thead>
            <tr>
              <th>Case ID</th>
              <th>Client</th>
              <th>Product</th>
              <th>Status</th>
              <th>Opened</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {cases.map((c) => (
              <tr key={c.case_id}>
                <td>{c.case_id}</td>
                <td className="client-name">{c.client_name}</td>
                <td>{c.product_type}</td>
                <td>
                  <span className={`status-badge status-${c.case_status.toLowerCase()}`}>
                    {c.case_status}
                  </span>
                </td>
                <td>{c.opened_date}</td>
                <td>
                  <Link to={`/cases/${c.case_id}`} className="view-case-button">View</Link>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}
