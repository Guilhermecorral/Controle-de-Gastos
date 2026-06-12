import { lazy, Suspense } from 'react'
import { Navigate, Route, Routes } from 'react-router-dom'
import { useAuthStore } from '../store/auth'

const LandingPage = lazy(() => import('../features/landing/pages/LandingPage'))
const LoginPage = lazy(() => import('../features/auth/pages/LoginPage'))
const RegisterPage = lazy(() => import('../features/auth/pages/RegisterPage'))
const ForgotPasswordPage = lazy(() => import('../features/auth/pages/ForgotPasswordPage'))
const ResetPasswordPage = lazy(() => import('../features/auth/pages/ResetPasswordPage'))
const NotFoundPage = lazy(() => import('../features/not-found/pages/NotFoundPage'))
const WorkspacePage = lazy(() => import('../features/workspace/pages/WorkspacePage'))

function RouteLoader() {
  return (
    <div className="flex min-h-screen items-center justify-center bg-[#f4f6f1] px-4">
      <div className="rounded-[28px] border border-emerald-100 bg-white px-6 py-5 text-sm font-semibold text-slate-600 shadow-[0_18px_60px_rgba(15,23,42,0.06)]">
        Carregando a próxima etapa com segurança...
      </div>
    </div>
  )
}

export default function AppRouter() {
  const { isAuthenticated, logout } = useAuthStore()

  return (
    <Suspense fallback={<RouteLoader />}>
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
    </Suspense>
  )
}
