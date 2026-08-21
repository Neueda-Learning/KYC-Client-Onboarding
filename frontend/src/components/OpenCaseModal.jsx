import { useEffect, useMemo, useState } from 'react';
import { api } from '../api/api';
import { REQUIRED_DOCS_BY_CLIENT_TYPE } from '../api/mockData';

const CLIENT_TYPES = ['INDIVIDUAL', 'CORPORATE', 'TRUST', 'POLITICAL'];
const ADDRESS_TYPES = ['REGISTERED', 'MAILING'];

const initialForm = {
  full_name: '',
  client_type: 'INDIVIDUAL',
  nationality: '',
  date_of_birth: '',
  country_of_birth: '',
  tax_residency: '',
  occupation: '',
  employer: '',
  main_source_of_funds: '',
  annual_income_band: '',
  address_type: 'REGISTERED',
  line1: '',
  line2: '',
  city: '',
  state: '',
  postcode: '',
  country: '',
  product_type: '',
  due_date: '',
  officer_id: '',
};

export default function OpenCaseModal({ officers, onClose, onOpened }) {
  const [form, setForm] = useState(initialForm);
  const [documentTypes, setDocumentTypes] = useState([]);
  const [checkedDocTypes, setCheckedDocTypes] = useState({});
  const [saving, setSaving] = useState(false);
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
      .then((types) => !cancelled && setDocumentTypes(types))
      .catch((err) => !cancelled && setError(err.message));
    return () => {
      cancelled = true;
    };
  }, []);

  const checklist = useMemo(() => {
    const requiredNames = REQUIRED_DOCS_BY_CLIENT_TYPE[form.client_type] || [];
    return documentTypes.filter((t) => requiredNames.includes(t.doc_type_name));
  }, [documentTypes, form.client_type]);

  useEffect(() => {
    setCheckedDocTypes((prev) => {
      const next = {};
      checklist.forEach((t) => {
        next[t.doc_type_id] = t.doc_type_id in prev ? prev[t.doc_type_id] : true;
      });
      return next;
    });
  }, [checklist]);

  const handleChange = (field) => (e) => {
    setForm((prev) => ({ ...prev, [field]: e.target.value }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);

    const required = [
      'full_name',
      'client_type',
      'nationality',
      'date_of_birth',
      'country_of_birth',
      'tax_residency',
      'line1',
      'city',
      'country',
      'product_type',
    ];
    const missing = required.filter((field) => !form[field].trim());
    if (missing.length > 0) {
      setError(`Missing required fields: ${missing.join(', ')}`);
      return;
    }

    setSaving(true);
    try {
      const payload = {
        client: {
          full_name: form.full_name,
          client_type: form.client_type,
          nationality: form.nationality,
          date_of_birth: form.date_of_birth,
          country_of_birth: form.country_of_birth,
          tax_residency: form.tax_residency,
          occupation: form.occupation || undefined,
          employer: form.employer || undefined,
          main_source_of_funds: form.main_source_of_funds || undefined,
          annual_income_band: form.annual_income_band || undefined,
        },
        address: {
          address_type: form.address_type,
          line1: form.line1,
          line2: form.line2 || undefined,
          city: form.city,
          state: form.state || undefined,
          postcode: form.postcode || undefined,
          country: form.country,
        },
        product_type: form.product_type,
        due_date: form.due_date || undefined,
        officer_id: form.officer_id === '' ? undefined : Number(form.officer_id),
        document_type_ids: Object.entries(checkedDocTypes)
          .filter(([, checked]) => checked)
          .map(([docTypeId]) => Number(docTypeId)),
      };

      const result = await api.openCase(payload);
      onOpened(result);
      onClose();
    } catch (err) {
      setError(err.message);
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal modal-wide" onClick={(e) => e.stopPropagation()}>
        <h2>Open a New Case</h2>

        <form onSubmit={handleSubmit}>
          <fieldset className="form-section">
            <legend>Client Details</legend>
            <div className="form-grid">
              <label>
                Full Name *
                <input type="text" value={form.full_name} onChange={handleChange('full_name')} required />
              </label>
              <label>
                Client Type *
                <select value={form.client_type} onChange={handleChange('client_type')}>
                  {CLIENT_TYPES.map((t) => (
                    <option key={t} value={t}>{t}</option>
                  ))}
                </select>
              </label>
              <label>
                Nationality *
                <input type="text" maxLength={2} value={form.nationality} onChange={handleChange('nationality')} required />
              </label>
              <label>
                Date of Birth *
                <input type="date" value={form.date_of_birth} onChange={handleChange('date_of_birth')} required />
              </label>
              <label>
                Country of Birth *
                <input type="text" maxLength={2} value={form.country_of_birth} onChange={handleChange('country_of_birth')} required />
              </label>
              <label>
                Tax Residency *
                <input type="text" maxLength={2} value={form.tax_residency} onChange={handleChange('tax_residency')} required />
              </label>
              <label>
                Occupation
                <input type="text" value={form.occupation} onChange={handleChange('occupation')} />
              </label>
              <label>
                Employer
                <input type="text" value={form.employer} onChange={handleChange('employer')} />
              </label>
              <label>
                Main Source of Funds
                <input type="text" value={form.main_source_of_funds} onChange={handleChange('main_source_of_funds')} />
              </label>
              <label>
                Annual Income Band
                <input type="text" value={form.annual_income_band} onChange={handleChange('annual_income_band')} placeholder="e.g. 50-100K" />
              </label>
            </div>
          </fieldset>

          <fieldset className="form-section">
            <legend>Address</legend>
            <div className="form-grid">
              <label>
                Address Type
                <select value={form.address_type} onChange={handleChange('address_type')}>
                  {ADDRESS_TYPES.map((t) => (
                    <option key={t} value={t}>{t}</option>
                  ))}
                </select>
              </label>
              <label>
                Line 1 *
                <input type="text" value={form.line1} onChange={handleChange('line1')} required />
              </label>
              <label>
                Line 2
                <input type="text" value={form.line2} onChange={handleChange('line2')} />
              </label>
              <label>
                City *
                <input type="text" value={form.city} onChange={handleChange('city')} required />
              </label>
              <label>
                State
                <input type="text" value={form.state} onChange={handleChange('state')} />
              </label>
              <label>
                Postcode
                <input type="text" value={form.postcode} onChange={handleChange('postcode')} />
              </label>
              <label>
                Country *
                <input type="text" value={form.country} onChange={handleChange('country')} required />
              </label>
            </div>
          </fieldset>

          <fieldset className="form-section">
            <legend>Case Details</legend>
            <div className="form-grid">
              <label>
                Product Type *
                <input type="text" value={form.product_type} onChange={handleChange('product_type')} placeholder="e.g. CURRENT_ACCOUNT" required />
              </label>
              <label>
                Due Date
                <input type="date" value={form.due_date} onChange={handleChange('due_date')} />
              </label>
              <label>
                Assign Risk Officer
                <select value={form.officer_id} onChange={handleChange('officer_id')}>
                  <option value="">Unassigned</option>
                  {officers.map((o) => (
                    <option key={o.officer_id} value={o.officer_id}>{o.full_name}</option>
                  ))}
                </select>
              </label>
            </div>
          </fieldset>

          <fieldset className="form-section">
            <legend>Documents Already Provided</legend>
            {checklist.length === 0 ? (
              <p className="hint">No known checklist for this client type.</p>
            ) : (
              <div className="checklist">
                {checklist.map((t) => (
                  <label key={t.doc_type_id} className="checklist-item">
                    <input
                      type="checkbox"
                      checked={!!checkedDocTypes[t.doc_type_id]}
                      onChange={(e) =>
                        setCheckedDocTypes((prev) => ({ ...prev, [t.doc_type_id]: e.target.checked }))
                      }
                    />
                    {t.doc_type_name}
                  </label>
                ))}
              </div>
            )}
          </fieldset>

          {error && <p className="error">{error}</p>}

          <div className="modal-actions">
            <button type="button" className="button-secondary" onClick={onClose} disabled={saving}>
              Cancel
            </button>
            <button type="submit" disabled={saving}>
              {saving ? 'Creating...' : 'Create Case'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
