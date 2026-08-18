import { Routes, Route, Navigate } from 'react-router-dom';
import { useAuth, ROLES } from './auth/AuthContext';
import ProtectedRoute from './components/ProtectedRoute';
import Navbar from './components/Navbar';
import LoginPage from './pages/LoginPage';
import ClientHomePage from './pages/ClientHomePage';
import OfficerHomePage from './pages/OfficerHomePage';
import AdminHomePage from './pages/AdminHomePage';
import CaseDetailPage from './pages/CaseDetailPage';
import './App.css';

const HOME_PATH_BY_ROLE = {
  [ROLES.CLIENT]: '/client',
  [ROLES.COMPLIANCE_OFFICER]: '/officer',
  [ROLES.ADMIN_COMPLIANCE_OFFICER]: '/admin',
};

function RoleHomeRedirect() {
  const { user } = useAuth();
  if (!user) return <Navigate to="/login" replace />;
  return <Navigate to={HOME_PATH_BY_ROLE[user.role]} replace />;
}

function App() {
  return (
    <>
      <Navbar />
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/" element={<RoleHomeRedirect />} />
        <Route
          path="/client"
          element={
            <ProtectedRoute allowedRoles={[ROLES.CLIENT]}>
              <ClientHomePage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/officer"
          element={
            <ProtectedRoute allowedRoles={[ROLES.COMPLIANCE_OFFICER]}>
              <OfficerHomePage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/admin"
          element={
            <ProtectedRoute allowedRoles={[ROLES.ADMIN_COMPLIANCE_OFFICER]}>
              <AdminHomePage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/cases/:caseId"
          element={
            <ProtectedRoute
              allowedRoles={[ROLES.COMPLIANCE_OFFICER, ROLES.ADMIN_COMPLIANCE_OFFICER]}
            >
              <CaseDetailPage />
            </ProtectedRoute>
          }
        />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </>
  );
}

export default App;
