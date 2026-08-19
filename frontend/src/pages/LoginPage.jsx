import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth, ROLES } from '../auth/AuthContext';

const HOME_PATH_BY_ROLE = {
  [ROLES.CLIENT]: '/client',
  [ROLES.COMPLIANCE_OFFICER]: '/officer',
  [ROLES.ADMIN_COMPLIANCE_OFFICER]: '/admin',
};

export default function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();

  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      const user = await login(username, password);
      navigate(HOME_PATH_BY_ROLE[user.role]);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-page">
      <form className="login-card" onSubmit={handleSubmit}>
        <div className="login-logo">
          <span className="logo-triangle" aria-hidden="true"></span>
          <span>KYC Onboarding</span>
        </div>
        <h1>KYC Client Onboarding</h1>
        <p className="login-subtitle">Sign in to continue</p>

        <label>
          Username
          <input
            type="text"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            required
          />
        </label>

        <label>
          Password
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
          />
        </label>

        {error && <p className="error">{error}</p>}

        <button type="submit" disabled={loading}>
          {loading ? 'Signing in...' : 'Log in'}
        </button>
      </form>
    </div>
  );
}
