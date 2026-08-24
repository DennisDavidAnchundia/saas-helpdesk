import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useBillingMe, useCheckoutSession } from '../hooks/useData';
import { apiDate } from '../lib/time';
import { CreditCardIcon, SparkIcon } from './icons';

type PlanId = 'FREE' | 'PRO' | 'ENTERPRISE';

const PLAN_RANK: Record<PlanId, number> = { FREE: 0, PRO: 1, ENTERPRISE: 2 };

function splitFeatures(t: (k: string) => string, key: string): string[] {
  return t(key).split('|');
}

export default function BillingPanel() {
  const { t, i18n } = useTranslation();
  const billingQuery = useBillingMe();
  const checkout = useCheckoutSession();
  const [error, setError] = useState('');

  const billing = billingQuery.data;
  const currentPlan: PlanId = billing?.plan ?? 'FREE';

  const handleUpgrade = async (target: PlanId) => {
    setError('');
    try {
      const { url } = await checkout.mutateAsync(target as 'PRO' | 'ENTERPRISE');
      window.location.assign(url);
    } catch {
      setError(t('billing.errorCheckout'));
    }
  };

  if (billingQuery.isLoading) {
    return (
      <div className={`${card} animate-pulse`}>
        <div className="mb-3 h-4 w-32 rounded-full bg-slate-200 dark:bg-white/10" />
        <div className="h-24 rounded-xl bg-slate-200 dark:bg-white/10" />
      </div>
    );
  }

  if (billingQuery.error || !billing) {
    return (
      <div className={errorBox}>
        {billingQuery.error?.message ?? t('billing.errorCheckout')}
      </div>
    );
  }

  const statusKey =
    billing.status === 'ACTIVE'
      ? 'billing.statusActive'
      : billing.status === 'PAST_DUE'
        ? 'billing.statusPastDue'
        : 'billing.statusCanceled';
  const unlimited = billing.plan === 'ENTERPRISE';
  const usagePct = unlimited
    ? 0
    : Math.min(100, Math.round((billing.ticketsUsed / Math.max(1, billing.ticketsLimit)) * 100));

  const plans = [
    { id: 'FREE' as PlanId, name: t('billing.freeName'), price: t('billing.freePrice'), tagline: t('billing.freeTagline'), features: splitFeatures(t, 'billing.freeFeatures'), highlight: false },
    { id: 'PRO' as PlanId, name: t('billing.proName'), price: t('billing.proPrice'), tagline: t('billing.proTagline'), features: splitFeatures(t, 'billing.proFeatures'), highlight: true },
    { id: 'ENTERPRISE' as PlanId, name: t('billing.enterpriseName'), price: t('billing.enterprisePrice'), tagline: t('billing.enterpriseTagline'), features: splitFeatures(t, 'billing.enterpriseFeatures'), highlight: false },
  ];

  return (
    <div className="animate-fade-up space-y-6">
      {/* ===== Plan actual ===== */}
      <section className={card}>
        <div className="flex flex-wrap items-center justify-between gap-3">
          <h2 className="flex items-center gap-2.5 font-display text-base font-bold tracking-tight text-slate-900 dark:text-white">
            <CreditCardIcon className="text-lg text-brand-500" />
            {t('billing.currentPlan')}
          </h2>
          <span
            className={`rounded-full px-3 py-1 text-[11px] font-bold uppercase tracking-wide ${
              billing.status === 'ACTIVE'
                ? 'bg-emerald-100 text-emerald-700 dark:bg-emerald-500/15 dark:text-emerald-300'
                : 'bg-red-100 text-red-700 dark:bg-red-500/15 dark:text-red-300'
            }`}
          >
            {t(statusKey)}
          </span>
        </div>

        <p className="mt-3 bg-gradient-to-r from-brand-500 to-violet-500 bg-clip-text font-display text-4xl font-bold tracking-tight text-transparent">
          {planDisplayName(currentPlan, t)}
        </p>

        <div className="mt-5">
          <div className="mb-1.5 flex items-center justify-between text-xs">
            <span className="font-medium text-slate-500 dark:text-slate-400">{t('billing.usage')}</span>
            <span className="font-mono font-semibold text-slate-700 dark:text-slate-200">
              {unlimited
                ? `${billing.ticketsUsed} · ${t('billing.unlimited')}`
                : `${billing.ticketsUsed} / ${billing.ticketsLimit}`}
            </span>
          </div>
          {!unlimited && (
            <div className="h-2.5 overflow-hidden rounded-full bg-slate-100 dark:bg-white/10">
              <div
                className={`h-full rounded-full transition-all duration-500 ${
                  usagePct >= 90
                    ? 'bg-gradient-to-r from-orange-400 to-red-500'
                    : 'bg-gradient-to-r from-brand-500 to-violet-500'
                }`}
                style={{ width: `${usagePct}%` }}
              />
            </div>
          )}
        </div>

        {billing.currentPeriodEnd && (
          <p className="mt-3 text-xs text-slate-400">
            {t('billing.periodUntil')}: {apiDate(billing.currentPeriodEnd).toLocaleDateString(i18n.language)}
          </p>
        )}
      </section>

      {error && <div className={errorBox}>{error}</div>}

      {/* ===== Comparativa de planes ===== */}
      <section className="grid gap-4 md:grid-cols-3">
        {plans.map((plan) => {
          const isCurrent = plan.id === currentPlan;
          const canUpgrade = PLAN_RANK[plan.id] > PLAN_RANK[currentPlan];
          return (
            <div
              key={plan.id}
              className={`relative rounded-2xl border p-5 shadow-sm transition-all duration-200 sm:p-6 ${
                isCurrent
                  ? 'border-brand-400 ring-2 ring-brand-500/25 dark:border-brand-500/60'
                  : plan.highlight && canUpgrade
                    ? 'border-slate-200/70 hover:-translate-y-1 hover:border-brand-300 hover:shadow-xl hover:shadow-brand-500/10 dark:border-white/10 dark:hover:border-brand-500/40'
                    : 'border-slate-200/70 dark:border-white/10'
              } bg-white dark:bg-white/[0.03]`}
            >
              {isCurrent && (
                <span className="absolute -top-2.5 left-1/2 -translate-x-1/2 rounded-full bg-gradient-to-r from-brand-500 to-violet-500 px-3 py-0.5 text-[10px] font-bold uppercase tracking-wider text-white shadow-md shadow-brand-500/30">
                  {t('billing.currentBadge')}
                </span>
              )}

              {plan.highlight && !isCurrent && (
                <SparkIcon className="absolute right-4 top-4 text-lg text-brand-400" />
              )}

              <h3 className="font-display text-sm font-bold uppercase tracking-wide text-slate-500 dark:text-slate-400">
                {plan.name}
              </h3>
              <p className="mt-2">
                <span className="font-display text-3xl font-bold tracking-tight text-slate-900 dark:text-white">
                  {plan.price}
                </span>
                <span className="text-sm text-slate-400">{t('billing.perMonth')}</span>
              </p>
              <p className="mt-1 text-xs text-slate-400">{plan.tagline}</p>

              <ul className="mt-4 space-y-2 border-t border-slate-100 pt-4 dark:border-white/5">
                {plan.features.map((f) => (
                  <li key={f} className="flex items-start gap-2 text-sm text-slate-600 dark:text-slate-300">
                    <span className="mt-0.5 grid size-4 shrink-0 place-items-center rounded-full bg-brand-100 text-[9px] text-brand-600 dark:bg-brand-500/20 dark:text-brand-300">
                      ✓
                    </span>
                    {f}
                  </li>
                ))}
              </ul>

              <div className="mt-5">
                {isCurrent ? (
                  <button
                    disabled
                    className="w-full cursor-not-allowed rounded-xl bg-slate-100 py-2.5 text-sm font-semibold text-slate-400 dark:bg-white/5 dark:text-slate-500"
                  >
                    {t('billing.currentBadge')}
                  </button>
                ) : canUpgrade ? (
                  <button
                    onClick={() => handleUpgrade(plan.id)}
                    disabled={checkout.isPending}
                    className={`w-full cursor-pointer rounded-xl py-2.5 text-sm font-semibold text-white transition-all duration-200 hover:-translate-y-0.5 disabled:cursor-not-allowed disabled:opacity-60 ${
                      plan.highlight
                        ? 'bg-gradient-to-r from-brand-500 to-violet-500 shadow-lg shadow-brand-500/25 hover:shadow-xl hover:shadow-brand-500/40'
                        : 'bg-slate-800 hover:bg-slate-900 dark:bg-slate-700 dark:hover:bg-slate-600'
                    }`}
                  >
                    {checkout.isPending ? t('billing.processing') : t('billing.upgradeCta', { plan: plan.name })}
                  </button>
                ) : null}
              </div>
            </div>
          );
        })}
      </section>
    </div>
  );
}

const card =
  'rounded-2xl border border-slate-200/70 bg-white p-5 shadow-sm sm:p-6 dark:border-white/10 dark:bg-white/[0.03]';
const errorBox =
  'rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 dark:border-red-500/20 dark:bg-red-500/10 dark:text-red-300';

function planDisplayName(plan: PlanId, t: (k: string) => string): string {
  if (plan === 'PRO') return t('billing.proName');
  if (plan === 'ENTERPRISE') return t('billing.enterpriseName');
  return t('billing.freeName');
}
