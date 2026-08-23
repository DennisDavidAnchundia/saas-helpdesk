import type { ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import ThemeToggle from './ThemeToggle';
import LanguageSwitcher from './LanguageSwitcher';
import { SparkIcon } from '../icons';

interface AuthLayoutProps {
  title: string;
  subtitle: string;
  children: ReactNode;
}

export default function AuthLayout({ title, subtitle, children }: AuthLayoutProps) {
  const { t } = useTranslation();

  const features = [
    t('auth.feature1'),
    t('auth.feature2'),
    t('auth.feature3'),
    t('auth.feature4'),
  ];

  return (
    <div className="grid min-h-dvh lg:grid-cols-2">
      {/* ===== Panel de marca (solo desktop) ===== */}
      <div className="relative hidden overflow-hidden bg-brand-950 lg:flex lg:flex-col lg:justify-between lg:p-12 dark:bg-[#07070f]">
        {/* Blobs decorativos */}
        <div className="absolute -left-24 -top-24 size-96 rounded-full bg-brand-500/30 blur-3xl" />
        <div className="absolute -bottom-32 -right-20 size-[28rem] rounded-full bg-violet-500/25 blur-3xl" />
        <div className="absolute left-1/3 top-1/2 size-72 rounded-full bg-fuchsia-500/10 blur-3xl" />

        <div className="relative flex items-center gap-3">
          <div className="grid size-11 place-items-center rounded-xl bg-white/10 text-white ring-1 ring-white/20 backdrop-blur">
            <SparkIcon className="text-xl" />
          </div>
          <div className="leading-tight">
            <p className="font-display text-lg font-bold tracking-tight text-white">HelpDesk</p>
            <p className="text-[11px] font-medium uppercase tracking-widest text-brand-300">
              SaaS Platform
            </p>
          </div>
        </div>

        <div className="relative max-w-md">
          <h1 className="font-display text-4xl font-bold leading-tight tracking-tight text-white">
            {t('auth.brandTagline')}{' '}
            <span className="bg-gradient-to-r from-brand-300 to-violet-300 bg-clip-text text-transparent">
              {t('auth.brandTaglineAccent')}
            </span>
          </h1>
          <ul className="mt-8 space-y-3.5">
            {features.map((f) => (
              <li key={f} className="flex items-center gap-3 text-sm text-slate-300">
                <span className="grid size-5 shrink-0 place-items-center rounded-full bg-brand-500/25 text-[10px] text-brand-200 ring-1 ring-brand-400/40">
                  ✓
                </span>
                {f}
              </li>
            ))}
          </ul>
        </div>

        <p className="relative text-xs text-slate-400">{t('auth.copyright')}</p>
      </div>

      {/* ===== Formulario ===== */}
      <div className="relative flex items-center justify-center p-4 sm:p-8">
        <div className="absolute right-4 top-4 flex gap-2 sm:right-6 sm:top-6">
          <LanguageSwitcher size="sm" />
          <ThemeToggle size="sm" />
        </div>

        <div className="w-full max-w-md animate-fade-up">
          {/* Logo compacto para móvil */}
          <div className="mb-8 flex items-center justify-center gap-3 lg:hidden">
            <div className="grid size-10 place-items-center rounded-xl bg-gradient-to-br from-brand-500 to-violet-500 text-white shadow-lg shadow-brand-500/30">
              <SparkIcon className="text-xl" />
            </div>
            <p className="font-display text-lg font-bold tracking-tight text-slate-900 dark:text-white">
              HelpDesk
            </p>
          </div>

          <div className="rounded-2xl border border-slate-200/70 bg-white p-7 shadow-xl shadow-slate-900/5 sm:p-9 dark:border-white/10 dark:bg-white/[0.03] dark:shadow-black/20">
            <h2 className="font-display text-2xl font-bold tracking-tight text-slate-900 dark:text-white">
              {title}
            </h2>
            <p className="mt-1.5 text-sm text-slate-500 dark:text-slate-400">{subtitle}</p>
            <div className="mt-7">{children}</div>
          </div>
        </div>
      </div>
    </div>
  );
}
