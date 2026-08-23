import { useDashboardSummary } from '../hooks/useData';

const STATUS_STYLES: Record<string, string> = {
  OPEN: 'bg-sky-100 text-sky-700 dark:bg-sky-500/15 dark:text-sky-300',
  IN_PROGRESS: 'bg-amber-100 text-amber-700 dark:bg-amber-500/15 dark:text-amber-300',
  RESOLVED: 'bg-emerald-100 text-emerald-700 dark:bg-emerald-500/15 dark:text-emerald-300',
  CLOSED: 'bg-slate-200 text-slate-600 dark:bg-white/10 dark:text-slate-400',
  REOPENED: 'bg-violet-100 text-violet-700 dark:bg-violet-500/15 dark:text-violet-300',
};

function formatSeconds(seconds: number | null): string {
  if (seconds === null) return 'Sin datos';
  if (seconds < 60) return `${Math.round(seconds)} s`;
  const minutes = Math.floor(seconds / 60);
  if (minutes < 60) return `${minutes} min`;
  const hours = Math.floor(minutes / 60);
  return `${hours} h ${minutes % 60} min`;
}

const card =
  'rounded-2xl border border-slate-200/70 bg-white p-4 shadow-sm transition-all duration-200 hover:-translate-y-0.5 hover:border-brand-300 hover:shadow-lg hover:shadow-brand-500/5 sm:p-5 dark:border-white/10 dark:bg-white/[0.03] dark:hover:border-brand-500/40';
const sectionCard =
  'rounded-2xl border border-slate-200/70 bg-white p-5 shadow-sm sm:p-6 dark:border-white/10 dark:bg-white/[0.03]';

export default function MetricsPanel() {
  const { data: summary, error, isLoading, refetch } = useDashboardSummary();

  if (isLoading) {
    return (
      <div className="animate-fade-up grid grid-cols-2 gap-4 md:grid-cols-4">
        {[0, 1, 2, 3].map((i) => (
          <div key={i} className={`${card} animate-pulse`}>
            <div className="mb-3 h-3 w-20 rounded-full bg-slate-200 dark:bg-white/10" />
            <div className="h-8 w-16 rounded-lg bg-slate-200 dark:bg-white/10" />
          </div>
        ))}
      </div>
    );
  }

  if (error) {
    return (
      <div className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 dark:border-red-500/20 dark:bg-red-500/10 dark:text-red-300">
        {error.message}
      </div>
    );
  }

  if (!summary) return null;

  return (
    <div className="animate-fade-up space-y-6">
      <div className="flex justify-end">
        <button
          onClick={() => refetch()}
          className="cursor-pointer rounded-xl border border-slate-200/70 bg-white px-4 py-2 text-xs font-semibold text-slate-600 shadow-sm transition-all duration-200 hover:-translate-y-0.5 hover:border-brand-300 hover:text-brand-600 dark:border-white/10 dark:bg-white/[0.03] dark:text-slate-400 dark:hover:border-brand-500/40 dark:hover:text-brand-300"
        >
          Refrescar
        </button>
      </div>

      {/* KPIs */}
      <div className="grid grid-cols-2 gap-4 md:grid-cols-4">
        <div className={card}>
          <p className="text-[11px] font-semibold uppercase tracking-widest text-slate-400">Tickets totales</p>
          <p className="mt-1.5 font-display text-3xl font-bold tracking-tight text-slate-900 dark:text-white">
            {summary.totalTickets}
          </p>
        </div>
        <div className={card}>
          <p className="text-[11px] font-semibold uppercase tracking-widest text-slate-400">SLA vencidos</p>
          <p
            className={`mt-1.5 font-display text-3xl font-bold tracking-tight ${
              summary.slaBreachedCount > 0 ? 'text-red-500' : 'text-emerald-500'
            }`}
          >
            {summary.slaBreachedCount}
          </p>
        </div>
        <div className={card}>
          <p className="text-[11px] font-semibold uppercase tracking-widest text-slate-400">Resolución promedio</p>
          <p className="mt-1.5 font-display text-xl font-bold tracking-tight text-slate-900 dark:text-white">
            {formatSeconds(summary.avgResolutionSeconds)}
          </p>
        </div>
        <div className={card}>
          <p className="text-[11px] font-semibold uppercase tracking-widest text-slate-400">Primera respuesta</p>
          <p className="mt-1.5 font-display text-xl font-bold tracking-tight text-slate-900 dark:text-white">
            {formatSeconds(summary.avgFirstResponseSeconds)}
          </p>
        </div>
      </div>

      {/* Distribución + agentes */}
      <div className="grid gap-4 md:grid-cols-2">
        <div className={sectionCard}>
          <h3 className="mb-4 font-display text-sm font-bold tracking-tight text-slate-900 dark:text-white">
            Tickets por estado
          </h3>
          <div className="space-y-3.5">
            {Object.entries(summary.ticketsByStatus).map(([status, count]) => {
              const pct = summary.totalTickets > 0 ? Math.round((count / summary.totalTickets) * 100) : 0;
              return (
                <div key={status}>
                  <div className="mb-1 flex items-center justify-between">
                    <span className={`rounded-full px-2 py-0.5 text-[11px] font-semibold ${STATUS_STYLES[status] ?? 'bg-slate-100 text-slate-600 dark:bg-white/10 dark:text-slate-400'}`}>
                      {status}
                    </span>
                    <span className="font-mono text-xs text-slate-500 dark:text-slate-400">
                      {count} ({pct}%)
                    </span>
                  </div>
                  <div className="h-2 overflow-hidden rounded-full bg-slate-100 dark:bg-white/10">
                    <div
                      className="h-full rounded-full bg-gradient-to-r from-brand-500 to-violet-500 transition-all duration-500"
                      style={{ width: `${pct}%` }}
                    />
                  </div>
                </div>
              );
            })}
          </div>
        </div>

        <div className={sectionCard}>
          <h3 className="mb-4 font-display text-sm font-bold tracking-tight text-slate-900 dark:text-white">
            Top agentes
          </h3>
          {summary.topAgents.length === 0 ? (
            <p className="py-6 text-center text-sm text-slate-400">
              Todavía no hay tickets asignados a agentes.
            </p>
          ) : (
            <ul className="divide-y divide-slate-100 dark:divide-white/5">
              {summary.topAgents.map((agent, i) => (
                <li key={agent.agentName} className="flex items-center gap-3 py-2.5">
                  <span
                    className={`grid size-7 place-items-center rounded-full text-xs font-bold ${
                      i === 0
                        ? 'bg-gradient-to-br from-amber-300 to-yellow-500 text-white shadow-md shadow-amber-500/30'
                        : i === 1
                          ? 'bg-gradient-to-br from-slate-300 to-slate-400 text-white'
                          : i === 2
                            ? 'bg-gradient-to-br from-orange-300 to-amber-600 text-white'
                            : 'bg-slate-100 text-slate-500 dark:bg-white/10 dark:text-slate-400'
                    }`}
                  >
                    {i + 1}
                  </span>
                  <span className="flex-1 truncate text-sm text-slate-700 dark:text-slate-300">
                    {agent.agentName}
                  </span>
                  <span className="font-mono text-sm font-semibold text-slate-800 dark:text-slate-100">
                    {agent.assignedTickets}
                  </span>
                </li>
              ))}
            </ul>
          )}
        </div>
      </div>
    </div>
  );
}
