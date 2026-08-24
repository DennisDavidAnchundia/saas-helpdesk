import { useEffect, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import {
  downloadAttachment,
  useAgents,
  useAssignAgent,
  useAttachments,
  useAutoAssignAgent,
  useChangeTicketStatus,
  useDeleteAttachment,
  useTicket,
  useUploadAttachment,
  type AttachmentInfo,
} from '../hooks/useData';
import { apiDate } from '../lib/time';
import { ChatIcon } from './icons';
import { PRIORITY_STYLES, STATUS_STYLES, TRANSITIONS } from './ticketUi';

interface Props {
  ticketId: number;
  role: string;
  onClose: () => void;
  /** Si viene, muestra el boton que salta al chat de este ticket */
  onOpenChat?: (ticketId: number) => void;
}

/** "4h 12m" a partir de una diferencia en milisegundos (se muestra con signo aparte). */
function humanizeDuration(diffMs: number): string {
  const abs = Math.abs(diffMs);
  const h = Math.floor(abs / 3_600_000);
  const m = Math.floor((abs % 3_600_000) / 60_000);
  return h > 0 ? `${h}h ${m}m` : `${m}m`;
}

/** Reloj para el chip de SLA: hace viva la cuenta regresiva sin impurezas en render. */
function useNow(intervalMs = 60_000): number {
  const [now, setNow] = useState(() => Date.now());
  useEffect(() => {
    const id = setInterval(() => setNow(Date.now()), intervalMs);
    return () => clearInterval(id);
  }, [intervalMs]);
  return now;
}

/** "1.2 MB" legible a partir de bytes. */
function formatSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

/** Tarjeta de archivos adjuntos del ticket: lista, subida y descarga protegida. */
function AttachmentsCard({ ticketId, role }: { ticketId: number; role: string }) {
  const { t, i18n } = useTranslation();
  const query = useAttachments(ticketId);
  const upload = useUploadAttachment(ticketId);
  const remove = useDeleteAttachment(ticketId);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [downloadingId, setDownloadingId] = useState<number | null>(null);

  const isStaff = role === 'ADMIN' || role === 'AGENT';

  const handleDownload = async (att: AttachmentInfo) => {
    setDownloadingId(att.id);
    try {
      await downloadAttachment(ticketId, att);
    } catch {
      /* el navegador no descarga; el error no rompe la UI */
    } finally {
      setDownloadingId(null);
    }
  };

  return (
    <div className="mt-4 rounded-2xl border border-slate-200/70 p-4 dark:border-white/10">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <p className="text-sm font-semibold text-slate-700 dark:text-slate-200">
          📎 {t('tickets.attachments')}
          {query.data && query.data.length > 0 && (
            <span className="ml-2 rounded-full bg-slate-100 px-2 py-0.5 text-[11px] font-bold text-slate-500 dark:bg-white/10 dark:text-slate-300">
              {query.data.length}
            </span>
          )}
        </p>
        <input
          ref={fileInputRef}
          type="file"
          className="hidden"
          onChange={(e) => {
            const file = e.target.files?.[0];
            if (file) upload.mutate(file);
            e.target.value = '';
          }}
        />
        <button
          onClick={() => fileInputRef.current?.click()}
          disabled={upload.isPending}
          className="cursor-pointer rounded-xl border border-slate-300 px-3 py-1.5 text-xs font-semibold text-slate-600 transition-all hover:-translate-y-0.5 hover:border-brand-400 hover:bg-brand-50 hover:text-brand-600 disabled:cursor-not-allowed disabled:opacity-50 dark:border-white/10 dark:text-slate-300 dark:hover:border-brand-500/40 dark:hover:bg-brand-500/10 dark:hover:text-brand-300"
        >
          {upload.isPending ? t('tickets.uploading') : `＋ ${t('tickets.attach')}`}
        </button>
      </div>

      {(upload.error || remove.error) && (
        <p className="mt-2 text-xs font-medium text-red-600 dark:text-red-400">
          {upload.error?.message ?? remove.error?.message}
        </p>
      )}

      {query.isLoading ? (
        <p className="mt-3 text-xs text-slate-400">{t('common.loading')}</p>
      ) : !query.data || query.data.length === 0 ? (
        <p className="mt-3 text-xs text-slate-400">{t('tickets.noAttachments')}</p>
      ) : (
        <ul className="mt-3 divide-y divide-slate-100 dark:divide-white/5">
          {query.data.map((att) => (
            <li key={att.id} className="flex flex-wrap items-center gap-2 py-2">
              <span aria-hidden className="text-base">📄</span>
              <span className="min-w-0 flex-1 truncate text-xs font-medium text-slate-700 dark:text-slate-200">
                {att.fileName}
              </span>
              <span className="text-[11px] text-slate-400">{formatSize(att.sizeBytes)}</span>
              <span className="hidden text-[11px] text-slate-400 sm:inline">
                {att.uploaderName} · {apiDate(att.createdAt).toLocaleDateString(i18n.language)}
              </span>
              <button
                onClick={() => handleDownload(att)}
                disabled={downloadingId === att.id}
                className="cursor-pointer rounded-lg px-2 py-1 text-[11px] font-semibold text-brand-600 transition-colors hover:bg-brand-50 disabled:opacity-50 dark:text-brand-300 dark:hover:bg-brand-500/10"
              >
                ⬇ {downloadingId === att.id ? '…' : t('tickets.download')}
              </button>
              {isStaff && (
                <button
                  onClick={() => remove.mutate(att.id)}
                  disabled={remove.isPending}
                  title={t('tickets.deleteAttachment')}
                  className="cursor-pointer rounded-lg px-2 py-1 text-[11px] font-semibold text-red-500 transition-colors hover:bg-red-50 disabled:opacity-50 dark:hover:bg-red-500/10"
                >
                  ✕
                </button>
              )}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

export default function TicketDetailPanel({ ticketId, role, onClose, onOpenChat }: Props) {
  const { t, i18n } = useTranslation();
  const now = useNow();
  const [selectedAgent, setSelectedAgent] = useState('');
  const ticketQuery = useTicket(ticketId);
  // El endpoint de agentes es solo ADMIN/AGENT: no lo llamamos para CUSTOMER
  const canManage = role === 'ADMIN' || role === 'AGENT';
  const agentsQuery = useAgents(canManage);
  const statusMutation = useChangeTicketStatus();
  const assignMutation = useAssignAgent();
  const autoAssignMutation = useAutoAssignAgent();

  if (ticketQuery.isLoading) {
    return (
      <section className="rounded-2xl border border-slate-200/70 bg-white p-6 text-sm text-slate-400 shadow-sm dark:border-white/10 dark:bg-white/[0.03]">
        {t('tickets.loadingDetail')}
      </section>
    );
  }

  if (!ticketQuery.data) {
    return (
      <section className="rounded-2xl border border-red-200 bg-red-50 p-6 text-sm text-red-700 shadow-sm dark:border-red-500/20 dark:bg-red-500/10 dark:text-red-300">
        {ticketQuery.error?.message ?? t('tickets.notFound')}
      </section>
    );
  }

  const tk = ticketQuery.data;
  const error =
    statusMutation.error?.message ??
    assignMutation.error?.message ??
    autoAssignMutation.error?.message ??
    '';
  const busy =
    statusMutation.isPending || assignMutation.isPending || autoAssignMutation.isPending;

  // Un CUSTOMER solo puede reabrir; ADMIN/AGENT pueden todo lo que permite la maquina de estados
  const transitions =
    role === 'CUSTOMER'
      ? (TRANSITIONS[tk.status] ?? []).filter((s) => s === 'REOPENED')
      : TRANSITIONS[tk.status] ?? [];

  const fmtDate = (value: string | null) =>
    value ? apiDate(value).toLocaleString(i18n.language) : '—';

  const metaRow = (label: string, value: string) => (
    <div className="rounded-xl bg-slate-50 px-3.5 py-2.5 dark:bg-white/[0.04]">
      <p className="text-[11px] font-semibold uppercase tracking-wide text-slate-400">{label}</p>
      <p className="mt-0.5 truncate text-sm text-slate-700 dark:text-slate-200">{value}</p>
    </div>
  );

  const handleAssign = async () => {
    if (!selectedAgent) return;
    try {
      await assignMutation.mutateAsync({ id: tk.id, agentId: Number(selectedAgent) });
      setSelectedAgent('');
    } catch {
      /* el error ya vive en la mutacion */
    }
  };

  return (
    <section className="animate-fade-up rounded-2xl border border-slate-200/70 bg-white p-5 shadow-sm sm:p-6 dark:border-white/10 dark:bg-white/[0.03]">
      {/* Cabecera */}
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div className="flex flex-wrap items-center gap-2">
          <span className="font-mono text-xs font-semibold text-brand-500 dark:text-brand-400">
            #{tk.id}
          </span>
          <h2 className="font-display text-lg font-bold tracking-tight text-slate-900 dark:text-white">
            {tk.title}
          </h2>
          <span className={`rounded-full px-2 py-0.5 text-[11px] font-semibold ${STATUS_STYLES[tk.status]}`}>
            {tk.status}
          </span>
          <span className={`rounded-full px-2 py-0.5 text-[11px] font-semibold ${PRIORITY_STYLES[tk.priority]}`}>
            {tk.priority}
          </span>
          {(() => {
            // Chip de SLA con cuenta regresiva solo mientras el ticket sigue abierto
            const open = tk.status === 'OPEN' || tk.status === 'IN_PROGRESS' || tk.status === 'REOPENED';
            if (!open || !tk.slaDueAt) return null;
            const diffMs = apiDate(tk.slaDueAt).getTime() - now;
            const overdue = !!tk.slaBreached || diffMs < 0;
            const soon = !overdue && diffMs < 2 * 60 * 60 * 1000;
            return (
              <span
                title={apiDate(tk.slaDueAt).toLocaleString('es')}
                className={`rounded-full px-2 py-0.5 text-[11px] font-bold ${
                  overdue
                    ? 'animate-pulse-dot bg-red-600 text-white'
                    : soon
                      ? 'bg-amber-500 text-white'
                      : 'bg-emerald-100 text-emerald-700 dark:bg-emerald-500/15 dark:text-emerald-300'
                }`}
              >
                {overdue
                  ? t('tickets.overdueBy', { time: humanizeDuration(diffMs) })
                  : t('tickets.dueIn', { time: humanizeDuration(diffMs) })}
              </span>
            );
          })()}
        </div>
        <button
          onClick={onClose}
          aria-label={t('tickets.closeDetail')}
          title={t('tickets.closeDetail')}
          className="cursor-pointer rounded-lg border border-slate-200/70 px-2 py-1 text-xs font-semibold text-slate-500 transition-colors hover:border-brand-300 hover:text-brand-600 dark:border-white/10 dark:text-slate-400 dark:hover:border-brand-500/40 dark:hover:text-brand-300"
        >
          ✕
        </button>
      </div>

      {/* Descripcion */}
      <p className="mt-3 whitespace-pre-line rounded-xl border border-slate-100 bg-slate-50/60 p-4 text-sm leading-relaxed text-slate-600 dark:border-white/5 dark:bg-white/[0.04] dark:text-slate-300">
        {tk.description}
      </p>

      <AttachmentsCard ticketId={tk.id} role={role} />

      {/* Info completa */}
      <div className="mt-4 grid grid-cols-1 gap-2.5 sm:grid-cols-2 lg:grid-cols-4">
        {metaRow(t('tickets.customer'), tk.customerName)}
        {metaRow(t('tickets.agent'), tk.agentName ?? t('tickets.unassigned'))}
        {metaRow(t('tickets.createdAt'), fmtDate(tk.createdAt))}
        {metaRow(t('tickets.updatedAt'), fmtDate(tk.updatedAt))}
        {metaRow(t('tickets.slaDue'), fmtDate(tk.slaDueAt))}
        {metaRow(t('tickets.firstResponse'), fmtDate(tk.firstResponseAt))}
        {metaRow(t('tickets.resolvedAt'), fmtDate(tk.resolvedAt))}
        {metaRow(t('tickets.closedAt'), fmtDate(tk.closedAt))}
      </div>

      {error && (
        <div className="mt-4 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 dark:border-red-500/20 dark:bg-red-500/10 dark:text-red-300">
          {error}
        </div>
      )}

      {/* Acciones */}
      <div className="mt-4 flex flex-wrap items-center gap-3 border-t border-slate-100 pt-4 dark:border-white/5">
        {transitions.map((next) => (
          <button
            key={next}
            disabled={busy}
            onClick={() => statusMutation.mutate({ id: tk.id, status: next })}
            className="cursor-pointer rounded-xl border border-slate-300 px-3.5 py-2 text-xs font-semibold text-slate-600 transition-all hover:-translate-y-0.5 hover:border-brand-400 hover:bg-brand-50 hover:text-brand-600 disabled:cursor-not-allowed disabled:opacity-50 dark:border-white/10 dark:text-slate-300 dark:hover:border-brand-500/40 dark:hover:bg-brand-500/10 dark:hover:text-brand-300"
          >
            → {next}
          </button>
        ))}

        {canManage && (
          <div className="ml-auto flex flex-wrap items-center gap-2">
            {/*
              Regla de asignacion: solo el ADMIN reparte tickets entre agentes;
              un AGENTE se toma los suyos con auto-asignar (el backend lo fuerza)
            */}
            {role === 'ADMIN' && (agentsQuery.isLoading ? (
              <span className="text-xs text-slate-400">{t('tickets.loadingAgents')}</span>
            ) : agentsQuery.error ? (
              <span className="text-xs font-medium text-red-600 dark:text-red-400">
                {t('tickets.agentsError')}: {agentsQuery.error.message}
              </span>
            ) : agentsQuery.data && agentsQuery.data.length > 0 ? (
              <>
                <select
                  value={selectedAgent}
                  onChange={(e) => setSelectedAgent(e.target.value)}
                  disabled={busy}
                  className="cursor-pointer rounded-xl border border-slate-300 bg-white px-3 py-2 text-xs outline-none focus:border-brand-400 disabled:opacity-50 dark:border-white/10 dark:bg-white/5 dark:text-slate-200"
                >
                  <option value="">{t('tickets.selectAgent')}</option>
                  {agentsQuery.data.map((a) => (
                    <option key={a.id} value={a.id}>
                      {a.fullName} ({a.email})
                    </option>
                  ))}
                </select>
                <button
                  onClick={handleAssign}
                  disabled={busy || !selectedAgent}
                  className="cursor-pointer rounded-xl bg-gradient-to-r from-brand-500 to-violet-500 px-4 py-2 text-xs font-semibold text-white shadow-md shadow-brand-500/25 transition-all hover:-translate-y-0.5 disabled:cursor-not-allowed disabled:opacity-60 disabled:hover:translate-y-0"
                >
                  {assignMutation.isPending ? t('tickets.assigning') : t('tickets.assignBtn')}
                </button>
              </>
            ) : (
              <span className="text-xs text-slate-400">{t('tickets.noAgents')}</span>
            ))}
            <button
              onClick={() => autoAssignMutation.mutate(tk.id)}
              disabled={busy}
              title={role === 'AGENT' ? t('tickets.takeTicketHint') : undefined}
              className="cursor-pointer rounded-xl border border-slate-300 px-3.5 py-2 text-xs font-semibold text-slate-600 transition-all hover:-translate-y-0.5 hover:border-brand-400 hover:bg-brand-50 hover:text-brand-600 disabled:cursor-not-allowed disabled:opacity-50 dark:border-white/10 dark:text-slate-300 dark:hover:border-brand-500/40 dark:hover:bg-brand-500/10 dark:hover:text-brand-300"
            >
              {autoAssignMutation.isPending
                ? t('tickets.assigning')
                : role === 'AGENT'
                  ? `⚡ ${t('tickets.takeTicket')}`
                  : `⚡ ${t('tickets.autoAssign')}`}
            </button>
          </div>
        )}

        {onOpenChat && (
          <button
            onClick={() => onOpenChat(tk.id)}
            className="flex cursor-pointer items-center gap-1.5 rounded-xl border border-slate-300 px-3.5 py-2 text-xs font-semibold text-slate-600 transition-all hover:-translate-y-0.5 hover:border-brand-400 hover:bg-brand-50 hover:text-brand-600 dark:border-white/10 dark:text-slate-300 dark:hover:border-brand-500/40 dark:hover:bg-brand-500/10 dark:hover:text-brand-300"
          >
            <ChatIcon /> {t('tickets.openChat')}
          </button>
        )}
      </div>
    </section>
  );
}
