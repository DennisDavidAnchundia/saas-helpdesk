import { useState, useEffect } from 'react';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import DashboardPage from './pages/DashboardPage';

type Page = 'login' | 'register' | 'dashboard';

interface AuthState {
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

function App() {
  const [auth, setAuth] = useState<AuthState | null>(getStoredAuth);
  const [page, setPage] = useState<Page>(auth ? 'dashboard' : 'login');
  const [initialSlug, setInitialSlug] = useState('');

  // Handle OAuth callback: /auth/callback?token=xxx&email=xxx
  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const token = params.get('token');
    const email = params.get('email');
    if (token && email) {
      // Decode role from JWT payload
      try {
        const payload = JSON.parse(atob(token.split('.')[1]));
        const role = payload.role || 'CUSTOMER';
        const tenantName = 'Mi Empresa';
        const state = { token, email, role, tenantName };
        setAuth(state);
        localStorage.setItem('token', token);
        localStorage.setItem('email', email);
        localStorage.setItem('role', role);
        localStorage.setItem('tenantName', tenantName);
        setPage('dashboard');
        window.history.replaceState({}, '', '/');
      } catch {
        console.error('Invalid OAuth callback token');
      }
    }
  }, []);

  const handleLogin = (token: string, email: string, role: string, tenantName: string) => {
    const state = { token, email, role, tenantName };
    setAuth(state);
    localStorage.setItem('token', token);
    localStorage.setItem('email', email);
    localStorage.setItem('role', role);
    localStorage.setItem('tenantName', tenantName);
    setPage('dashboard');
  };

  const handleLogout = () => {
    setAuth(null);
    localStorage.clear();
    setPage('login');
  };

  return (
    <>
      {page === 'login' && (
        <LoginPage
          onLogin={handleLogin}
          onGoToRegister={() => setPage('register')}
          initialSlug={initialSlug}
        />
      )}
      {page === 'register' && (
        <RegisterPage
          onRegistered={(slug) => {
            if (slug) setInitialSlug(slug);
            setPage('login');
          }}
          onGoToLogin={() => setPage('login')}
        />
      )}
      {page === 'dashboard' && auth && (
        <DashboardPage
          email={auth.email}
          role={auth.role}
          tenantName={auth.tenantName}
          token={auth.token}
          onLogout={handleLogout}
        />
      )}
    </>
  );
}

export default App;
