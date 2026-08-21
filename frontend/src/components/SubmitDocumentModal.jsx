import { useEffect, useState } from 'react';
import { api } from '../api/api';

export default function SubmitDocumentModal({ caseId, docType, onClose, onSubmitted }) {
  const [file, setFile] = useState(null);
  const [notes, setNotes] = useState('');
  const [docTypeId, setDocTypeId] = useState(null);
  const [loadingTypes, setLoadingTypes] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    const onKeyDown = (e) => e.key === 'Escape' && onClose();
    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
  }, [onClose]);

  useEffect(() => {
    let cancelled = false;
    api
      .getDocumentTypes()
      .then((types) => {
        if (cancelled) return;
        const match = types.find((t) => t.doc_type_name === docType);
        setDocTypeId(match ? match.doc_type_id : null);
      })
      .catch((err) => !cancelled && setError(err.message))
      .finally(() => !cancelled && setLoadingTypes(false));
    return () => {
      cancelled = true;
    };
  }, [docType]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!docTypeId) {
      setError('Could not resolve this document type. Please try again.');
      return;
    }
    setSubmitting(true);
    setError(null);
    try {
      await api.submitDocument(caseId, docTypeId);
      onSubmitted();
      onClose();
    } catch (err) {
      setError(err.message);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <h2>Submit Document</h2>
        <p className="hint">{docType}</p>

        <form onSubmit={handleSubmit}>
          <label>
            File
            <input type="file" onChange={(e) => setFile(e.target.files?.[0] ?? null)} />
          </label>

          <label>
            Notes (optional)
            <textarea
              rows={3}
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
            />
          </label>

          {error && <p className="error">{error}</p>}

          <div className="modal-actions">
            <button type="button" className="button-secondary" onClick={onClose} disabled={submitting}>
              Cancel
            </button>
            <button type="submit" disabled={submitting || loadingTypes}>
              {submitting ? 'Submitting...' : 'Submit'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}