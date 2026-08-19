import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api/api';
import AssignOfficerModal from '../components/AssignOfficerModal';

const PENDING_STATUSES = ['AWAITING_DOCUMENTS', 'IN_REVIEW'];
const CLOSED_STATUSES = ['APPROVED', 'REJECTED'];
const DUE_SOON_DAYS = 30;

function statusRowClass(status) {
  if (status === 'OPEN') return 'row-open';
  if (PENDING_STATUSES.includes(status)) return 'row-pending';
  if (CLOSED_STATUSES.includes(status)) return 'row-closed';
  return '';
}

function dueDateClass(dueDate, status) {
  if (!dueDate || CLOSED_STATUSES.includes(status)) return '';
  const daysLeft = Math.ceil((new Date(dueDate) - new Date()) / (1000 * 60 * 60 * 24));
  return daysLeft <= DUE_SOON_DAYS ? 'due-date-soon' : '';
}

export default function AdminHomePage() {
  const [cases, setCases] = useState([]);
  const [officers, setOfficers] = useState([]);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(true);
  const [assignError, setAssignError] = useState(null);
  const [activeCase, setActiveCase] = useState(null);

  useEffect(() => {
    let cancelled = false;

    Promise.all([api.getCases(), api.getOfficers()])
      .then(([allCases, allOfficers]) => {
        if (cancelled) return;
        setCases(allCases);
        setOfficers(allOfficers);
      })
      .catch((err) => !cancelled && setError(err.message))
      .finally(() => !cancelled && setLoading(false));

    return () => {
      cancelled = true;
    };
  }, []);

  const handleAssignOfficer = async (caseId, officerId) => {
    setAssignError(null);
    try {
      const result = await api.assignOfficer(caseId, officerId);
      setCases((prev) =>
        prev.map((c) =>
          c.case_id === caseId
            ? { ...c, assigned_officer_id: result.assigned_officer_id, officer_name: result.officer_name }
            : c
        )
      );
    } catch (err) {
      setAssignError(err.message);
      return false;
    }
  };

  if (loading) return <div className="page">Loading all cases...</div>;
  if (error) return <div className="page error">Error: {error}</div>;

  return (
    <div className="page">
      <h1>All Cases</h1>
      {assignError && <p className="error">{assignError}</p>}

      <table className="cases-table">
        <thead>
          <tr>
            <th>Case ID</th>
            <th>Client</th>
            <th>Product</th>
            <th>Status</th>
            <th>Due Date</th>
            <th>Assigned Officer</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          {cases.map((c) => (
            <tr key={c.case_id} className={statusRowClass(c.case_status)}>
              <td>{c.case_id}</td>
              <td className="client-name">{c.client_name}</td>
              <td>{c.product_type}</td>
              <td>
                <span className={`status-badge status-${c.case_status.toLowerCase()}`}>
                  {c.case_status}
                </span>
              </td>
              <td className={dueDateClass(c.due_date, c.case_status)}>{c.due_date || '—'}</td>
              <td>
                <button type="button" className="button-secondary" onClick={() => setActiveCase(c)}>
                  {c.officer_name || 'Unassigned'}
                </button>
              </td>
              <td>
                <Link to={`/cases/${c.case_id}`} className="view-case-button">View Case</Link>
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      {activeCase && (
        <AssignOfficerModal
          caseItem={activeCase}
          officers={officers}
          onClose={() => setActiveCase(null)}
          onAssigned={handleAssignOfficer}
        />
      )}
    </div>
  );
}
