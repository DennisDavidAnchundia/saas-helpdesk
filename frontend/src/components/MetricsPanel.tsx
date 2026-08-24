import { useTranslation } from 'react-i18next';
import {
  Area,
  AreaChart,
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Legend,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import { useDashboardSummary, useDashboardTrend } from '../hooks/useData';

/** Colores hex de las graficas (Recharts no usa Tailwind). */
const STATUS_CHART_COLORS: Record<string, string> = {
  OPEN: '#0ea5e9',
  IN_PROGRESS: '#f59e0b',
  RESOLVED: '#10b981',
  CLOSED: '#94a3b8',
  REOPENED: '#8b5cf6',
};

const card =
  'rounded-2xl border border-slate-200/70 bg-white p-4 shadow-sm transition-all duration-200 hover:-translate-y-0.5 hover:border-brand-300 hover:shadow-lg hover:shadow-brand-500/5 sm:p-5 dark:border-white/10 dark:bg-white/[0.03] dark:hover:border-brand-500/40';
const sectionCard =
  'rounded-2xl border border-slate-200/70 bg-white p-5 shadow-sm sm:p-6 dark:border-white/10 dark:bg-white/[0.03]';

export default function MetricsPanel() {
  const { t } = useTranslation();
  const summaryQuery = useDashboardSummary();
  const trendQuery = useDashboardTrend();

  if (summaryQuery.isLoading) {
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

  if (summaryQuery.error) {
    return (
      <div className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 dark:border-red-500/20 dark:bg-red-500/10 dark:text-red-300">
        {summaryQuery.error.message}
      </div>
    );
  }

  const summary = summaryQuery.data;
  if (!summary) return null;

  const pieData = Object.entries(summary.ticketsByStatus)
    .filter(([, count]) => count > 0)
    .map(([status, count]) => ({ name: status, value: count }));

  const trendData = (trendQuery.data ?? []).map((p) => ({
    ...p,
    label: new Date(`${p.date}T12:00:00Z`).toLocaleDateString(undefined, { day: 'numeric', month: 'short' }),
  }));

  const tooltipStyle = {
    backgroundColor: 'var(--color-surface, #fff)',
    border: '1px solid rgba(100,116,139,0.25)',
    borderRadius: 12,
    fontSize: 12,
  };

  return (
    <div className="animate-fade-up space-y-6">
      <div className="flex justify-end">
        <button
          onClick={() => {
            summaryQuery.refetch();
            trendQuery.refetch();
          }}
          className="cursor-pointer rounded-xl border border-slate-200/70 bg-white px-4 py-2 text-xs font-semibold text-slate-600 shadow-sm transition-all duration-200 hover:-translate-y-0.5 hover:border-brand-300 hover:text-brand-600 dark:border-white/10 dark:bg-white/[0.03] dark:text-slate-400 dark:hover:border-brand-500/40 dark:hover:text-brand-300"
        >
          {t('metrics.refresh')}
        </button>
      </div>

      {/* KPIs */}
      <div className="grid grid-cols-2 gap-4 md:grid-cols-4">
        <div className={card}>
          <p className="text-[11px] font-semibold uppercase tracking-widest text-slate-400">{t('metrics.totalTickets')}</p>
          <p className="mt-1.5 font-display text-3xl font-bold tracking-tight text-slate-900 dark:text-white">
            {summary.totalTickets}
          </p>
        </div>
        <div className={card}>
          <p className="text-[11px] font-semibold uppercase tracking-widest text-slate-400">{t('metrics.slaBreached')}</p>
          <p
            className={`mt-1.5 font-display text-3xl font-bold tracking-tight ${
              summary.slaBreachedCount > 0 ? 'text-red-500' : 'text-emerald-500'
            }`}
          >
            {summary.slaBreachedCount}
          </p>
        </div>
        <div className={card}>
          <p className="text-[11px] font-semibold uppercase tracking-widest text-slate-400">{t('metrics.avgResolution')}</p>
          <p className="mt-1.5 font-display text-xl font-bold tracking-tight text-slate-900 dark:text-white">
            {formatSeconds(summary.avgResolutionSeconds, t('metrics.noData'))}
          </p>
        </div>
        <div className={card}>
          <p className="text-[11px] font-semibold uppercase tracking-widest text-slate-400">{t('metrics.firstResponse')}</p>
          <p className="mt-1.5 font-display text-xl font-bold tracking-tight text-slate-900 dark:text-white">
            {formatSeconds(summary.avgFirstResponseSeconds, t('metrics.noData'))}
          </p>
        </div>
      </div>

      {/* Graficas: torta por estado + tendencia diaria */}
      <div className="grid gap-4 lg:grid-cols-2">
        <div className={`${sectionCard} min-h-[300px]`}>
          <h3 className="mb-2 font-display text-sm font-bold tracking-tight text-slate-900 dark:text-white">
            {t('metrics.byStatus')}
          </h3>
          {pieData.length === 0 ? (
            <p className="grid h-[240px] place-items-center text-sm text-slate-400">{t('tickets.empty')}</p>
          ) : (
            <ResponsiveContainer width="100%" height={260}>
              <PieChart>
                <Pie data={pieData} dataKey="value" nameKey="name" innerRadius={55} outerRadius={90} paddingAngle={3}>
                  {pieData.map((entry) => (
                    <Cell key={entry.name} fill={STATUS_CHART_COLORS[entry.name] ?? '#64748b'} stroke="none" />
                  ))}
                </Pie>
                <Tooltip contentStyle={tooltipStyle} />
                <Legend
                  iconType="circle"
                  iconSize={8}
                  formatter={(value) => (
                    <span className="text-xs text-slate-500 dark:text-slate-400">{value}</span>
                  )}
                />
              </PieChart>
            </ResponsiveContainer>
          )}
        </div>

        <div className={`${sectionCard} min-h-[300px]`}>
          <h3 className="mb-2 font-display text-sm font-bold tracking-tight text-slate-900 dark:text-white">
            {t('metrics.trendTitle')}
          </h3>
          <ResponsiveContainer width="100%" height={260}>
            <AreaChart data={trendData} margin={{ top: 10, right: 10, left: -18, bottom: 0 }}>
              <defs>
                <linearGradient id="gradCreated" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stopColor="#8b5cf6" stopOpacity={0.35} />
                  <stop offset="100%" stopColor="#8b5cf6" stopOpacity={0} />
                </linearGradient>
                <linearGradient id="gradResolved" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stopColor="#10b981" stopOpacity={0.35} />
                  <stop offset="100%" stopColor="#10b981" stopOpacity={0} />
                </linearGradient>
              </defs>
              <CartesianGrid strokeDasharray="3 3" stroke="rgba(100,116,139,0.2)" vertical={false} />
              <XAxis dataKey="label" tick={{ fontSize: 10 }} tickLine={false} axisLine={false} interval="preserveStartEnd" />
              <YAxis tick={{ fontSize: 10 }} tickLine={false} axisLine={false} allowDecimals={false} />
              <Tooltip contentStyle={tooltipStyle} />
              <Area type="monotone" dataKey="created" name={t('metrics.created')} stroke="#8b5cf6" strokeWidth={2} fill="url(#gradCreated)" />
              <Area type="monotone" dataKey="resolved" name={t('metrics.resolved')} stroke="#10b981" strokeWidth={2} fill="url(#gradResolved)" />
            </AreaChart>
          </ResponsiveContainer>
        </div>
      </div>

      {/* Carga por agente */}
      <div className={sectionCard}>
        <h3 className="mb-2 font-display text-sm font-bold tracking-tight text-slate-900 dark:text-white">
          {t('metrics.topAgents')}
        </h3>
        {summary.topAgents.length === 0 ? (
          <p className="py-6 text-center text-sm text-slate-400">{t('metrics.emptyAgents')}</p>
        ) : (
          <ResponsiveContainer width="100%" height={Math.max(160, summary.topAgents.length * 52)}>
            <BarChart data={summary.topAgents.map((a) => ({ name: a.agentName, tickets: a.assignedTickets }))} layout="vertical" margin={{ top: 5, right: 24, left: 8, bottom: 5 }}>
              <CartesianGrid strokeDasharray="3 3" stroke="rgba(100,116,139,0.2)" horizontal={false} />
              <XAxis type="number" tick={{ fontSize: 11 }} tickLine={false} axisLine={false} allowDecimals={false} />
              <YAxis type="category" dataKey="name" width={110} tick={{ fontSize: 12 }} tickLine={false} axisLine={false} />
              <Tooltip contentStyle={tooltipStyle} cursor={{ fill: 'rgba(139,92,246,0.06)' }} />
              <Bar dataKey="tickets" name={t('metrics.ticketsShort')} radius={[0, 8, 8, 0]} barSize={18}>
                {summary.topAgents.map((_, i) => (
                  <Cell key={i} fill={`hsl(${252 - i * 14}, 83%, ${58 + i * 4}%)`} />
                ))}
              </Bar>
            </BarChart>
          </ResponsiveContainer>
        )}
      </div>
    </div>
  );
}

function formatSeconds(seconds: number | null, noDataLabel: string): string {
  if (seconds === null) return noDataLabel;
  if (seconds < 60) return `${Math.round(seconds)} s`;
  const minutes = Math.floor(seconds / 60);
  if (minutes < 60) return `${minutes} min`;
  const hours = Math.floor(minutes / 60);
  return `${hours} h ${minutes % 60} min`;
}
