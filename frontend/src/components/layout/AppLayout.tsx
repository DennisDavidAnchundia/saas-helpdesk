import { useState } from 'react';
import type { ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import { useChangeOwnPassword } from '../../hooks/useData';
import ThemeToggle from './ThemeToggle';
import LanguageSwitcher from './LanguageSwitcher';
import {
  CloseIcon,
  LogoutIcon,
  MenuIcon,
  SparkIcon,
} from '../icons';

export interface NavItem {
  id: string;
  label: string;
  icon: ReactNode;
  badge?: string;
}

export interface LayoutUser {
  email: string;
  role: string;
  tenantName: string;
}

interface AppLayoutProps {
  items: NavItem[];
  activeId: string;
  onNavigate: (id: string) => void;
  user: LayoutUser;
  onLogout: () => void;
  actions?: ReactNode;
  children: ReactNode;
}

function Logo() {
  return (
    <div className="flex items-center gap-3">
      <div className="grid size-10 place-items-center rounded-xl bg-gradient-to-br from-brand-500 to-violet-500 text-white shadow-lg shadow-brand-500/30">
        <SparkIcon className="text-xl" />
      </div>
      <div className="leading-tight">
        <p className="font-display text-lg font-bold tracking-tight text-slate-900 dark:text-white">
          HelpDesk
        </p>
        <p className="text-[11px] font-medium uppercase tracking-widest text-brand-500 dark:text-brand-400">
          SaaS Platform
        </p>
      </div>
    </div>
  );
}

export default function AppLayout({
  items,
  activeId,
  onNavigate,
  user,
  onLogout,
  actions,
  children,
}: AppLayoutProps) {
  const { t } = useTranslation();
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [passModalOpen, setPassModalOpen] = useState(false);
  const [currentPass, setCurrentPass] = useState('');
  const [newPass, setNewPass] = useState('');
  const changePassMutation = useChangeOwnPassword();

  const closePassModal = () => {
    setPassModalOpen(false);
    setCurrentPass('');
    setNewPass('');
  };

  const handleChangePassword = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await changePassMutation.mutateAsync({ currentPassword: currentPass, newPassword: newPass });
      closePassModal();
    } catch {
      /* el error ya vive en la mutacion */
    }
  };

  const activeItem = items.find((i) => i.id === activeId);

  const navList = (onSelect?: () => void) => (
    <nav className="flex flex-col gap-1">
      {items.map((item) => {
        const active = item.id === activeId;
        return (
          <button
            key={item.id}
            type="button"
            onClick={() => {
              onNavigate(item.id);
              onSelect?.();
            }}
            className={`group flex w-full cursor-pointer items-center gap-3 rounded-xl px-3.5 py-2.5 text-sm font-medium transition-all duration-200 ${
              active
                ? 'bg-gradient-to-r from-brand-500 to-violet-500 text-white shadow-lg shadow-brand-500/25'
                : 'text-slate-600 hover:bg-slate-100 hover:text-slate-900 dark:text-slate-400 dark:hover:bg-white/5 dark:hover:text-white'
            }`}
          >
            <span className={`text-lg ${active ? '' : 'text-slate-400 group-hover:text-brand-500 dark:group-hover:text-brand-400'}`}>
              {item.icon}
            </span>
            <span className="flex-1 text-left">{item.label}</span>
            {item.badge && (
              <span
                className={`rounded-full px-2 py-0.5 text-[10px] font-semibold ${
                  active
                    ? 'bg-white/20 text-white'
                    : 'bg-brand-100 text-brand-600 dark:bg-brand-500/15 dark:text-brand-300'
                }`}
              >
                {item.badge}
              </span>
            )}
          </button>
        );
      })}
    </nav>
  );

  const userCard = (
    <div className="rounded-2xl border border-slate-200/80 bg-white/70 p-3 dark:border-white/10 dark:bg-white/5">
      <div className="flex items-center gap-3">
        <div className="grid size-9 shrink-0 place-items-center rounded-full bg-gradient-to-br from-brand-400 to-violet-500 font-display text-sm font-bold text-white">
          {user.email.charAt(0).toUpperCase()}
        </div>
        <div className="min-w-0 flex-1 leading-tight">
          <p className="truncate text-sm font-semibold text-slate-800 dark:text-slate-100">
            {user.email}
          </p>
          <p className="truncate text-xs text-slate-400">
            {user.tenantName} · {user.role}
          </p>
        </div>
        <button
          type="button"
          onClick={() => setPassModalOpen(true)}
          title={t('layout.changePassword')}
          className="cursor-pointer rounded-lg p-2 text-slate-400 transition-colors hover:bg-brand-50 hover:text-brand-500 dark:hover:bg-brand-500/10"
        >
          <svg className="text-base" fill="none" stroke="currentColor" strokeWidth={1.8} viewBox="0 0 24 24" width="1em" height="1em">
            <path strokeLinecap="round" strokeLinejoin="round" d="M15.75 5.25a3 3 0 013 3m3 0a6 6 0 01-7.029 5.912c-.563-.097-1.159.026-1.563.43L10.5 17.25H8.25v2.25H6v2.25H2.25v-2.818c0-.597.237-1.17.659-1.591l6.499-6.499c.404-.404.527-1 .43-1.563A6 6 0 1121.75 8.25z" />
          </svg>
        </button>
        <button
          type="button"
          onClick={onLogout}
          title={t('layout.logout')}
          className="cursor-pointer rounded-lg p-2 text-slate-400 transition-colors hover:bg-red-50 hover:text-red-500 dark:hover:bg-red-500/10"
        >
          <LogoutIcon className="text-base" />
        </button>
      </div>
    </div>
  );

  return (
    <div className="min-h-dvh">
      {/* ===== Sidebar desktop ===== */}
      <aside className="fixed inset-y-0 left-0 z-30 hidden w-72 flex-col justify-between border-r border-slate-200/70 bg-white/60 p-5 backdrop-blur-xl lg:flex dark:border-white/5 dark:bg-[#0c0c16]/80">
        <div className="flex flex-col gap-8">
          <Logo />
          <div>
            <p className="mb-2 px-3.5 text-[11px] font-semibold uppercase tracking-widest text-slate-400 dark:text-slate-500">
              {t('nav.workspace')}
            </p>
            {navList()}
          </div>
        </div>
        {userCard}
      </aside>

      {/* ===== Drawer móvil ===== */}
      {drawerOpen && (
        <div className="fixed inset-0 z-40 lg:hidden">
          <div
            className="absolute inset-0 animate-fade-up bg-slate-950/50 backdrop-blur-sm"
            onClick={() => setDrawerOpen(false)}
          />
          <aside className="animate-fade-up absolute inset-y-0 left-0 flex w-72 flex-col justify-between bg-white p-5 shadow-2xl dark:bg-[#0c0c16]">
            <div className="flex flex-col gap-8">
              <div className="flex items-center justify-between">
                <Logo />
                <button
                  type="button"
                  onClick={() => setDrawerOpen(false)}
                  className="cursor-pointer rounded-lg p-2 text-slate-400 hover:bg-slate-100 dark:hover:bg-white/5"
                >
                  <CloseIcon className="text-lg" />
                </button>
              </div>
              {navList(() => setDrawerOpen(false))}
            </div>
            {userCard}
          </aside>
        </div>
      )}

      {/* ===== Contenido principal ===== */}
      <div className="lg:pl-72">
        <header className="sticky top-0 z-20 flex h-16 items-center gap-4 border-b border-slate-200/70 bg-[#f6f7fb]/80 px-4 backdrop-blur-xl sm:px-6 lg:px-8 dark:border-white/5 dark:bg-[#09090f]/80">
          <button
            type="button"
            onClick={() => setDrawerOpen(true)}
            className="grid size-9 cursor-pointer place-items-center rounded-xl border border-slate-200/80 bg-white/70 text-base text-slate-500 lg:hidden dark:border-white/10 dark:bg-white/5 dark:text-slate-400"
          >
            <MenuIcon />
          </button>

          <div className="min-w-0 flex-1">
            <h1 className="truncate font-display text-lg font-bold tracking-tight text-slate-900 dark:text-white">
              {activeItem?.label ?? t('layout.dashboard')}
            </h1>
          </div>

          {actions}
          <LanguageSwitcher />
          <ThemeToggle />
        </header>

        <main className="mx-auto max-w-7xl px-4 py-6 sm:px-6 lg:px-8">{children}</main>
      </div>

      {/* ===== Modal cambio de contrasena ===== */}
      {passModalOpen && (
        <div className="fixed inset-0 z-50 grid place-items-center p-4">
          <div
            className="absolute inset-0 bg-slate-950/50 backdrop-blur-sm"
            onClick={closePassModal}
          />
          <form
            onSubmit={handleChangePassword}
            className="animate-fade-up relative w-full max-w-sm rounded-3xl border border-slate-200/80 bg-white p-6 shadow-2xl dark:border-white/10 dark:bg-[#12121e]"
          >
            <button
              type="button"
              onClick={closePassModal}
              className="absolute right-4 top-4 cursor-pointer rounded-lg p-1.5 text-slate-400 transition-colors hover:bg-slate-100 hover:text-slate-600 dark:hover:bg-white/5 dark:hover:text-slate-200"
            >
              <CloseIcon />
            </button>
            <h2 className="font-display text-lg font-bold text-slate-900 dark:text-white">
              {t('layout.changePassword')}
            </h2>
            <p className="mt-1 text-xs text-slate-400">
              {t('layout.changePasswordHint')}
            </p>
            <div className="mt-5 space-y-3">
              <input
                type="password"
                value={currentPass}
                onChange={(e) => setCurrentPass(e.target.value)}
                placeholder={t('layout.currentPassword')}
                required
                autoFocus
                className="w-full rounded-xl border border-slate-300 bg-white px-3.5 py-2.5 text-sm outline-none focus:border-brand-400 dark:border-white/10 dark:bg-white/5 dark:text-white"
              />
              <input
                type="password"
                value={newPass}
                onChange={(e) => setNewPass(e.target.value)}
                placeholder={t('layout.newPassword')}
                required
                minLength={8}
                className="w-full rounded-xl border border-slate-300 bg-white px-3.5 py-2.5 text-sm outline-none focus:border-brand-400 dark:border-white/10 dark:bg-white/5 dark:text-white"
              />
              <p className="text-[11px] text-slate-400">{t('layout.minChars')}</p>
            </div>
            {changePassMutation.error && (
              <p className="mt-3 rounded-xl bg-red-50 px-3 py-2 text-xs text-red-600 dark:bg-red-500/10 dark:text-red-400">
                {changePassMutation.error.message}
              </p>
            )}
            <button
              type="submit"
              disabled={changePassMutation.isPending || newPass.length < 8}
              className="mt-5 w-full cursor-pointer rounded-xl bg-gradient-to-r from-brand-500 to-violet-500 px-4 py-2.5 text-sm font-semibold text-white shadow-lg shadow-brand-500/30 transition-all hover:-translate-y-0.5 disabled:cursor-not-allowed disabled:opacity-50"
            >
              {changePassMutation.isPending ? t('common.saving') : t('layout.savePassword')}
            </button>
          </form>
        </div>
      )}
    </div>
  );
}
