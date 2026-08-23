import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import type { TicketPriority, TicketStatus } from '../services/api';
import { useChangeTicketStatus, useCreateTicket, useTickets } from '../hooks/useData';
import TicketDetailPanel from './TicketDetailPanel';
import { ALL_PRIORITIES, ALL_STATUSES, PRIORITY_STYLES, STATUS_STYLES, TRANSITIONS } from './ticketUi';

const PAGE_SIZE = 5;

interface Props {
  role: string;
  /** Salta al chat del ticket indicado (lo provee DashboardPage) */
  onOpenChat?: (ticketId: number) => void;
}

export default function TicketsSection({ role, onOpenChat }: Props) {
  const { t } = useTranslation();
  const [statusFilter, setStatusFilter] = useState<TicketStatus | ''>('');
  const [priorityFilter, setPriorityFilter] = useState<TicketPriority | ''>('');
  const [page, setPage] = useState(0);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [newTitle, setNewTitle] = useState('');
  const [newDesc, setNewDesc] = useState('');
  const [newPriority, setNewPriority] = useState<TicketPriority>('MEDIUM');

  const ticketsQuery = useTickets({
    status: statusFilter || undefined,
    priority: priorityFilter || undefined,
    page,
    size: PAGE_SIZE,
  });
  const createMutation = useCreateTicket();
  const statusMutation = useChangeTicketStatus();

  const data = ticketsQuery.data;
  const tickets = data?.content ?? [];
  const totalPages = Math.max(1, data?.totalPages ?? 1);
  const totalElements = data?.totalElements ?? 0;
  const error =
    ticketsQuery.error?.message ??
    createMutation.error?.message ??
    statusMutation.error?.message ??
    '';

  const handleCreateTicket = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await createMutation.mutateAsync({ title: newTitle, description: newDesc, priority: newPriority });
      setNewTitle('');
      setNewDesc('');
      setNewPriority('MEDIUM');
      setPage(0);
    } catch {
      /* el error ya vive en la mutacion */
    }
  };

  const changeFilter = (apply: () => void) => {
    apply();
    setPage(0);
  };

  return (
    <>
      {/* Formulario de creación */}
      <section className="rounded-2xl border border-slate-200/70 bg-white p-5 shadow-sm sm:p-6 dark:border-white/10 dark:bg-white/[0.03]">
        <h2 className="mb-4 font-display text-base font-bold tracking-tight text-slate-900 dark:text-white">
          {t('tickets.newTicket')}
        </h2>
        <form onSubmit={handleCreateTicket} className="space-y-3">
          <input
            type="text"
            value={newTitle}
            onChange={(e) => setNewTitle(e.target.value)}
            placeholder={t('tickets.titlePlaceholder')}
            className="w-full rounded-xl border border-slate-300 bg-white px-3.5 py-2.5 text-sm outline-none transition-shadow placeholder:text-slate-400 focus:border-brand-400 focus:ring-4 focus:ring-brand-500/15 dark:border-white/10 dark:bg-white/5 dark:text-white"
            required
            maxLength={255}
          />
          <textarea
            value={newDesc}
            onChange={(e) => setNewDesc(e.target.value)}
            placeholder={t('tickets.descPlaceholder')}
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
              {ALL_PRIORITIES.filter(Boolean).map((p) => (
                <option key={p} value={p}>{p}</option>
              ))}
            </select>
            <button
              type="submit"
              disabled={createMutation.isPending}
              className="cursor-pointer rounded-xl bg-gradient-to-r from-brand-500 to-violet-500 px-5 py-2.5 text-sm font-semibold text-white shadow-lg shadow-brand-500/25 transition-all duration-200 hover:-translate-y-0.5 hover:shadow-xl hover:shadow-brand-500/30 disabled:cursor-not-allowed disabled:opacity-60 disabled:hover:translate-y-0"
            >
              {createMutation.isPending ? t('tickets.creating') : t('tickets.createBtn')}
            </button>
          </div>
        </form>

        {error && (
          <div className="animate-fade-up mt-4 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 dark:border-red-500/20 dark:bg-red-500/10 dark:text-red-300">
            {error}
          </div>
        )}
      </section>

      {/* Barra de filtros */}
      <section className="flex flex-wrap items-center gap-2">
        {ALL_STATUSES.map((s) => (
          <button
            key={s || 'all'}
            onClick={() => changeFilter(() => setStatusFilter(s))}
            className={`cursor-pointer rounded-full px-3.5 py-1.5 text-xs font-semibold transition-all duration-200 ${
              statusFilter === s
                ? 'bg-gradient-to-r from-brand-500 to-violet-500 text-white shadow-md shadow-brand-500/25'
                : 'border border-slate-200/70 bg-white text-slate-500 hover:border-brand-300 hover:text-brand-600 dark:border-white/10 dark:bg-white/[0.03] dark:text-slate-400 dark:hover:border-brand-500/40 dark:hover:text-brand-300'
            }`}
          >
            {s || t('tickets.all')}
          </button>
        ))}
        <select
          value={priorityFilter}
          onChange={(e) => changeFilter(() => setPriorityFilter(e.target.value as TicketPriority | ''))}
          className={`ml-auto cursor-pointer rounded-full border px-3 py-1.5 text-xs font-semibold outline-none ${
            priorityFilter
              ? 'border-brand-400 bg-brand-50 text-brand-600 dark:border-brand-500/40 dark:bg-brand-500/15 dark:text-brand-300'
              : 'border-slate-200/70 bg-white text-slate-500 dark:border-white/10 dark:bg-white/[0.03] dark:text-slate-400'
          }`}
        >
          <option value="">{t('tickets.allPriorities')}</option>
          {ALL_PRIORITIES.filter(Boolean).map((p) => (
            <option key={p} value={p}>{p}</option>
          ))}
        </select>
      </section>

      {/* Detalle del ticket seleccionado */}
      {selectedId !== null && (
        <TicketDetailPanel
          ticketId={selectedId}
          role={role}
          onClose={() => setSelectedId(null)}
          onOpenChat={onOpenChat}
        />
      )}

      {/* Lista de tickets */}
      <section className="space-y-3">
        <h2 className="font-display text-base font-bold tracking-tight text-slate-900 dark:text-white">
          {t('tickets.count', { count: totalElements })}
        </h2>
        {ticketsQuery.isLoading && (
          <p className="py-8 text-center text-sm text-slate-400">{t('tickets.loading')}</p>
        )}
        {tickets.map((tk) => (
          <article
            key={tk.id}
            onClick={() => setSelectedId(selectedId === tk.id ? null : tk.id)}
            className={`group cursor-pointer rounded-2xl border bg-white p-4 shadow-sm transition-all duration-200 hover:-translate-y-0.5 hover:shadow-lg hover:shadow-brand-500/5 sm:p-5 dark:bg-white/[0.03] ${
              selectedId === tk.id
                ? 'border-brand-400 ring-4 ring-brand-500/15 dark:border-brand-500/60'
                : 'border-slate-200/70 hover:border-brand-300 dark:border-white/10 dark:hover:border-brand-500/40'
            }`}
          >
            <div className="flex flex-wrap items-center justify-between gap-2">
              <div className="flex flex-wrap items-center gap-2">
                <span className="font-mono text-xs font-semibold text-brand-500 dark:text-brand-400">
                  #{tk.id}
                </span>
                <span className="text-sm font-semibold text-slate-800 dark:text-slate-100">
                  {tk.title}
                </span>
                <span className={`rounded-full px-2 py-0.5 text-[11px] font-semibold ${STATUS_STYLES[tk.status]}`}>
                  {tk.status}
                </span>
                <span className={`rounded-full px-2 py-0.5 text-[11px] font-semibold ${PRIORITY_STYLES[tk.priority]}`}>
                  {tk.priority}
                </span>
                {tk.slaBreached && (
                  <span className="animate-pulse-dot rounded-full bg-red-600 px-2 py-0.5 text-[11px] font-bold text-white">
                    {t('tickets.slaBreached')}
                  </span>
                )}
              </div>
              <div className="flex gap-1.5">
                {(TRANSITIONS[tk.status] || []).map((next) => (
                  <button
                    key={next}
                    onClick={(e) => {
                      e.stopPropagation();
                      statusMutation.mutate({ id: tk.id, status: next });
                    }}
                    className="cursor-pointer rounded-lg border border-slate-300 px-2.5 py-1 text-[11px] font-medium text-slate-600 transition-colors hover:border-brand-400 hover:bg-brand-50 hover:text-brand-600 dark:border-white/10 dark:text-slate-400 dark:hover:border-brand-500/40 dark:hover:bg-brand-500/10 dark:hover:text-brand-300"
                  >
                    {next}
                  </button>
                ))}
              </div>
            </div>
            <p className="mt-2 text-sm text-slate-600 dark:text-slate-400">{tk.description}</p>
            <p className="mt-1.5 text-xs text-slate-400">
              {t('tickets.customer')}: {tk.customerName}
              {' · '}{t('tickets.agent')}: {tk.agentName || t('tickets.unassigned')}
              {tk.slaDueAt && ` · ${t('tickets.slaDue')}: ${new Date(tk.slaDueAt).toLocaleString('es')}`}
              {tk.firstResponseAt && ` · ${t('tickets.firstResponse')}: ${new Date(tk.firstResponseAt).toLocaleTimeString('es')}`}
            </p>
          </article>
        ))}
        {tickets.length === 0 && !error && !ticketsQuery.isLoading && (
          <p className="py-8 text-center text-sm text-slate-400">{t('tickets.empty')}</p>
        )}

        {/* Paginación */}
        {totalPages > 1 && (
          <div className="flex items-center justify-between pt-2">
            <button
              onClick={() => setPage((p) => Math.max(0, p - 1))}
              disabled={page === 0}
              className="cursor-pointer rounded-xl border border-slate-200/70 bg-white px-4 py-2 text-xs font-semibold text-slate-600 transition-colors hover:border-brand-300 hover:text-brand-600 disabled:cursor-not-allowed disabled:opacity-40 dark:border-white/10 dark:bg-white/[0.03] dark:text-slate-400 dark:hover:text-brand-300"
            >
              ← {t('tickets.prev')}
            </button>
            <span className="text-xs text-slate-400">
              {t('tickets.pageOf', { page: page + 1, totalPages })}
            </span>
            <button
              onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
              disabled={page >= totalPages - 1}
              className="cursor-pointer rounded-xl border border-slate-200/70 bg-white px-4 py-2 text-xs font-semibold text-slate-600 transition-colors hover:border-brand-300 hover:text-brand-600 disabled:cursor-not-allowed disabled:opacity-40 dark:border-white/10 dark:bg-white/[0.03] dark:text-slate-400 dark:hover:text-brand-300"
            >
              {t('tickets.next')} →
            </button>
          </div>
        )}
      </section>
    </>
  );
}
