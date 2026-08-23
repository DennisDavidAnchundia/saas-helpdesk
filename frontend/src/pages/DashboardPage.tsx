import { useEffect, useState } from 'react';
import {
  testEndpoint,
  listTickets,
  createTicket,
  changeTicketStatus,
  type Ticket,
  type TicketPriority,
  type TicketStatus,
} from '../services/api';
import AppLayout, { type NavItem } from '../components/layout/AppLayout';
import {
  BookIcon,
  ChartIcon,
  ChatIcon,
  FlaskIcon,
  TicketIcon,
} from '../components/icons';
import ArticlesPanel from '../components/ArticlesPanel';
import ChatPanel from '../components/ChatPanel';
import MetricsPanel from '../components/MetricsPanel';

interface Props {
  email: string;
  role: string;
  tenantName: string;
  token: string;
  onLogout: () => void;
}

type Tab = 'tickets' | 'articles' | 'chat' | 'metrics' | 'tests';

interface TestResult {
  endpoint: string;
  status: number;
  body: any;
}

const NAV_ITEMS: NavItem[] = [
  { id: 'tickets', label: 'Tickets', icon: <TicketIcon /> },
  { id: 'articles', label: 'Base de Conocimiento', icon: <BookIcon /> },
  { id: 'chat', label: 'Chat en Vivo', icon: <ChatIcon /> },
  { id: 'metrics', label: 'Métricas', icon: <ChartIcon /> },
  { id: 'tests', label: 'Pruebas API', icon: <FlaskIcon /> },
];

const ENDPOINTS = [
  { path: '/api/test/admin', label: 'Admin Only', expected: 'ADMIN' },
  { path: '/api/test/agent', label: 'Agent Only', expected: 'AGENT' },
  { path: '/api/test/customer', label: 'Customer Only', expected: 'CUSTOMER' },
  { path: '/api/test/any', label: 'Any Role', expected: 'ANY' },
];

const TRANSITIONS: Record<TicketStatus, TicketStatus[]> = {
  OPEN: ['IN_PROGRESS', 'RESOLVED'],
  IN_PROGRESS: ['RESOLVED'],
  RESOLVED: ['CLOSED', 'REOPENED'],
  CLOSED: ['REOPENED'],
  REOPENED: ['IN_PROGRESS', 'RESOLVED'],
};

const STATUS_STYLES: Record<TicketStatus, string> = {
  OPEN: 'bg-sky-100 text-sky-700 dark:bg-sky-500/15 dark:text-sky-300',
  IN_PROGRESS: 'bg-amber-100 text-amber-700 dark:bg-amber-500/15 dark:text-amber-300',
  RESOLVED: 'bg-emerald-100 text-emerald-700 dark:bg-emerald-500/15 dark:text-emerald-300',
  CLOSED: 'bg-slate-200 text-slate-600 dark:bg-white/10 dark:text-slate-400',
  REOPENED: 'bg-violet-100 text-violet-700 dark:bg-violet-500/15 dark:text-violet-300',
};

const PRIORITY_STYLES: Record<TicketPriority, string> = {
  LOW: 'bg-slate-100 text-slate-600 dark:bg-white/10 dark:text-slate-400',
  MEDIUM: 'bg-yellow-100 text-yellow-700 dark:bg-yellow-500/15 dark:text-yellow-300',
  HIGH: 'bg-orange-100 text-orange-700 dark:bg-orange-500/15 dark:text-orange-300',
  URGENT: 'bg-red-100 text-red-700 dark:bg-red-500/15 dark:text-red-300',
};

export default function DashboardPage({ email, role, tenantName, token, onLogout }: Props) {
  const [tab, setTab] = useState<Tab>('tickets');
  const [results, setResults] = useState<TestResult[]>([]);
  const [testing, setTesting] = useState(false);
  const [tickets, setTickets] = useState<Ticket[]>([]);
  const [ticketsError, setTicketsError] = useState('');
  const [newTitle, setNewTitle] = useState('');
  const [newDesc, setNewDesc] = useState('');
  const [newPriority, setNewPriority] = useState<TicketPriority>('MEDIUM');
  const [creating, setCreating] = useState(false);
  const [actionError, setActionError] = useState('');

  const loadTickets = async () => {
    try {
      setTicketsError('');
      const data = await listTickets(token);
      setTickets(data);
    } catch (err: any) {
      setTicketsError(err.message);
    }
  };

  useEffect(() => {
    loadTickets();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // Badge con la cantidad de tickets abiertos
  const openCount = tickets.filter((t) => t.status === 'OPEN' || t.status === 'REOPENED').length;
  const navItems = NAV_ITEMS.map((item) =>
    item.id === 'tickets' && openCount > 0 ? { ...item, badge: String(openCount) } : item,
  );

  const handleCreateTicket = async (e: React.FormEvent) => {
    e.preventDefault();
    setActionError('');
    setCreating(true);
    try {
      await createTicket(token, { title: newTitle, description: newDesc, priority: newPriority });
      setNewTitle('');
      setNewDesc('');
      setNewPriority('MEDIUM');
      await loadTickets();
    } catch (err: any) {
      setActionError(err.message);
    } finally {
      setCreating(false);
    }
  };

  const handleStatusChange = async (id: number, status: TicketStatus) => {
    setActionError('');
    try {
      await changeTicketStatus(token, id, status);
      await loadTickets();
    } catch (err: any) {
      setActionError(err.message);
    }
  };

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

  const errorBox =
    'animate-fade-up rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 dark:border-red-500/20 dark:bg-red-500/10 dark:text-red-300';

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
        {tab === 'tickets' && (
          <>
            {/* Formulario de creación */}
            <section className="rounded-2xl border border-slate-200/70 bg-white p-5 shadow-sm sm:p-6 dark:border-white/10 dark:bg-white/[0.03]">
              <h2 className="mb-4 font-display text-base font-bold tracking-tight text-slate-900 dark:text-white">
                Nuevo ticket
              </h2>
              <form onSubmit={handleCreateTicket} className="space-y-3">
                <input
                  type="text"
                  value={newTitle}
                  onChange={(e) => setNewTitle(e.target.value)}
                  placeholder="Título del ticket"
                  className="w-full rounded-xl border border-slate-300 bg-white px-3.5 py-2.5 text-sm outline-none transition-shadow placeholder:text-slate-400 focus:border-brand-400 focus:ring-4 focus:ring-brand-500/15 dark:border-white/10 dark:bg-white/5 dark:text-white"
                  required
                  maxLength={255}
                />
                <textarea
                  value={newDesc}
                  onChange={(e) => setNewDesc(e.target.value)}
                  placeholder="Descripción del problema"
                  className="w-full resize-none rounded-xl border border-slate-300 bg-white px-3.5 py-2.5 text-sm outline-none transition-shadow placeholder:text-slate-400 focus:border-brand-400 focus:ring-4 focus:ring-brand-500/15 dark:border-white/10 dark:bg-white/5 dark:text-white"
                  rows={2}
                  required
                />
                <div className="flex items-center gap-3">
                  <select
                    value={newPriority}
                    onChange={(e) => setNewPriority(e.target.value as TicketPriority)}
                    className="cursor-pointer rounded-xl border border-slate-300 bg-white px-3 py-2.5 text-sm outline-none focus:border-brand-400 dark:border-white/10 dark:bg-white/5 dark:text-white"
                  >
                    <option value="LOW">LOW</option>
                    <option value="MEDIUM">MEDIUM</option>
                    <option value="HIGH">HIGH</option>
                    <option value="URGENT">URGENT</option>
                  </select>
                  <button
                    type="submit"
                    disabled={creating}
                    className="cursor-pointer rounded-xl bg-gradient-to-r from-brand-500 to-violet-500 px-5 py-2.5 text-sm font-semibold text-white shadow-lg shadow-brand-500/25 transition-all duration-200 hover:-translate-y-0.5 hover:shadow-xl hover:shadow-brand-500/30 disabled:cursor-not-allowed disabled:opacity-60 disabled:hover:translate-y-0"
                  >
                    {creating ? 'Creando…' : '+ Crear ticket'}
                  </button>
                </div>
              </form>

              {(actionError || ticketsError) && (
                <div className={`${errorBox} mt-4`}>
                  {actionError || ticketsError}
                </div>
              )}
            </section>

            {/* Lista de tickets */}
            <section className="space-y-3">
              <h2 className="font-display text-base font-bold tracking-tight text-slate-900 dark:text-white">
                Tickets ({tickets.length})
              </h2>
              {tickets.map((t) => (
                <article
                  key={t.id}
                  className="group rounded-2xl border border-slate-200/70 bg-white p-4 shadow-sm transition-all duration-200 hover:-translate-y-0.5 hover:border-brand-300 hover:shadow-lg hover:shadow-brand-500/5 sm:p-5 dark:border-white/10 dark:bg-white/[0.03] dark:hover:border-brand-500/40"
                >
                  <div className="flex flex-wrap items-center justify-between gap-2">
                    <div className="flex flex-wrap items-center gap-2">
                      <span className="font-mono text-xs font-semibold text-brand-500 dark:text-brand-400">
                        #{t.id}
                      </span>
                      <span className="text-sm font-semibold text-slate-800 dark:text-slate-100">
                        {t.title}
                      </span>
                      <span className={`rounded-full px-2 py-0.5 text-[11px] font-semibold ${STATUS_STYLES[t.status]}`}>
                        {t.status}
                      </span>
                      <span className={`rounded-full px-2 py-0.5 text-[11px] font-semibold ${PRIORITY_STYLES[t.priority]}`}>
                        {t.priority}
                      </span>
                      {t.slaBreached && (
                        <span className="animate-pulse-dot rounded-full bg-red-600 px-2 py-0.5 text-[11px] font-bold text-white">
                          SLA VENCIDO
                        </span>
                      )}
                    </div>
                    <div className="flex gap-1.5">
                      {(TRANSITIONS[t.status] || []).map((next) => (
                        <button
                          key={next}
                          onClick={() => handleStatusChange(t.id, next)}
                          className="cursor-pointer rounded-lg border border-slate-300 px-2.5 py-1 text-[11px] font-medium text-slate-600 transition-colors hover:border-brand-400 hover:bg-brand-50 hover:text-brand-600 dark:border-white/10 dark:text-slate-400 dark:hover:border-brand-500/40 dark:hover:bg-brand-500/10 dark:hover:text-brand-300"
                        >
                          {next}
                        </button>
                      ))}
                    </div>
                  </div>
                  <p className="mt-2 text-sm text-slate-600 dark:text-slate-400">{t.description}</p>
                  <p className="mt-1.5 text-xs text-slate-400">
                    Cliente: {t.customerName}
                    {' · '}Agente: {t.agentName || 'sin asignar'}
                    {t.slaDueAt && ` · SLA vence: ${new Date(t.slaDueAt).toLocaleString('es')}`}
                    {t.firstResponseAt && ` · 1ra respuesta: ${new Date(t.firstResponseAt).toLocaleTimeString('es')}`}
                  </p>
                </article>
              ))}
              {tickets.length === 0 && !ticketsError && (
                <p className="py-8 text-center text-sm text-slate-400">
                  No hay tickets todavía. Crea el primero arriba.
                </p>
              )}
            </section>
          </>
        )}

        {tab === 'articles' && <ArticlesPanel token={token} role={role} />}
        {tab === 'chat' && <ChatPanel token={token} tickets={tickets} />}
        {tab === 'metrics' && <MetricsPanel token={token} />}

        {/* ===================== PRUEBAS API ===================== */}
        {tab === 'tests' && (
          <section className="space-y-4">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <div>
                <h2 className="font-display text-base font-bold tracking-tight text-slate-900 dark:text-white">
                  Acceso por roles
                </h2>
                <p className="text-xs text-slate-400">
                  Verifica qué endpoints responde tu rol · Sesión: {email} ({role}) · {tenantName}
                </p>
              </div>
              <button
                onClick={runAllTests}
                disabled={testing}
                className="cursor-pointer rounded-xl bg-gradient-to-r from-brand-500 to-violet-500 px-5 py-2.5 text-sm font-semibold text-white shadow-lg shadow-brand-500/25 transition-all duration-200 hover:-translate-y-0.5 disabled:opacity-60"
              >
                {testing ? 'Probando…' : 'Probar todos'}
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
