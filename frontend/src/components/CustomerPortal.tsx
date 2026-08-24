import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import type { TicketPriority } from '../services/api';
import { useCreateTicket, useTickets } from '../hooks/useData';
import { apiDate } from '../lib/time';
import TicketDetailPanel from './TicketDetailPanel';
import { PRIORITY_STYLES, STATUS_STYLES } from './ticketUi';

const ALL_PRIORITIES: TicketPriority[] = ['LOW', 'MEDIUM', 'HIGH', 'URGENT'];

/** Portal simplificado para CUSTOMER: crea tickets y ve solo los suyos. */
export default function CustomerPortal({ onOpenChat }: { onOpenChat?: (ticketId: number) => void }) {
  const { t } = useTranslation();
  const [newTitle, setNewTitle] = useState('');
  const [newDesc, setNewDesc] = useState('');
  const [newPriority, setNewPriority] = useState<TicketPriority>('MEDIUM');
  const [selectedId, setSelectedId] = useState<number | null>(null);

  // El backend ya filtra por cliente cuando el rol es CUSTOMER
  const ticketsQuery = useTickets({ size: 20 });
  const createMutation = useCreateTicket();

  const tickets = ticketsQuery.data?.content ?? [];
  const totalElements = ticketsQuery.data?.totalElements ?? 0;
  const error =
    ticketsQuery.error?.message ?? createMutation.error?.message ?? '';

  const handleCreateTicket = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await createMutation.mutateAsync({ title: newTitle, description: newDesc, priority: newPriority });
      setNewTitle('');
      setNewDesc('');
      setNewPriority('MEDIUM');
    } catch {
      /* el error ya vive en la mutacion */
    }
  };

  return (
    <>
      {/* Crear ticket */}
      <section className="rounded-2xl border border-slate-200/70 bg-white p-5 shadow-sm sm:p-6 dark:border-white/10 dark:bg-white/[0.03]">
        <h2 className="font-display text-base font-bold tracking-tight text-slate-900 dark:text-white">
          {t('portal.newTicket')}
        </h2>
        <p className="mt-1 text-xs text-slate-400">{t('portal.welcome')}</p>
        <form onSubmit={handleCreateTicket} className="mt-4 space-y-3">
          <input
            type="text"
            value={newTitle}
            onChange={(e) => setNewTitle(e.target.value)}
            placeholder={t('portal.titlePlaceholder')}
            className="w-full rounded-xl border border-slate-300 bg-white px-3.5 py-2.5 text-sm outline-none transition-shadow placeholder:text-slate-400 focus:border-brand-400 focus:ring-4 focus:ring-brand-500/15 dark:border-white/10 dark:bg-white/5 dark:text-white"
            required
            maxLength={255}
          />
          <textarea
            value={newDesc}
            onChange={(e) => setNewDesc(e.target.value)}
            placeholder={t('portal.descPlaceholder')}
            className="w-full resize-none rounded-xl border border-slate-300 bg-white px-3.5 py-2.5 text-sm outline-none transition-shadow placeholder:text-slate-400 focus:border-brand-400 focus:ring-4 focus:ring-brand-500/15 dark:border-white/10 dark:bg-white/5 dark:text-white"
            rows={3}
            required
          />
          <div className="flex flex-wrap items-center gap-3">
            <label className="text-xs font-semibold text-slate-500 dark:text-slate-400">
              {t('portal.priorityLabel')}
            </label>
            <select
              value={newPriority}
              onChange={(e) => setNewPriority(e.target.value as TicketPriority)}
              className="cursor-pointer rounded-xl border border-slate-300 bg-white px-3 py-2.5 text-sm outline-none focus:border-brand-400 dark:border-white/10 dark:bg-white/5 dark:text-white"
            >
              {ALL_PRIORITIES.map((p) => (
                <option key={p} value={p}>{p}</option>
              ))}
            </select>
            <button
              type="submit"
              disabled={createMutation.isPending}
              className="ml-auto cursor-pointer rounded-xl bg-gradient-to-r from-brand-500 to-violet-500 px-5 py-2.5 text-sm font-semibold text-white shadow-lg shadow-brand-500/25 transition-all duration-200 hover:-translate-y-0.5 hover:shadow-xl disabled:cursor-not-allowed disabled:opacity-60 disabled:hover:translate-y-0"
            >
              {createMutation.isPending ? t('portal.creating') : t('portal.createBtn')}
            </button>
          </div>
        </form>

        {error && (
          <div className="animate-fade-up mt-4 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 dark:border-red-500/20 dark:bg-red-500/10 dark:text-red-300">
            {error}
          </div>
        )}
      </section>

      {/* Detalle del ticket seleccionado */}
      {selectedId !== null && (
        <TicketDetailPanel
          ticketId={selectedId}
          role="CUSTOMER"
          onClose={() => setSelectedId(null)}
          onOpenChat={onOpenChat}
        />
      )}

      {/* Mis tickets */}
      <section className="space-y-3">
        <div className="flex flex-wrap items-center justify-between gap-2">
          <h2 className="font-display text-base font-bold tracking-tight text-slate-900 dark:text-white">
            {t('portal.mine', { count: totalElements })}
          </h2>
          <p className="text-xs text-slate-400">{t('portal.reopenHint')}</p>
        </div>

        {ticketsQuery.isLoading && (
          <p className="py-8 text-center text-sm text-slate-400">{t('portal.loading')}</p>
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
            <p className="mt-2 line-clamp-2 text-sm text-slate-600 dark:text-slate-400">{tk.description}</p>
            <p className="mt-1.5 text-xs text-slate-400">
              {tk.agentName
                ? `${t('tickets.agent')}: ${tk.agentName}`
                : `${t('tickets.agent')}: ${t('tickets.unassigned')}`}
              {tk.slaDueAt && ` · ${t('tickets.slaDue')}: ${apiDate(tk.slaDueAt).toLocaleString('es')}`}
            </p>
          </article>
        ))}

        {tickets.length === 0 && !error && !ticketsQuery.isLoading && (
          <p className="py-8 text-center text-sm text-slate-400">{t('portal.empty')}</p>
        )}
      </section>
    </>
  );
}
