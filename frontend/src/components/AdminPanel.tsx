import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import type { SlaPolicy, UserInfo, UserRole } from '../services/api';
import { useSetUserActive, useSlaPolicy, useUpdateSlaPolicy, useUsers } from '../hooks/useData';

const ROLE_STYLES: Record<UserRole, string> = {
  ADMIN: 'bg-violet-100 text-violet-700 dark:bg-violet-500/15 dark:text-violet-300',
  AGENT: 'bg-sky-100 text-sky-700 dark:bg-sky-500/15 dark:text-sky-300',
  CUSTOMER: 'bg-slate-100 text-slate-600 dark:bg-white/10 dark:text-slate-400',
};

export default function AdminPanel() {
  const { t } = useTranslation();
  const usersQuery = useUsers(true);
  const toggleMutation = useSetUserActive();
  const slaQuery = useSlaPolicy(true);
  const slaMutation = useUpdateSlaPolicy();

  const [slaForm, setSlaForm] = useState<SlaPolicy | null>(null);
  const [savedNote, setSavedNote] = useState(false);

  // Sincroniza el formulario cuando llegan los datos del servidor
  useEffect(() => {
    if (slaQuery.data) setSlaForm(slaQuery.data);
  }, [slaQuery.data]);

  const users = usersQuery.data ?? [];
  const error = usersQuery.error?.message ?? toggleMutation.error?.message ?? '';

  const handleToggle = async (u: UserInfo) => {
    setSavedNote(false);
    try {
      await toggleMutation.mutateAsync({ id: u.id, isActive: !u.active });
    } catch {
      /* el error ya vive en la mutacion */
    }
  };

  const handleSaveSla = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!slaForm) return;
    try {
      await slaMutation.mutateAsync(slaForm);
      setSavedNote(true);
    } catch {
      /* el error ya vive en la mutacion */
    }
  };

  const slaField = (
    label: string,
    key: keyof Pick<SlaPolicy, 'urgentHours' | 'highHours' | 'mediumHours' | 'lowHours'>,
  ) => (
    <label className="flex flex-col gap-1.5">
      <span className="text-xs font-semibold text-slate-500 dark:text-slate-400">{label}</span>
      <span className="relative">
        <input
          type="number"
          min={1}
          max={720}
          value={slaForm?.[key] ?? ''}
          onChange={(e) => {
            setSavedNote(false);
            setSlaForm((prev) => (prev ? { ...prev, [key]: Number(e.target.value) } : prev));
          }}
          className="w-full rounded-xl border border-slate-300 bg-white px-3 py-2.5 pr-9 text-sm outline-none transition-shadow focus:border-brand-400 focus:ring-4 focus:ring-brand-500/15 dark:border-white/10 dark:bg-white/5 dark:text-white"
        />
        <span className="pointer-events-none absolute right-3 top-1/2 -translate-y-1/2 text-xs text-slate-400">h</span>
      </span>
    </label>
  );

  return (
    <>
      {/* Usuarios del tenant */}
      <section className="rounded-2xl border border-slate-200/70 bg-white p-5 shadow-sm sm:p-6 dark:border-white/10 dark:bg-white/[0.03]">
        <div className="mb-4 flex flex-wrap items-center justify-between gap-2">
          <h2 className="font-display text-base font-bold tracking-tight text-slate-900 dark:text-white">
            {t('admin.title')}
          </h2>
          <p className="text-xs text-slate-400">{t('admin.subtitle')}</p>
        </div>

        {usersQuery.isLoading && (
          <p className="py-8 text-center text-sm text-slate-400">{t('admin.loading')}</p>
        )}

        <ul className="divide-y divide-slate-100 dark:divide-white/5">
          {users.map((u) => (
            <li key={u.id} className="flex flex-wrap items-center gap-3 py-3">
              <span className="grid size-9 shrink-0 place-items-center rounded-full bg-gradient-to-br from-brand-500 to-violet-500 text-xs font-bold text-white">
                {u.fullName.split(' ').map((w) => w[0]).slice(0, 2).join('').toUpperCase()}
              </span>
              <span className="min-w-0 flex-1">
                <span className="block truncate text-sm font-semibold text-slate-800 dark:text-slate-100">
                  {u.fullName}
                </span>
                <span className="block truncate text-xs text-slate-400">{u.email}</span>
              </span>
              <span className={`rounded-full px-2 py-0.5 text-[11px] font-semibold ${ROLE_STYLES[u.role]}`}>
                {u.role}
              </span>
              <span
                className={`rounded-full px-2 py-0.5 text-[11px] font-semibold ${
                  u.active
                    ? 'bg-emerald-100 text-emerald-700 dark:bg-emerald-500/15 dark:text-emerald-300'
                    : 'bg-red-100 text-red-700 dark:bg-red-500/15 dark:text-red-300'
                }`}
              >
                {u.active ? t('admin.active') : t('admin.inactive')}
              </span>
              {u.role === 'AGENT' && (
                <button
                  onClick={() => handleToggle(u)}
                  disabled={toggleMutation.isPending}
                  className={`cursor-pointer rounded-xl border px-3 py-1.5 text-[11px] font-semibold transition-all hover:-translate-y-0.5 disabled:cursor-not-allowed disabled:opacity-50 ${
                    u.active
                      ? 'border-red-200 text-red-600 hover:bg-red-50 dark:border-red-500/30 dark:text-red-300 dark:hover:bg-red-500/10'
                      : 'border-emerald-200 text-emerald-600 hover:bg-emerald-50 dark:border-emerald-500/30 dark:text-emerald-300 dark:hover:bg-emerald-500/10'
                  }`}
                >
                  {u.active ? t('admin.deactivate') : t('admin.activate')}
                </button>
              )}
            </li>
          ))}
        </ul>

        {users.length === 0 && !usersQuery.isLoading && !error && (
          <p className="py-8 text-center text-sm text-slate-400">{t('admin.empty')}</p>
        )}

        {error && (
          <div className="mt-4 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 dark:border-red-500/20 dark:bg-red-500/10 dark:text-red-300">
            {error}
          </div>
        )}
      </section>

      {/* SLA configurable */}
      <section className="rounded-2xl border border-slate-200/70 bg-white p-5 shadow-sm sm:p-6 dark:border-white/10 dark:bg-white/[0.03]">
        <h2 className="font-display text-base font-bold tracking-tight text-slate-900 dark:text-white">
          {t('admin.slaTitle')}
        </h2>
        <p className="mt-1 text-xs text-slate-400">{t('admin.slaSubtitle')}</p>

        {!slaQuery.isLoading && slaForm && (
          <form onSubmit={handleSaveSla} className="mt-4 space-y-4">
            <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
              {slaField(t('admin.slaUrgent'), 'urgentHours')}
              {slaField(t('admin.slaHigh'), 'highHours')}
              {slaField(t('admin.slaMedium'), 'mediumHours')}
              {slaField(t('admin.slaLow'), 'lowHours')}
            </div>
            <div className="flex flex-wrap items-center gap-3">
              <button
                type="submit"
                disabled={slaMutation.isPending}
                className="cursor-pointer rounded-xl bg-gradient-to-r from-brand-500 to-violet-500 px-5 py-2.5 text-sm font-semibold text-white shadow-lg shadow-brand-500/25 transition-all duration-200 hover:-translate-y-0.5 hover:shadow-xl disabled:cursor-not-allowed disabled:opacity-60 disabled:hover:translate-y-0"
              >
                {slaMutation.isPending ? t('admin.saving') : t('admin.save')}
              </button>
              {savedNote && !slaMutation.error && (
                <span className="text-sm font-medium text-emerald-600 dark:text-emerald-400">
                  ✓ {t('admin.saved')}
                </span>
              )}
              {slaMutation.error && (
                <span className="text-sm text-red-600 dark:text-red-400">{slaMutation.error.message}</span>
              )}
            </div>
          </form>
        )}
      </section>
    </>
  );
}
