import { useEffect, useState } from 'react';

export default function AssignOfficerModal({ caseItem, officers, onClose, onAssigned }) {
  const [officerId, setOfficerId] = useState(caseItem.assigned_officer_id ?? '');
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    const onKeyDown = (e) => e.key === 'Escape' && onClose();
    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
  }, [onClose]);

  const handleSave = async () => {
    setSaving(true);
    setError(null);
    try {
      const value = officerId === '' ? null : Number(officerId);
      const result = await onAssigned(caseItem.case_id, value);
      if (result !== false) {
        onClose();
      }
    } catch (err) {
      setError(err.message);
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <h2>Assign Officer</h2>
        <p className="hint">Case #{caseItem.case_id} — {caseItem.client_name}</p>

        <label>
          Officer
          <select value={officerId} onChange={(e) => setOfficerId(e.target.value)}>
            <option value="">Unassigned</option>
            {officers.map((o) => (
              <option key={o.officer_id} value={o.officer_id}>
                {o.full_name}
              </option>
            ))}
          </select>
        </label>

        {error && <p className="error">{error}</p>}

        <div className="modal-actions">
          <button type="button" className="button-secondary" onClick={onClose} disabled={saving}>
            Cancel
          </button>
          <button type="button" onClick={handleSave} disabled={saving}>
            {saving ? 'Saving...' : 'Save'}
          </button>
        </div>
      </div>
    </div>
  );
}
