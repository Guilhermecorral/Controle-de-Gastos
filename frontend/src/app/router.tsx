import { Navigate, Route, Routes } from 'react-router-dom';
import LandingPage from '../features/landing/pages/LandingPage';
import LoginPage from '../features/auth/pages/LoginPage';
import RegisterPage from '../features/auth/pages/RegisterPage';
import ForgotPasswordPage from '../features/auth/pages/ForgotPasswordPage';
import ResetPasswordPage from '../features/auth/pages/ResetPasswordPage';
import NotFoundPage from '../features/not-found/pages/NotFoundPage';
import WorkspacePage from '../features/workspace/pages/WorkspacePage';
import { useAuthStore } from '../store/auth';

export default function AppRouter() {
  const { isAuthenticated, logout } = useAuthStore();

  return (
    <Routes>
      <Route path="/" element={<LandingPage isLoggedIn={isAuthenticated} />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/cadastro" element={<RegisterPage />} />
      <Route path="/esqueci-a-senha" element={<ForgotPasswordPage />} />
      <Route path="/redefinir-senha" element={<ResetPasswordPage />} />
      <Route
        path="/app"
        element={isAuthenticated ? <WorkspacePage onLogout={logout} /> : <Navigate replace to="/login" />}
      />
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  );
}
