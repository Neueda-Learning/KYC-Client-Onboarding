import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api/api';
import { MOCK_OFFICERS } from '../api/mockData';

export default function AdminHomePage() {
  const [cases, setCases] = useState([]);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(true);
  const [assignments, setAssignments] = useState({});

  useEffect(() => {
    let cancelled = false;

    api
      .getCases()
      .then((all) => !cancelled && setCases(all))
      .catch((err) => !cancelled && setError(err.message))
      .finally(() => !cancelled && setLoading(false));

    return () => {
      cancelled = true;
    };
  }, []);

  const handleAssign = (caseId, officerId) => {
    // No backend endpoint exists yet to persist officer assignment, so this
    // only updates local UI state for the skeleton.
    setAssignments((prev) => ({ ...prev, [caseId]: officerId }));
  };

  if (loading) return <div className="page">Loading all cases...</div>;
  if (error) return <div className="page error">Error: {error}</div>;

  return (
    <div className="page">
      <h1>All Cases</h1>
      <p className="hint">
        Assigning an officer here only updates the page — the API has no endpoint yet to persist
        `assigned_officer_id`.
      </p>

      <table>
        <thead>
          <tr>
            <th>Case ID</th>
            <th>Client</th>
            <th>Product</th>
            <th>Status</th>
            <th>Assigned Officer</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          {cases.map((c) => {
            const assignedOfficerId = assignments[c.case_id] ?? c.assigned_officer_id ?? '';
            return (
              <tr key={c.case_id}>
                <td>{c.case_id}</td>
                <td>{c.client_name}</td>
                <td>{c.product_type}</td>
                <td>
                  <span className={`status-badge status-${c.case_status.toLowerCase()}`}>
                    {c.case_status}
                  </span>
                </td>
                <td>
                  <select
                    value={assignedOfficerId}
                    onChange={(e) => handleAssign(c.case_id, Number(e.target.value))}
                  >
                    <option value="">Unassigned</option>
                    {MOCK_OFFICERS.map((o) => (
                      <option key={o.officer_id} value={o.officer_id}>
                        {o.full_name}
                      </option>
                    ))}
                  </select>
                </td>
                <td>
                  <Link to={`/cases/${c.case_id}`}>View</Link>
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}
