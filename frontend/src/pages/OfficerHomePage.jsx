import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { api } from '../api/api';
import CaseFilters, { matchesDueDateFilter, sortCasesByDueDate } from '../components/CaseFilters';

const PENDING_STATUSES = ['AWAITING_DOCUMENTS', 'IN_REVIEW'];
const CLOSED_STATUSES = ['APPROVED', 'REJECTED', 'CLOSED'];
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

function dateOnly(value) {
  return value ? value.split(/[ T]/)[0] : '—';
}

export default function OfficerHomePage() {
  const { user } = useAuth();
  const [cases, setCases] = useState([]);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(true);
  const [selectedStatuses, setSelectedStatuses] = useState([]);
  const [dueDateFilter, setDueDateFilter] = useState('all');
  const [dueDateSort, setDueDateSort] = useState('none');

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

  const toggleStatus = (status) => {
    setSelectedStatuses((prev) =>
      prev.includes(status) ? prev.filter((s) => s !== status) : [...prev, status]
    );
  };

  const visibleCases = sortCasesByDueDate(
    cases.filter(
      (c) =>
        (selectedStatuses.length === 0 || selectedStatuses.includes(c.case_status)) &&
        matchesDueDateFilter(c.due_date, dueDateFilter)
    ),
    dueDateSort
  );

  return (
    <div className="page">
      <h1>My Cases</h1>

      <CaseFilters
        selectedStatuses={selectedStatuses}
        onToggleStatus={toggleStatus}
        dueDateFilter={dueDateFilter}
        onDueDateFilterChange={setDueDateFilter}
        dueDateSort={dueDateSort}
        onDueDateSortChange={setDueDateSort}
      />

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
              <th>Due Date</th>
              <th>Case Opened</th>
              <th>Due Date</th>
              <th>Case Opened</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {visibleCases.length === 0 && (
              <tr>
                <td colSpan={7}>No cases match the selected filters.</td>
              </tr>
            )}
            {visibleCases.map((c) => (
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
                <td>{dateOnly(c.opened_date)}</td>
                <td className={dueDateClass(c.due_date, c.case_status)}>{c.due_date || '—'}</td>
                <td>{dateOnly(c.opened_date)}</td>
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