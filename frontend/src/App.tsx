import { useEffect, useMemo, useState } from 'react';
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import type { ReactNode } from 'react';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import JoinPage from './pages/JoinPage';
import DashboardPage from './pages/DashboardPage';

export interface AuthState {
  token: string;
  email: string;
  role: string;
  tenantName: string;
}

function getStoredAuth(): AuthState | null {
  const token = localStorage.getItem('token');
  const email = localStorage.getItem('email');
  const role = localStorage.getItem('role');
  const tenantName = localStorage.getItem('tenantName');
  if (token && email && role && tenantName) {
    return { token, email, role, tenantName };
  }
  return null;
}

/** Redirige a /login si no hay sesión activa. */
function ProtectedRoute({ children }: { children: ReactNode }) {
  if (!getStoredAuth()) {
    return <Navigate to="/login" replace />;
  }
  return <>{children}</>;
}

/** Parsea el callback de OAuth (?token=...&email=...); null si falta algo o el JWT es invalido. */
function parseCallbackSession(): AuthState | null {
  const params = new URLSearchParams(window.location.search);
  const token = params.get('token');
  const email = params.get('email');
  if (!token || !email) return null;
  try {
    const payload = JSON.parse(atob(token.split('.')[1])) as { role?: string; tenantName?: string };
    return {
      token,
      email,
      role: payload.role ?? 'CUSTOMER',
      tenantName: payload.tenantName ?? 'Mi Empresa',
    };
  } catch {
    return null;
  }
}

/** Recibe el callback de OAuth, guarda la sesion y entra. */
function OAuthCallback({ onSuccess }: { onSuccess: (auth: AuthState) => void }) {
  const { t } = useTranslation();
  // La URL no cambia mientras el componente vive: la sesion se deriva una sola vez
  const session = useMemo(() => parseCallbackSession(), []);

  // Sincroniza con sistemas externos (localStorage + historial del navegador)
  useEffect(() => {
    if (!session) return;
    localStorage.setItem('token', session.token);
    localStorage.setItem('email', session.email);
    localStorage.setItem('role', session.role);
    localStorage.setItem('tenantName', session.tenantName);
    onSuccess(session);
    window.history.replaceState({}, '', '/');
  }, [session, onSuccess]);

  if (!session) return <Navigate to="/login" replace />;

  return (
    <div className="grid min-h-dvh place-items-center">
      <div className="text-center">
        <div className="mx-auto size-10 animate-spin rounded-full border-4 border-brand-200 border-t-brand-500" />
        <p className="mt-4 text-sm text-slate-500">{t('auth.completingLogin')}</p>
      </div>
    </div>
  );
}

function App() {
  const [auth, setAuth] = useState<AuthState | null>(getStoredAuth);

  const handleLoginSuccess = (next: AuthState) => setAuth(next);

  const handleLogin = (token: string, email: string, role: string, tenantName: string) => {
    const state = { token, email, role, tenantName };
    localStorage.setItem('token', token);
    localStorage.setItem('email', email);
    localStorage.setItem('role', role);
    localStorage.setItem('tenantName', tenantName);
    setAuth(state);
  };

  const handleLogout = () => {
    setAuth(null);
    localStorage.clear();
  };

  return (
    <BrowserRouter>
      <Routes>
        <Route
          path="/login"
          element={
            auth ? (
              <Navigate to="/" replace />
            ) : (
              <LoginPage onSuccess={handleLogin} />
            )
          }
        />
        <Route path="/register" element={<RegisterPage />} />
        <Route path="/join" element={<JoinPage />} />
        <Route
          path="/auth/callback"
          element={auth ? <Navigate to="/" replace /> : <OAuthCallback onSuccess={handleLoginSuccess} />}
        />
        <Route
          path="/"
          element={
            <ProtectedRoute>
              {auth && (
                <DashboardPage
                  email={auth.email}
                  role={auth.role}
                  tenantName={auth.tenantName}
                  token={auth.token}
                  onLogout={handleLogout}
                />
              )}
            </ProtectedRoute>
          }
        />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
