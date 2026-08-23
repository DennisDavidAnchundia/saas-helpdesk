import { useEffect, useState } from 'react';
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import type { ReactNode } from 'react';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
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

/** Recibe el callback de OAuth (?token=...&email=...), guarda la sesion y entra. */
function OAuthCallback({ onSuccess }: { onSuccess: (auth: AuthState) => void }) {
  const [error, setError] = useState(false);

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const token = params.get('token');
    const email = params.get('email');
    if (!token || !email) {
      setError(true);
      return;
    }
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      const role = payload.role || 'CUSTOMER';
      const tenantName = payload.tenantName || 'Mi Empresa';
      localStorage.setItem('token', token);
      localStorage.setItem('email', email);
      localStorage.setItem('role', role);
      localStorage.setItem('tenantName', tenantName);
      onSuccess({ token, email, role, tenantName });
      window.history.replaceState({}, '', '/');
    } catch {
      setError(true);
    }
  }, [onSuccess]);

  if (error) return <Navigate to="/login" replace />;

  return (
    <div className="grid min-h-dvh place-items-center">
      <div className="text-center">
        <div className="mx-auto size-10 animate-spin rounded-full border-4 border-brand-200 border-t-brand-500" />
        <p className="mt-4 text-sm text-slate-500">Completando inicio de sesión…</p>
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
