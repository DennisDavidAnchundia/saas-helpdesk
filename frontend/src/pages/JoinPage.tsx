import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useNavigate } from 'react-router-dom';
import AuthLayout from '../components/layout/AuthLayout';
import { join } from '../services/api';

const inputCls =
  'w-full rounded-xl border border-slate-300 bg-white px-3.5 py-2.5 text-sm outline-none transition-shadow placeholder:text-slate-400 focus:border-brand-400 focus:ring-4 focus:ring-brand-500/15 dark:border-white/10 dark:bg-white/5 dark:text-white';

/** Registro publico como cliente de una empresa existente (portal). */
export default function JoinPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [tenantSlug, setTenantSlug] = useState('');
  const [error, setError] = useState('');
  const [successSlug, setSuccessSlug] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setSuccessSlug('');
    setLoading(true);
    try {
      const res = await join({ fullName, email, password, tenantSlug });
      setSuccessSlug(res.tenantSlug || tenantSlug);
    } catch (err: any) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  if (successSlug) {
    return (
      <AuthLayout title={t('auth.joinedTitle')} subtitle={t('auth.joinedSubtitle')}>
        <div className="text-center">
          <div className="mx-auto mb-5 grid size-16 place-items-center rounded-full bg-emerald-100 text-emerald-600 dark:bg-emerald-500/15 dark:text-emerald-400">
            <svg className="size-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.2} d="M5 13l4 4L19 7" />
            </svg>
          </div>
          <button
            onClick={() => navigate('/login', { state: { slug: successSlug }, replace: true })}
            className="mt-2 w-full cursor-pointer rounded-xl bg-gradient-to-r from-brand-500 to-violet-500 py-2.5 font-display text-sm font-bold tracking-wide text-white shadow-lg shadow-brand-500/30 transition-all duration-200 hover:-translate-y-0.5"
          >
            {t('auth.goToLogin')}
          </button>
        </div>
      </AuthLayout>
    );
  }

  return (
    <AuthLayout title={t('auth.joinTitle')} subtitle={t('auth.joinSubtitle')}>
      <form onSubmit={handleSubmit} className="space-y-4">
        <div>
          <label className="mb-1.5 block text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">
            {t('auth.tenantSlug')}
          </label>
          <input
            type="text"
            value={tenantSlug}
            onChange={(e) => setTenantSlug(e.target.value)}
            className={inputCls}
            placeholder="mi-empresa"
            required
          />
        </div>
        <div>
          <label className="mb-1.5 block text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">
            {t('auth.fullName')}
          </label>
          <input
            type="text"
            value={fullName}
            onChange={(e) => setFullName(e.target.value)}
            className={inputCls}
            placeholder="Carla Cliente"
            required
          />
        </div>
        <div>
          <label className="mb-1.5 block text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">
            {t('auth.email')}
          </label>
          <input
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            className={inputCls}
            placeholder="tu@email.com"
            required
          />
        </div>
        <div>
          <label className="mb-1.5 block text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">
            {t('auth.password')}
          </label>
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            className={inputCls}
            placeholder="Mínimo 8"
            minLength={8}
            required
          />
        </div>

        {error && (
          <div className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 dark:border-red-500/20 dark:bg-red-500/10 dark:text-red-300">
            {error}
          </div>
        )}

        <button
          type="submit"
          disabled={loading}
          className="w-full cursor-pointer rounded-xl bg-gradient-to-r from-brand-500 to-violet-500 py-2.5 font-display text-sm font-bold tracking-wide text-white shadow-lg shadow-brand-500/30 transition-all duration-200 hover:-translate-y-0.5 hover:shadow-xl hover:shadow-brand-500/40 disabled:cursor-not-allowed disabled:opacity-60 disabled:hover:translate-y-0"
        >
          {loading ? t('auth.creatingAccount') : t('auth.joinBtn')}
        </button>
      </form>

      <p className="mt-7 text-center text-sm text-slate-500 dark:text-slate-400">
        {t('auth.wantCompany')}{' '}
        <Link
          to="/register"
          className="font-semibold text-brand-500 transition-colors hover:text-brand-600 dark:text-brand-400"
        >
          {t('auth.registerLink')}
        </Link>
      </p>
    </AuthLayout>
  );
}
