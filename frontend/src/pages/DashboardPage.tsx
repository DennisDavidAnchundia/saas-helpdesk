import { useEffect, useState } from 'react';
import { testEndpoint, listTickets, createTicket, changeTicketStatus, type Ticket, type TicketPriority, type TicketStatus } from '../services/api';
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
  OPEN: 'bg-blue-100 text-blue-700',
  IN_PROGRESS: 'bg-amber-100 text-amber-700',
  RESOLVED: 'bg-green-100 text-green-700',
  CLOSED: 'bg-slate-200 text-slate-600',
  REOPENED: 'bg-purple-100 text-purple-700',
};

const PRIORITY_STYLES: Record<TicketPriority, string> = {
  LOW: 'bg-slate-100 text-slate-600',
  MEDIUM: 'bg-yellow-100 text-yellow-700',
  HIGH: 'bg-orange-100 text-orange-700',
  URGENT: 'bg-red-100 text-red-700',
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
    if (code === 200) return 'text-green-600 bg-green-50 border-green-200';
    if (code === 403) return 'text-amber-600 bg-amber-50 border-amber-200';
    if (code === 401) return 'text-red-600 bg-red-50 border-red-200';
    return 'text-slate-600 bg-slate-50 border-slate-200';
  };

  return (
    <div className="min-h-screen p-4 md:p-8">
      <div className="max-w-4xl mx-auto space-y-6">
        {/* Header */}
        <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
          <div>
            <h1 className="text-2xl font-bold text-slate-800">Dashboard</h1>
            <p className="text-slate-500 text-sm">SaaS Help Desk</p>
          </div>
          <button
            onClick={onLogout}
            className="px-4 py-2 text-sm font-medium text-red-600 border border-red-200 rounded-lg hover:bg-red-50 transition-colors"
          >
            Cerrar sesion
          </button>
        </div>

        {/* Tabs */}
        <div className="flex flex-wrap gap-2">
          {(
            [
              ['tickets', 'Tickets'],
              ['articles', 'Base de Conocimiento'],
              ['chat', 'Chat'],
              ['metrics', 'Métricas'],
              ['tests', 'Pruebas API'],
            ] as [Tab, string][]
          ).map(([key, label]) => (
            <button
              key={key}
              onClick={() => setTab(key)}
              className={`px-4 py-2 text-sm font-medium rounded-lg transition-colors ${
                tab === key
                  ? 'bg-blue-600 text-white'
                  : 'bg-white text-slate-600 border border-slate-200 hover:bg-slate-50'
              }`}
            >
              {label}
            </button>
          ))}
        </div>

        {tab === 'tickets' && (
        <div className="bg-white rounded-xl shadow-lg border border-slate-200 p-6">
          <h2 className="text-lg font-semibold text-slate-800 mb-4">Tickets ({tickets.length})</h2>

          <form onSubmit={handleCreateTicket} className="space-y-3 mb-6">
            <input
              type="text"
              value={newTitle}
              onChange={(e) => setNewTitle(e.target.value)}
              placeholder="Titulo del ticket"
              className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-blue-500 outline-none text-sm"
              required
              maxLength={255}
            />
            <textarea
              value={newDesc}
              onChange={(e) => setNewDesc(e.target.value)}
              placeholder="Descripcion del problema"
              className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-blue-500 outline-none text-sm"
              rows={2}
              required
            />
            <div className="flex gap-3 items-center">
              <select
                value={newPriority}
                onChange={(e) => setNewPriority(e.target.value as TicketPriority)}
                className="px-3 py-2 border border-slate-300 rounded-lg text-sm bg-white"
              >
                <option value="LOW">LOW</option>
                <option value="MEDIUM">MEDIUM</option>
                <option value="HIGH">HIGH</option>
                <option value="URGENT">URGENT</option>
              </select>
              <button
                type="submit"
                disabled={creating}
                className="px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-lg hover:bg-blue-700 transition-colors disabled:opacity-50"
              >
                {creating ? 'Creando...' : '+ Crear ticket'}
              </button>
            </div>
          </form>

          {actionError && (
            <div className="bg-red-50 text-red-700 text-sm p-3 rounded-lg border border-red-200 mb-4">
              {actionError}
            </div>
          )}
          {ticketsError && (
            <div className="bg-red-50 text-red-700 text-sm p-3 rounded-lg border border-red-200 mb-4">
              {ticketsError}
            </div>
          )}

          <div className="space-y-3">
            {tickets.map((t) => (
              <div key={t.id} className="border border-slate-200 rounded-lg p-4">
                <div className="flex flex-wrap items-center gap-2 justify-between">
                  <div className="flex flex-wrap items-center gap-2">
                    <span className="font-semibold text-slate-800 text-sm">#{t.id}</span>
                    <span className="text-sm font-medium text-slate-800">{t.title}</span>
                    <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${STATUS_STYLES[t.status]}`}>
                      {t.status}
                    </span>
                    <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${PRIORITY_STYLES[t.priority]}`}>
                      {t.priority}
                    </span>
                    {t.slaBreached && (
                      <span className="text-xs px-2 py-0.5 rounded-full font-bold bg-red-600 text-white animate-pulse">
                        SLA VENCIDO
                      </span>
                    )}
                  </div>
                  <div className="flex gap-1.5">
                    {(TRANSITIONS[t.status] || []).map((next) => (
                      <button
                        key={next}
                        onClick={() => handleStatusChange(t.id, next)}
                        className="text-xs px-2.5 py-1 border border-slate-300 rounded-md hover:bg-slate-50 transition-colors"
                      >
                        {next}
                      </button>
                    ))}
                  </div>
                </div>
                <p className="text-xs text-slate-500 mt-2">{t.description}</p>
                <p className="text-xs text-slate-400 mt-1">
                  Cliente: {t.customerName}
                  {' · '}Agente: {t.agentName || 'sin asignar'}
                  {t.slaDueAt && ` · SLA vence: ${new Date(t.slaDueAt).toLocaleString('es')}`}
                  {t.firstResponseAt && ` · 1ra respuesta: ${new Date(t.firstResponseAt).toLocaleTimeString('es')}`}
                </p>
              </div>
            ))}
            {tickets.length === 0 && !ticketsError && (
              <p className="text-sm text-slate-400 text-center py-4">
                No hay tickets todavia. Crea el primero arriba.
              </p>
            )}
          </div>
        </div>
        )}

        {tab === 'articles' && <ArticlesPanel token={token} role={role} />}
        {tab === 'chat' && <ChatPanel token={token} tickets={tickets} />}
        {tab === 'metrics' && <MetricsPanel token={token} />}

        {/* User Info Card */}
        <div className="bg-white rounded-xl shadow-lg border border-slate-200 p-6">
          <h2 className="text-lg font-semibold text-slate-800 mb-4">Informacion del Usuario</h2>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <InfoRow label="Email" value={email} />
            <InfoRow label="Rol" value={role} />
            <InfoRow label="Empresa" value={tenantName} />
          </div>
          <div className="mt-4">
            <label className="block text-xs font-medium text-slate-400 mb-1">Token JWT (truncado)</label>
            <div className="bg-slate-50 border border-slate-200 rounded-lg p-3 text-xs font-mono text-slate-600 break-all">
              {token.substring(0, 50)}...{token.substring(token.length - 20)}
            </div>
          </div>
        </div>

        {/* Test Panel */}
        {tab === 'tests' && (
        <div className="bg-white rounded-xl shadow-lg border border-slate-200 p-6">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-lg font-semibold text-slate-800">Panel de Pruebas - Acceso por Roles</h2>
            <button
              onClick={runAllTests}
              disabled={testing}
              className="px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-lg hover:bg-blue-700 transition-colors disabled:opacity-50"
            >
              {testing ? 'Probando...' : 'Probar Todos'}
            </button>
          </div>

          <div className="grid grid-cols-2 md:grid-cols-4 gap-3 mb-6">
            {ENDPOINTS.map((ep) => (
              <button
                key={ep.path}
                onClick={() => runSingleTest(ep.path, ep.label)}
                disabled={testing}
                className="px-3 py-2 text-sm font-medium border border-slate-200 rounded-lg hover:bg-slate-50 transition-colors disabled:opacity-50"
              >
                {ep.label}
              </button>
            ))}
          </div>

          {results.length > 0 && (
            <div className="space-y-3">
              <h3 className="text-sm font-medium text-slate-400">Resultados:</h3>
              {results.map((r, i) => (
                <div key={i} className={`border rounded-lg p-4 ${statusColor(r.status)}`}>
                  <div className="flex items-center justify-between mb-2">
                    <span className="font-mono text-sm font-semibold">{r.endpoint}</span>
                    <span className="text-lg font-bold">{r.status}</span>
                  </div>
                  <pre className="text-xs font-mono opacity-75 overflow-auto max-h-32">
                    {JSON.stringify(r.body, null, 2)}
                  </pre>
                </div>
              ))}
            </div>
          )}
        </div>
        )}
      </div>
    </div>
  );
}

function InfoRow({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <span className="text-xs font-medium text-slate-400">{label}</span>
      <p className="text-sm font-semibold text-slate-700">{value}</p>
    </div>
  );
}
