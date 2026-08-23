import { useState } from 'react';
import type { ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
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
    </div>
  );
}
