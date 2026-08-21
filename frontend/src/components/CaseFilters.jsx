export const CASE_STATUSES = [
  { value: 'OPEN', label: 'Open', className: 'status-open' },
  { value: 'AWAITING_DOCUMENTS', label: 'Awaiting Documents', className: 'status-awaiting_documents' },
  { value: 'IN_REVIEW', label: 'In Review', className: 'status-in_review' },
  { value: 'APPROVED', label: 'Approved', className: 'status-approved' },
  { value: 'REJECTED', label: 'Rejected', className: 'status-rejected' },
];

export const DUE_DATE_FILTERS = [
  { value: 'all', label: 'All' },
  { value: 'overdue', label: 'Overdue' },
  { value: 'soon', label: 'Due in 30 days' },
  { value: 'none', label: 'No due date' },
];

// Checks whether a case's due date satisfies one of the quick due-date filter options.
export function matchesDueDateFilter(dueDate, filter) {
  if (filter === 'all') return true;
  if (filter === 'none') return !dueDate;
  if (!dueDate) return false;
  const daysLeft = Math.ceil((new Date(dueDate) - new Date()) / (1000 * 60 * 60 * 24));
  if (filter === 'overdue') return daysLeft < 0;
  if (filter === 'soon') return daysLeft >= 0 && daysLeft <= 30;
  return true;
}

// Sorts cases by due date; cases have no due date are always pushed to the end.
export function sortCasesByDueDate(cases, direction) {
  if (direction !== 'asc' && direction !== 'desc') return cases;
  const sign = direction === 'asc' ? 1 : -1;
  return [...cases].sort((a, b) => {
    if (!a.due_date && !b.due_date) return 0;
    if (!a.due_date) return 1;
    if (!b.due_date) return -1;
    return sign * (new Date(a.due_date) - new Date(b.due_date));
  });
}

export default function CaseFilters({
  selectedStatuses,
  onToggleStatus,
  dueDateFilter,
  onDueDateFilterChange,
  dueDateSort,
  onDueDateSortChange,
}) {
  return (
    <div className="case-filters">
      <div className="filter-group">
        <span className="filter-label">Status</span>
        <div className="filter-buttons">
          {CASE_STATUSES.map((s) => (
            <button
              key={s.value}
              type="button"
              className={`status-filter-button ${s.className} ${
                selectedStatuses.includes(s.value) ? 'active' : ''
              }`}
              onClick={() => onToggleStatus(s.value)}
            >
              {s.label}
            </button>
          ))}
        </div>
      </div>

      <div className="filter-group">
        <span className="filter-label">Due date</span>
        <div className="filter-buttons">
          {DUE_DATE_FILTERS.map((d) => (
            <button
              key={d.value}
              type="button"
              className={`due-date-filter-button ${dueDateFilter === d.value ? 'active' : ''}`}
              onClick={() => onDueDateFilterChange(d.value)}
            >
              {d.label}
            </button>
          ))}
        </div>
      </div>

      <div className="filter-group">
        <span className="filter-label">Sort by due date</span>
        <div className="filter-buttons">
          <button
            type="button"
            className={`due-date-filter-button ${dueDateSort === 'asc' ? 'active' : ''}`}
            onClick={() => onDueDateSortChange(dueDateSort === 'asc' ? 'none' : 'asc')}
          >
            Soonest first ↑
          </button>
          <button
            type="button"
            className={`due-date-filter-button ${dueDateSort === 'desc' ? 'active' : ''}`}
            onClick={() => onDueDateSortChange(dueDateSort === 'desc' ? 'none' : 'desc')}
          >
            Latest first ↓
          </button>
        </div>
      </div>
    </div>
  );
}

