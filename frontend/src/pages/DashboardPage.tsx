import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { testEndpoint } from '../services/api';
import { useTickets } from '../hooks/useData';
import AppLayout from '../components/layout/AppLayout';
import type { NavItem } from '../components/layout/AppLayout';
import {
  BookIcon,
  ChartIcon,
  ChatIcon,
  CreditCardIcon,
  FlaskIcon,
  TicketIcon,
  UsersIcon,
} from '../components/icons';
import AdminPanel from '../components/AdminPanel';
import ArticlesPanel from '../components/ArticlesPanel';
import BillingPanel from '../components/BillingPanel';
import ChatPanel from '../components/ChatPanel';
import CustomerPortal from '../components/CustomerPortal';
import MetricsPanel from '../components/MetricsPanel';
import TicketsSection from '../components/TicketsSection';

interface Props {
  email: string;
  role: string;
  tenantName: string;
  token: string;
  onLogout: () => void;
}

type Tab = 'tickets' | 'articles' | 'chat' | 'metrics' | 'billing' | 'users' | 'tests';

interface TestResult {
  endpoint: string;
  status: number;
  body: any;
}

export default function DashboardPage({ email, role, tenantName, token, onLogout }: Props) {
  const { t } = useTranslation();
  const [tab, setTab] = useState<Tab>('tickets');
  const [results, setResults] = useState<TestResult[]>([]);
  const [testing, setTesting] = useState(false);
  // Ticket que se quiere ver en el chat (viene del boton "Abrir chat" del detalle)
  const [chatFocusId, setChatFocusId] = useState<number | null>(null);

  const handleOpenChat = (ticketId: number) => {
    setChatFocusId(ticketId);
    setTab('chat');
  };

  // Lista sin filtros para el selector de chat y el badge del sidebar
  const ticketsQuery = useTickets();
  const tickets = ticketsQuery.data?.content ?? [];

  const baseNavItems: NavItem[] =
    role === 'CUSTOMER'
      ? [
          { id: 'tickets', label: t('nav.myTickets'), icon: <TicketIcon /> },
          { id: 'articles', label: t('nav.articles'), icon: <BookIcon /> },
          { id: 'chat', label: t('nav.chat'), icon: <ChatIcon /> },
        ]
      : [
          { id: 'tickets', label: t('nav.tickets'), icon: <TicketIcon /> },
          { id: 'articles', label: t('nav.articles'), icon: <BookIcon /> },
          { id: 'chat', label: t('nav.chat'), icon: <ChatIcon /> },
          { id: 'metrics', label: t('nav.metrics'), icon: <ChartIcon /> },
          ...(role === 'ADMIN'
            ? [
                { id: 'billing', label: t('nav.billing'), icon: <CreditCardIcon /> },
                { id: 'users', label: t('nav.users'), icon: <UsersIcon /> },
              ]
            : []),
          { id: 'tests', label: t('nav.tests'), icon: <FlaskIcon /> },
        ];

  // Badge con la cantidad de tickets abiertos
  const openCount = tickets.filter((t) => t.status === 'OPEN' || t.status === 'REOPENED').length;
  const navItems = baseNavItems.map((item) =>
    item.id === 'tickets' && openCount > 0 ? { ...item, badge: String(openCount) } : item,
  );

  const ENDPOINTS = [
    { path: '/api/test/admin', label: t('tests.adminOnly') },
    { path: '/api/test/agent', label: t('tests.agentOnly') },
    { path: '/api/test/customer', label: t('tests.customerOnly') },
    { path: '/api/test/any', label: t('tests.anyRole') },
  ];

  const runAllTests = async () => {
    setTesting(true);
    setResults([]);
    const newResults: TestResult[] = [];
    for (const ep of ENDPOINTS) {
      const { status, data } = await testEndpoint(ep.path, token);
      newResults.push({ endpoint: `${ep.label} (${ep.path})`, status, body: data });
    }
    setResults(newResults);
    setTesting(false);
  };

  const runSingleTest = async (path: string, label: string) => {
    const { status, data } = await testEndpoint(path, token);
    setResults((prev) => {
      const filtered = prev.filter((r) => r.endpoint !== `${label} (${path})`);
      return [...filtered, { endpoint: `${label} (${path})`, status, body: data }];
    });
  };

  const statusColor = (code: number) => {
    if (code === 200) return 'border-emerald-200 bg-emerald-50 text-emerald-700 dark:border-emerald-500/20 dark:bg-emerald-500/10 dark:text-emerald-300';
    if (code === 403) return 'border-amber-200 bg-amber-50 text-amber-700 dark:border-amber-500/20 dark:bg-amber-500/10 dark:text-amber-300';
    if (code === 401) return 'border-red-200 bg-red-50 text-red-700 dark:border-red-500/20 dark:bg-red-500/10 dark:text-red-300';
    return 'border-slate-200 bg-slate-50 text-slate-600 dark:border-white/10 dark:bg-white/5 dark:text-slate-400';
  };

  return (
    <AppLayout
      items={navItems}
      activeId={tab}
      onNavigate={(id) => setTab(id as Tab)}
      user={{ email, role, tenantName }}
      onLogout={onLogout}
    >
      <div className="animate-fade-up space-y-6">
        {/* ===================== TICKETS ===================== */}
        {tab === 'tickets' && (role === 'CUSTOMER' ? <CustomerPortal onOpenChat={handleOpenChat} /> : <TicketsSection role={role} onOpenChat={handleOpenChat} />)}

        {tab === 'articles' && <ArticlesPanel role={role} />}
        {tab === 'chat' && <ChatPanel token={token} tickets={tickets} focusTicketId={chatFocusId} />}
        {tab === 'metrics' && <MetricsPanel />}
        {tab === 'billing' && role === 'ADMIN' && <BillingPanel />}
        {tab === 'users' && role === 'ADMIN' && <AdminPanel />}

        {/* ===================== PRUEBAS API ===================== */}
        {tab === 'tests' && (
          <section className="space-y-4">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <div>
                <h2 className="font-display text-base font-bold tracking-tight text-slate-900 dark:text-white">
                  {t('tests.title')}
                </h2>
                <p className="text-xs text-slate-400">
                  {t('tests.subtitle', { email, role, tenant: tenantName })}
                </p>
              </div>
              <button
                onClick={runAllTests}
                disabled={testing}
                className="cursor-pointer rounded-xl bg-gradient-to-r from-brand-500 to-violet-500 px-5 py-2.5 text-sm font-semibold text-white shadow-lg shadow-brand-500/25 transition-all duration-200 hover:-translate-y-0.5 disabled:opacity-60"
              >
                {testing ? t('tests.testing') : t('tests.runAll')}
              </button>
            </div>

            <div className="grid grid-cols-2 gap-3 md:grid-cols-4">
              {ENDPOINTS.map((ep) => (
                <button
                  key={ep.path}
                  onClick={() => runSingleTest(ep.path, ep.label)}
                  disabled={testing}
                  className="cursor-pointer rounded-xl border border-slate-200/70 bg-white px-3 py-2.5 text-sm font-medium text-slate-600 shadow-sm transition-all duration-200 hover:-translate-y-0.5 hover:border-brand-300 hover:text-brand-600 dark:border-white/10 dark:bg-white/[0.03] dark:text-slate-400 dark:hover:border-brand-500/40 dark:hover:text-brand-300"
                >
                  {ep.label}
                </button>
              ))}
            </div>

            {results.length > 0 && (
              <div className="space-y-3">
                {results.map((r, i) => (
                  <div key={i} className={`animate-fade-up rounded-2xl border p-4 ${statusColor(r.status)}`}>
                    <div className="mb-2 flex items-center justify-between">
                      <span className="font-mono text-sm font-semibold">{r.endpoint}</span>
                      <span className="font-display text-lg font-bold">{r.status}</span>
                    </div>
                    <pre className="max-h-32 overflow-auto font-mono text-xs opacity-75">
                      {JSON.stringify(r.body, null, 2)}
                    </pre>
                  </div>
                ))}
              </div>
            )}

            <details className="rounded-2xl border border-slate-200/70 bg-white p-4 text-sm shadow-sm dark:border-white/10 dark:bg-white/[0.03]">
              <summary className="cursor-pointer font-medium text-slate-500 dark:text-slate-400">
                Token JWT (debug)
              </summary>
              <p className="mt-3 break-all font-mono text-xs text-slate-500 dark:text-slate-500">
                {token.substring(0, 50)}…{token.substring(token.length - 20)}
              </p>
            </details>
          </section>
        )}
      </div>
    </AppLayout>
  );
}
