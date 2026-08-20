import { useEffect, useState } from 'react';

// Mock document preview — shows a placeholder "PDF" and lets the reviewer verify or cancel.
export default function DocumentViewModal({ doc, onVerify, onClose }) {
  const [verifying, setVerifying] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    const onKeyDown = (e) => e.key === 'Escape' && onClose();
    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
  }, [onClose]);

  const handleVerify = async () => {
    setVerifying(true);
    setError(null);
    try {
      await onVerify(doc.doc_id);
      onClose();
    } catch (err) {
      setError(err.message);
    } finally {
      setVerifying(false);
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal document-modal" onClick={(e) => e.stopPropagation()}>
        <h2>{doc.doc_type.replaceAll('_', ' ')}</h2>

        <div className="mock-pdf-preview">
          <div className="mock-pdf-page">
            <span className="mock-pdf-icon" aria-hidden="true">📄</span>
            <p>{doc.doc_type.replaceAll('_', ' ')}.pdf</p>
            <p className="hint">Submitted {doc.submission_date}</p>
          </div>
        </div>

        {error && <p className="error">{error}</p>}

        <div className="modal-actions">
          <button type="button" className="button-secondary" onClick={onClose}>
            Cancel
          </button>
          {!doc.verified && (
            <button type="button" onClick={handleVerify} disabled={verifying}>
              {verifying ? 'Verifying...' : 'Verify document'}
            </button>
          )}
        </div>
      </div>
    </div>
  );
}