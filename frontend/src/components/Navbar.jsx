import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';

const ROLE_LABELS = {
  CLIENT: 'Client',
  COMPLIANCE_OFFICER: 'Compliance Officer',
  ADMIN_COMPLIANCE_OFFICER: 'Admin Compliance Officer',
};

export default function Navbar() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  if (!user) return null;

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <nav className="navbar">
      <Link to="/" className="navbar-brand">
        <span className="logo-triangle" aria-hidden="true"></span>
        KYC Client Onboarding
      </Link>
      <div className="navbar-user">
        <span>
          {user.fullName || user.username} &middot; {ROLE_LABELS[user.role]}
        </span>
        <button onClick={handleLogout}>Log out</button>
      </div>
    </nav>
  );
}
