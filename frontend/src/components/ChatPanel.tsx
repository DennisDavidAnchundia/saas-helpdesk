import { useEffect, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Client, type IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { getMessages, getPresence, type ChatMessage, type OnlineUser, type SendChatPayload, type Ticket, type TicketStatus } from '../services/api';
import { downloadAttachment, useChangeTicketStatus, useMarkTicketRead, useUnreadCounts, useUploadFile, type AttachmentInfo } from '../hooks/useData';
import { decodeJwt } from '../lib/jwt';
import { API_URL } from '../lib/config';
import { apiDate } from '../lib/time';
import { STATUS_STYLES, TRANSITIONS } from './ticketUi';

interface Props {
  token: string;
  tickets: Ticket[];
  /** Ticket a mostrar al llegar desde el boton "Abrir chat" del detalle */
  focusTicketId?: number | null;
}

/** Tamaño humanizado para chips de adjuntos. */
function humanSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1048576) return `${(bytes / 1024).toFixed(0)} kB`;
  return `${(bytes / 1048576).toFixed(1)} MB`;
}

export default function ChatPanel({ token, tickets, focusTicketId }: Props) {
  const { t, i18n } = useTranslation();
  const payload = decodeJwt(token);
  const tenantId = payload.tenantId;
  const userId = Number(payload.userId);
  const role = payload.role;

  const [selectedId, setSelectedId] = useState<number | null>(tickets[0]?.id ?? null);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  // Paginacion: proxima pagina a pedir y total; null = no hay historial viejo
  const [older, setOlder] = useState<{ next: number; total: number } | null>(null);
  const [loadingOlder, setLoadingOlder] = useState(false);
  const [input, setInput] = useState('');
  const [connected, setConnected] = useState(false);
  const [onlineIds, setOnlineIds] = useState<number[]>([]);
  const [onlineRoster, setOnlineRoster] = useState<OnlineUser[]>([]);
  const [error, setError] = useState('');

  // Adjunto pendiente de enviar con el proximo mensaje (ya subido al ticket)
  const [pendingFile, setPendingFile] = useState<AttachmentInfo | null>(null);
  const upload = useUploadFile();
  const chatFileRef = useRef<HTMLInputElement>(null);

  const statusMutation = useChangeTicketStatus();
  const markReadMutation = useMarkTicketRead();
  const unreadQuery = useUnreadCounts(token);
  const unreadMap = unreadQuery.data ?? {};
  const selectedTicket = tickets.find((tk) => tk.id === selectedId) ?? null;
  const canResolve =
    role !== 'CUSTOMER' &&
    selectedTicket != null &&
    (TRANSITIONS[selectedTicket.status as TicketStatus] ?? []).includes('RESOLVED');

  // Ref para usar la mutacion desde el callback del WebSocket sin closures viejas
  const markReadRef = useRef(markReadMutation);
  useEffect(() => {
    markReadRef.current = markReadMutation;
  }, [markReadMutation]);

  const clientRef = useRef<Client | null>(null);
  const subRef = useRef<{ unsubscribe: () => void } | null>(null);
  const selectedRef = useRef<number | null>(selectedId);
  const endRef = useRef<HTMLDivElement | null>(null);
  const scrollBoxRef = useRef<HTMLDivElement | null>(null);
  const lastMsgIdRef = useRef<number | null>(null);
  const prevHeightRef = useRef(0);

  useEffect(() => {
    selectedRef.current = selectedId;
  }, [selectedId]);

  // Si llegamos desde el detalle de un ticket, seleccionamos esa conversacion
  useEffect(() => {
    if (focusTicketId != null) setSelectedId(focusTicketId);
  }, [focusTicketId]);

  // Abrir una conversacion implica leerla
  useEffect(() => {
    if (selectedId) markReadMutation.mutate(selectedId);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selectedId]);

  useEffect(() => {
    if (!selectedId) return;
    setError('');
    setLoadingOlder(false);
    setPendingFile(null);
    getMessages(token, selectedId)
      .then((pageData) => {
        setMessages(pageData.content);
        setOlder(
          pageData.totalPages > 1
            ? { next: pageData.page + 1, total: pageData.totalPages }
            : null,
        );
      })
      .catch((err) => setError(err.message));
    getPresence(token, selectedId)
      .then((users) => {
        setOnlineRoster(users);
        setOnlineIds(users.map((u) => u.id));
      })
      .catch(() => {});
  }, [selectedId, token]);

  /** Pide la pagina anterior y antepone los mensajes sin que salte la vista. */
  const loadOlder = async () => {
    if (!selectedId || !older || loadingOlder) return;
    prevHeightRef.current = scrollBoxRef.current?.scrollHeight ?? 0;
    setLoadingOlder(true);
    try {
      const pageData = await getMessages(token, selectedId, older.next);
      setMessages((prev) => [...pageData.content, ...prev]);
      setOlder(
        pageData.page + 1 < pageData.totalPages
          ? { next: pageData.page + 1, total: pageData.totalPages }
          : null,
      );
      requestAnimationFrame(() => {
        const box = scrollBoxRef.current;
        if (box) box.scrollTop = box.scrollHeight - prevHeightRef.current;
      });
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Error al cargar mensajes');
    } finally {
      setLoadingOlder(false);
    }
  };

  useEffect(() => {
    if (!tenantId) return;

    const client = new Client({
      webSocketFactory: () => new SockJS(`${API_URL}/ws`),
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 3000,
      onConnect: () => {
        setConnected(true);
        client.subscribe(`/topic/presence/${tenantId}`, (m: IMessage) => {
          try {
            const data = JSON.parse(m.body);
            const ids: number[] = Array.isArray(data.online) ? data.online : [];
            setOnlineIds(ids);
            // Si aparece alguien que no esta en el roster, pedimos nombres de nuevo
            setOnlineRoster((prev) => {
              if (ids.some((id) => !prev.some((u) => u.id === id)) && selectedRef.current) {
                getPresence(token, selectedRef.current)
                  .then(setOnlineRoster)
                  .catch(() => {});
              }
              return prev;
            });
          } catch { /* ignore */ }
        });
        if (selectedRef.current) {
          subRef.current?.unsubscribe();
          subRef.current = client.subscribe(
            `/topic/tickets/${selectedRef.current}`,
            handleIncoming
          );
        }
      },
      onWebSocketClose: () => setConnected(false),
      onStompError: (frame) => setError(frame.headers['message'] || 'Error de autenticacion en chat'),
    });

    function handleIncoming(m: IMessage) {
      try {
        const msg = JSON.parse(m.body) as ChatMessage;
        if (msg.ticketId === selectedRef.current) {
          setMessages((prev) => (prev.some((x) => x.id === msg.id) ? prev : [...prev, msg]));
          // Estamos viendo esta conversacion: queda leida al instante
          markReadRef.current.mutate(msg.ticketId);
        }
      } catch { /* ignore */ }
    }

    clientRef.current = client;
    client.activate();

    return () => {
      subRef.current?.unsubscribe();
      subRef.current = null;
      client.deactivate();
      clientRef.current = null;
      setConnected(false);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [token, tenantId]);

  useEffect(() => {
    const client = clientRef.current;
    if (!client || !client.connected || !selectedId) return;
    subRef.current?.unsubscribe();
    subRef.current = client.subscribe(`/topic/tickets/${selectedId}`, (m: IMessage) => {
      try {
        const msg = JSON.parse(m.body) as ChatMessage;
        if (msg.ticketId === selectedRef.current) {
          setMessages((prev) => (prev.some((x) => x.id === msg.id) ? prev : [...prev, msg]));
        }
      } catch { /* ignore */ }
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [connected, selectedId]);

  // Baja al final solo cuando cambia el ULTIMO mensaje (llego uno nuevo o se
  // cambio de conversacion); antecponer historial viejo no debe mover la vista.
  useEffect(() => {
    const last = messages[messages.length - 1];
    if (!last || last.id === lastMsgIdRef.current) return;
    lastMsgIdRef.current = last.id;
    endRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  const handleFilePicked = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    e.target.value = '';
    if (!file || !selectedId) return;
    try {
      // Se sube YA al ticket; el mensaje solo referencia su id por WS
      const att = await upload.mutateAsync({ ticketId: selectedId, file });
      setPendingFile(att);
      setError('');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Error al subir el archivo');
    }
  };

  const handleSend = (e: React.FormEvent) => {
    e.preventDefault();
    const content = input.trim();
    if ((!content && !pendingFile) || !selectedId) return;
    if (!clientRef.current?.connected) {
      setError('Sin conexion con el servidor de chat');
      return;
    }
    const payload: SendChatPayload = { content };
    if (pendingFile) payload.attachmentId = pendingFile.id;
    clientRef.current.publish({
      destination: `/app/chat/${selectedId}`,
      body: JSON.stringify(payload),
    });
    setInput('');
    setPendingFile(null);
  };

  const handleDownloadAttachment = async (m: ChatMessage) => {
    if (!m.attachment) return;
    try {
      await downloadAttachment(m.ticketId, m.attachment);
    } catch { /* la descarga falla en silencio */ }
  };

  return (
    <div className="animate-fade-up rounded-2xl border border-slate-200/70 bg-white p-5 shadow-sm sm:p-6 dark:border-white/10 dark:bg-white/[0.03]">
      <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
        <h2 className="font-display text-base font-bold tracking-tight text-slate-900 dark:text-white">
          {t('chat.title')}
        </h2>
        <div className="flex items-center gap-2 text-xs">
          <span
            className={`size-2 rounded-full ${
              connected ? 'animate-pulse-dot bg-emerald-500' : 'bg-red-500'
            }`}
          />
          <span className={connected ? 'font-medium text-emerald-600 dark:text-emerald-400' : 'font-medium text-red-500'}>
            {connected ? t('chat.connected') : t('chat.disconnected')}
          </span>
          <span className="hidden text-slate-400 sm:inline">·</span>
          <span className="hidden flex-wrap items-center gap-1 sm:flex">
            {onlineRoster.filter((u) => onlineIds.includes(u.id)).map((u) => (
              <span
                key={u.id}
                title={u.role}
                className="inline-flex items-center gap-1 rounded-full bg-emerald-50 px-2 py-0.5 text-[11px] font-medium text-emerald-700 dark:bg-emerald-500/10 dark:text-emerald-300"
              >
                <span className="size-1.5 rounded-full bg-emerald-500" />
                {u.fullName}
                {u.id === userId && ' (vos)'}
              </span>
            ))}
            {onlineIds.length === 0 && (
              <span className="text-[11px] text-slate-400">{t('chat.nobodyOnline')}</span>
            )}
          </span>
        </div>
      </div>

      {tickets.length === 0 ? (
        <p className="py-8 text-center text-sm text-slate-400">{t('chat.needTicket')}</p>
      ) : (
        <>
          <div className="mb-4 max-h-44 space-y-1.5 overflow-y-auto pr-1">
            {tickets.map((tk) => {
              const unread = Number(unreadMap[String(tk.id)] ?? 0);
              const active = tk.id === selectedId;
              return (
                <button
                  key={tk.id}
                  type="button"
                  onClick={() => setSelectedId(tk.id)}
                  className={`flex w-full cursor-pointer items-center gap-2 rounded-xl border px-3 py-2 text-left text-sm transition-colors ${
                    active
                      ? 'border-brand-300 bg-brand-50/70 dark:border-brand-500/40 dark:bg-brand-500/10'
                      : 'border-slate-200/70 bg-white hover:border-brand-200 dark:border-white/10 dark:bg-white/[0.03] dark:hover:border-brand-500/30'
                  }`}
                >
                  <span className="min-w-0 flex-1 truncate">
                    <span className="font-semibold text-slate-700 dark:text-slate-200">#{tk.id}</span>{' '}
                    <span className="text-slate-500 dark:text-slate-400">{tk.title}</span>
                  </span>
                  {unread > 0 && (
                    <span className="grid min-w-5 shrink-0 place-items-center rounded-full bg-red-500 px-1.5 py-0.5 text-[10px] font-bold text-white shadow-sm">
                      {unread > 99 ? '99+' : unread}
                    </span>
                  )}
                  <span
                    className={`shrink-0 rounded-md px-1.5 py-0.5 text-[10px] font-semibold ${
                      STATUS_STYLES[tk.status as TicketStatus] ?? ''
                    }`}
                  >
                    {tk.status}
                  </span>
                </button>
              );
            })}
          </div>

          {error && (
            <div className="mb-3 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 dark:border-red-500/20 dark:bg-red-500/10 dark:text-red-300">
              {error}
            </div>
          )}

          {canResolve && (
            <div className="mb-3 flex flex-wrap items-center justify-between gap-2 rounded-xl border border-emerald-200/70 bg-emerald-50/70 px-3 py-2 dark:border-emerald-500/20 dark:bg-emerald-500/10">
              <span className="text-xs font-medium text-emerald-700 dark:text-emerald-300">
                #{selectedTicket!.id} · {selectedTicket!.status}
              </span>
              <button
                type="button"
                onClick={() => statusMutation.mutate({ id: selectedTicket!.id, status: 'RESOLVED' })}
                disabled={statusMutation.isPending}
                title={t('chat.resolveHint')}
                className="cursor-pointer rounded-lg bg-emerald-600 px-3 py-1.5 text-xs font-semibold text-white shadow-sm transition-all hover:-translate-y-0.5 hover:bg-emerald-700 disabled:cursor-not-allowed disabled:opacity-50"
              >
                ✓ {t('chat.resolve')}
              </button>
            </div>
          )}
          {statusMutation.error && (
            <p className="mb-3 rounded-xl bg-red-50 px-3 py-2 text-xs text-red-600 dark:bg-red-500/10 dark:text-red-400">
              {statusMutation.error.message}
            </p>
          )}

          <div
            ref={scrollBoxRef}
            className="h-[50dvh] max-h-96 min-h-64 space-y-2 overflow-y-auto rounded-xl border border-slate-200/70 bg-slate-50/70 p-4 dark:border-white/10 dark:bg-black/20"
          >
            {older && (
              <div className="flex justify-center">
                <button
                  type="button"
                  onClick={loadOlder}
                  disabled={loadingOlder}
                  className="cursor-pointer rounded-full border border-slate-200 bg-white px-3 py-1 text-[11px] font-semibold text-slate-500 shadow-sm transition-all hover:-translate-y-0.5 hover:border-brand-300 hover:text-brand-600 disabled:cursor-not-allowed disabled:opacity-60 dark:border-white/10 dark:bg-white/5 dark:text-slate-300 dark:hover:border-brand-500/40"
                >
                  {loadingOlder ? t('chat.loadingOlder') : t('chat.loadOlder')}
                </button>
              </div>
            )}
            {messages.map((m) => {
              const mine = m.senderId === userId;
              return (
                <div key={m.id} className={`flex ${mine ? 'justify-end' : 'justify-start'}`}>
                  <div
                    className={`max-w-[75%] rounded-2xl px-3.5 py-2 text-sm shadow-sm ${
                      mine
                        ? 'rounded-br-md bg-gradient-to-br from-brand-500 to-violet-500 text-white shadow-brand-500/25'
                        : 'rounded-bl-md border border-slate-200/70 bg-white text-slate-800 dark:border-white/10 dark:bg-white/[0.06] dark:text-slate-100'
                    }`}
                  >
                    {!mine && (
                      <p className="mb-0.5 text-xs font-semibold text-brand-500 dark:text-brand-300">
                        {m.senderName}
                      </p>
                    )}
                    {m.content && <p className="whitespace-pre-wrap break-words">{m.content}</p>}
                    {m.attachment && (
                      <button
                        type="button"
                        onClick={() => handleDownloadAttachment(m)}
                        title={t('tickets.download')}
                        className={`mt-1 flex cursor-pointer items-center gap-1.5 rounded-lg px-2 py-1 text-xs font-semibold transition-colors ${
                          mine
                            ? 'bg-white/15 hover:bg-white/30'
                            : 'bg-slate-100 hover:bg-slate-200 dark:bg-white/10 dark:hover:bg-white/20'
                        }`}
                      >
                        <span aria-hidden>📎</span>
                        <span className="max-w-36 truncate">{m.attachment.fileName}</span>
                        <span className="opacity-70">{humanSize(m.attachment.sizeBytes)}</span>
                        <span aria-hidden>⬇</span>
                      </button>
                    )}
                    <p className={`mt-1 text-[10px] ${mine ? 'text-white/70' : 'text-slate-400'}`}>
                      {apiDate(m.sentAt).toLocaleTimeString(i18n.language)}
                    </p>
                  </div>
                </div>
              );
            })}
            {messages.length === 0 && (
              <p className="py-10 text-center text-xs text-slate-400">{t('chat.noMessages')}</p>
            )}
            <div ref={endRef} />
          </div>

          {pendingFile && (
            <div className="mb-2 flex items-center gap-2 rounded-xl border border-brand-200 bg-brand-50/70 px-3 py-1.5 text-xs dark:border-brand-500/30 dark:bg-brand-500/10">
              <span aria-hidden>📄</span>
              <span className="min-w-0 flex-1 truncate font-medium text-brand-700 dark:text-brand-200">
                {pendingFile.fileName}
              </span>
              <span className="shrink-0 text-[10px] text-slate-400">
                {humanSize(pendingFile.sizeBytes)}
              </span>
              <button
                type="button"
                onClick={() => setPendingFile(null)}
                title={t('chat.removeAttachment')}
                className="cursor-pointer font-bold text-slate-400 transition-colors hover:text-red-500"
              >
                ✕
              </button>
            </div>
          )}

          <form onSubmit={handleSend} className="mt-3 flex gap-2">
            <input
              ref={chatFileRef}
              type="file"
              className="hidden"
              onChange={handleFilePicked}
            />
            <button
              type="button"
              onClick={() => chatFileRef.current?.click()}
              disabled={!connected || upload.isPending || !selectedId}
              title={t('chat.attach')}
              className="cursor-pointer rounded-xl border border-slate-300 px-3 text-lg text-slate-500 transition-all hover:-translate-y-0.5 hover:border-brand-300 hover:text-brand-600 disabled:cursor-not-allowed disabled:opacity-50 dark:border-white/10 dark:text-slate-400 dark:hover:border-brand-500/40"
            >
              {upload.isPending ? '…' : '📎'}
            </button>
            <input
              type="text"
              value={input}
              onChange={(e) => setInput(e.target.value)}
              placeholder={pendingFile ? t('chat.placeholderFile') : connected ? t('chat.placeholder') : t('chat.waitingConnection')}
              disabled={!connected}
              className="flex-1 rounded-xl border border-slate-300 bg-white px-3.5 py-2.5 text-sm outline-none transition-shadow placeholder:text-slate-400 focus:border-brand-400 focus:ring-4 focus:ring-brand-500/15 disabled:cursor-not-allowed disabled:bg-slate-100 disabled:opacity-60 dark:border-white/10 dark:bg-white/5 dark:text-white dark:disabled:bg-white/[0.02]"
            />
            <button
              type="submit"
              disabled={!connected || (!input.trim() && !pendingFile)}
              className="cursor-pointer rounded-xl bg-gradient-to-r from-brand-500 to-violet-500 px-5 py-2.5 text-sm font-semibold text-white shadow-lg shadow-brand-500/25 transition-all duration-200 hover:-translate-y-0.5 hover:shadow-xl hover:shadow-brand-500/30 disabled:cursor-not-allowed disabled:opacity-50 disabled:hover:translate-y-0"
            >
              {t('chat.send')}
            </button>
          </form>
        </>
      )}
    </div>
  );
}
