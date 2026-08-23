import { useCallback, useEffect, useState } from 'react';
import { getDashboardSummary, type DashboardSummary } from '../services/api';

const STATUS_STYLES: Record<string, string> = {
  OPEN: 'bg-blue-100 text-blue-700',
  IN_PROGRESS: 'bg-amber-100 text-amber-700',
  RESOLVED: 'bg-green-100 text-green-700',
  CLOSED: 'bg-slate-100 text-slate-600',
  REOPENED: 'bg-red-100 text-red-700',
};

function formatSeconds(seconds: number | null): string {
  if (seconds === null) return 'Sin datos';
  if (seconds < 60) return `${Math.round(seconds)} s`;
  const minutes = Math.floor(seconds / 60);
  if (minutes < 60) return `${minutes} min`;
  const hours = Math.floor(minutes / 60);
  return `${hours} h ${minutes % 60} min`;
}

export default function MetricsPanel({ token }: { token: string }) {
  const [summary, setSummary] = useState<DashboardSummary | null>(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    try {
      setError('');
      setSummary(await getDashboardSummary(token));
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Error al cargar métricas');
    } finally {
      setLoading(false);
    }
  }, [token]);

  useEffect(() => {
    load();
  }, [load]);

  if (loading) {
    return <div className="text-sm text-slate-500 p-6">Cargando métricas...</div>;
  }

  if (error) {
    return (
      <div className="bg-white rounded-xl shadow-lg border border-slate-200 p-6">
        <p className="text-sm text-red-600">{error}</p>
      </div>
    );
  }

  if (!summary) return null;

  return (
    <div className="space-y-4">
      <div className="flex justify-end">
        <button
          onClick={() => { setLoading(true); load(); }}
          className="px-3 py-1.5 text-xs font-medium bg-white border border-slate-200 rounded-lg hover:bg-slate-50"
        >
          Refrescar
        </button>
      </div>

      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <div className="bg-white rounded-xl shadow border border-slate-200 p-4">
          <p className="text-xs uppercase tracking-wide text-slate-400">Tickets totales</p>
          <p className="text-3xl font-bold text-slate-800">{summary.totalTickets}</p>
        </div>
        <div className="bg-white rounded-xl shadow border border-slate-200 p-4">
          <p className="text-xs uppercase tracking-wide text-slate-400">SLA vencidos</p>
          <p className={`text-3xl font-bold ${summary.slaBreachedCount > 0 ? 'text-red-600' : 'text-green-600'}`}>
            {summary.slaBreachedCount}
          </p>
        </div>
        <div className="bg-white rounded-xl shadow border border-slate-200 p-4">
          <p className="text-xs uppercase tracking-wide text-slate-400">Resolución promedio</p>
          <p className="text-xl font-bold text-slate-800 mt-2">{formatSeconds(summary.avgResolutionSeconds)}</p>
        </div>
        <div className="bg-white rounded-xl shadow border border-slate-200 p-4">
          <p className="text-xs uppercase tracking-wide text-slate-400">Primera respuesta</p>
          <p className="text-xl font-bold text-slate-800 mt-2">{formatSeconds(summary.avgFirstResponseSeconds)}</p>
        </div>
      </div>

      <div className="grid md:grid-cols-2 gap-4">
        <div className="bg-white rounded-xl shadow-lg border border-slate-200 p-6">
          <h3 className="text-sm font-semibold text-slate-700 mb-3">Tickets por estado</h3>
          <div className="space-y-2">
            {Object.entries(summary.ticketsByStatus).map(([status, count]) => {
              const pct = summary.totalTickets > 0 ? Math.round((count / summary.totalTickets) * 100) : 0;
              return (
                <div key={status}>
                  <div className="flex justify-between items-center mb-1">
                    <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${STATUS_STYLES[status] ?? 'bg-slate-100 text-slate-600'}`}>
                      {status}
                    </span>
                    <span className="text-sm text-slate-600">{count} ({pct}%)</span>
                  </div>
                  <div className="h-2 bg-slate-100 rounded-full overflow-hidden">
                    <div
                      className="h-full bg-blue-500 rounded-full transition-all"
                      style={{ width: `${pct}%` }}
                    />
                  </div>
                </div>
              );
            })}
          </div>
        </div>

        <div className="bg-white rounded-xl shadow-lg border border-slate-200 p-6">
          <h3 className="text-sm font-semibold text-slate-700 mb-3">Top agentes (por tickets asignados)</h3>
          {summary.topAgents.length === 0 ? (
            <p className="text-sm text-slate-400">Todavia no hay tickets asignados a agentes.</p>
          ) : (
            <ul className="divide-y divide-slate-100">
              {summary.topAgents.map((agent, i) => (
                <li key={agent.agentName} className="flex items-center gap-3 py-2">
                  <span className={`w-6 h-6 flex items-center justify-center rounded-full text-xs font-bold ${
                    i === 0 ? 'bg-yellow-100 text-yellow-700' : 'bg-slate-100 text-slate-500'
                  }`}>
                    {i + 1}
                  </span>
                  <span className="flex-1 text-sm text-slate-700">{agent.agentName}</span>
                  <span className="text-sm font-semibold text-slate-800">{agent.assignedTickets}</span>
                </li>
              ))}
            </ul>
          )}
        </div>
      </div>
    </div>
  );
}
