import { createContext, useContext, useState, useMemo } from 'react';
import { api } from '../api/api';

// Authenticates against the real POST /api/auth/login endpoint (see
// controller.AuthHandler / service.AuthService on the backend) and stores the
// returned role/entity id for routing to the right pages.

const AuthContext = createContext(null);

const STORAGE_KEY = 'kyc.auth';

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    const stored = localStorage.getItem(STORAGE_KEY);
    return stored ? JSON.parse(stored) : null;
  });

  const login = async (username, password) => {
    const result = await api.login(username, password);
    const nextUser = {
      username: result.username,
      fullName: result.full_name,
      role: result.role,
      entityId: result.entity_id,
    };
    localStorage.setItem(STORAGE_KEY, JSON.stringify(nextUser));
    setUser(nextUser);
    return nextUser;
  };

  const logout = () => {
    localStorage.removeItem(STORAGE_KEY);
    setUser(null);
  };

  const value = useMemo(() => ({ user, login, logout }), [user]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return ctx;
}

export const ROLES = {
  CLIENT: 'CLIENT',
  COMPLIANCE_OFFICER: 'COMPLIANCE_OFFICER',
  ADMIN_COMPLIANCE_OFFICER: 'ADMIN_COMPLIANCE_OFFICER',
};
