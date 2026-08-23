import { useEffect, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Client, type IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { getMessages, getPresence, type ChatMessage, type Ticket, type TicketStatus } from '../services/api';
import { useChangeTicketStatus, useMarkTicketRead, useUnreadCounts } from '../hooks/useData';
import { decodeJwt } from '../lib/jwt';
import { STATUS_STYLES, TRANSITIONS } from './ticketUi';

interface Props {
  token: string;
  tickets: Ticket[];
  /** Ticket a mostrar al llegar desde el boton "Abrir chat" del detalle */
  focusTicketId?: number | null;
}

export default function ChatPanel({ token, tickets, focusTicketId }: Props) {
  const { t, i18n } = useTranslation();
  const payload = decodeJwt(token);
  const tenantId = payload.tenantId;
  const userId = Number(payload.userId);
  const role = payload.role;

  const [selectedId, setSelectedId] = useState<number | null>(tickets[0]?.id ?? null);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [input, setInput] = useState('');
  const [connected, setConnected] = useState(false);
  const [onlineIds, setOnlineIds] = useState<number[]>([]);
  const [error, setError] = useState('');

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
    getMessages(token, selectedId)
      .then(setMessages)
      .catch((err) => setError(err.message));
    getPresence(token, selectedId).then(setOnlineIds).catch(() => {});
  }, [selectedId, token]);

  useEffect(() => {
    if (!tenantId) return;

    const client = new Client({
      webSocketFactory: () => new SockJS('http://localhost:8080/ws'),
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 3000,
      onConnect: () => {
        setConnected(true);
        client.subscribe(`/topic/presence/${tenantId}`, (m: IMessage) => {
          try {
            const data = JSON.parse(m.body);
            setOnlineIds(Array.isArray(data.online) ? data.online : []);
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

  useEffect(() => {
    endRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages.length]);

  const handleSend = (e: React.FormEvent) => {
    e.preventDefault();
    const content = input.trim();
    if (!content || !selectedId) return;
    if (!clientRef.current?.connected) {
      setError('Sin conexion con el servidor de chat');
      return;
    }
    clientRef.current.publish({
      destination: `/app/chat/${selectedId}`,
      body: JSON.stringify({ content }),
    });
    setInput('');
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
          <span className="text-slate-400">· {t('chat.online')}: {onlineIds.length}</span>
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

          <div className="h-80 space-y-2 overflow-y-auto rounded-xl border border-slate-200/70 bg-slate-50/70 p-4 dark:border-white/10 dark:bg-black/20">
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
                    <p className="whitespace-pre-wrap break-words">{m.content}</p>
                    <p className={`mt-1 text-[10px] ${mine ? 'text-white/70' : 'text-slate-400'}`}>
                      {new Date(m.sentAt).toLocaleTimeString(i18n.language)}
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

          <form onSubmit={handleSend} className="mt-3 flex gap-2">
            <input
              type="text"
              value={input}
              onChange={(e) => setInput(e.target.value)}
              placeholder={connected ? t('chat.placeholder') : t('chat.waitingConnection')}
              disabled={!connected}
              className="flex-1 rounded-xl border border-slate-300 bg-white px-3.5 py-2.5 text-sm outline-none transition-shadow placeholder:text-slate-400 focus:border-brand-400 focus:ring-4 focus:ring-brand-500/15 disabled:cursor-not-allowed disabled:bg-slate-100 disabled:opacity-60 dark:border-white/10 dark:bg-white/5 dark:text-white dark:disabled:bg-white/[0.02]"
            />
            <button
              type="submit"
              disabled={!connected || !input.trim()}
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
