import { useEffect, useRef, useState } from 'react';
import { Client, type IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { getMessages, getPresence, type ChatMessage, type Ticket } from '../services/api';

interface Props {
  token: string;
  tickets: Ticket[];
}

function decodeJwt(token: string): { tenantId?: number; userId?: number } {
  try {
    return JSON.parse(atob(token.split('.')[1]));
  } catch {
    return {};
  }
}

export default function ChatPanel({ token, tickets }: Props) {
  const payload = decodeJwt(token);
  const tenantId = payload.tenantId;
  const userId = Number(payload.userId);

  const [selectedId, setSelectedId] = useState<number | null>(tickets[0]?.id ?? null);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [input, setInput] = useState('');
  const [connected, setConnected] = useState(false);
  const [onlineIds, setOnlineIds] = useState<number[]>([]);
  const [error, setError] = useState('');

  const clientRef = useRef<Client | null>(null);
  const subRef = useRef<{ unsubscribe: () => void } | null>(null);
  const selectedRef = useRef<number | null>(selectedId);
  const endRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    selectedRef.current = selectedId;
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
          Chat del ticket
        </h2>
        <div className="flex items-center gap-2 text-xs">
          <span
            className={`size-2 rounded-full ${
              connected ? 'animate-pulse-dot bg-emerald-500' : 'bg-red-500'
            }`}
          />
          <span className={connected ? 'font-medium text-emerald-600 dark:text-emerald-400' : 'font-medium text-red-500'}>
            {connected ? 'Conectado' : 'Desconectado'}
          </span>
          <span className="text-slate-400">· En línea: {onlineIds.length}</span>
        </div>
      </div>

      {tickets.length === 0 ? (
        <p className="py-8 text-center text-sm text-slate-400">
          Crea un ticket primero para poder chatear.
        </p>
      ) : (
        <>
          <select
            value={selectedId ?? ''}
            onChange={(e) => setSelectedId(Number(e.target.value))}
            className="mb-4 w-full cursor-pointer rounded-xl border border-slate-300 bg-white px-3 py-2.5 text-sm outline-none transition-shadow focus:border-brand-400 focus:ring-4 focus:ring-brand-500/15 dark:border-white/10 dark:bg-white/5 dark:text-white"
          >
            {tickets.map((t) => (
              <option key={t.id} value={t.id}>
                #{t.id} · {t.title} ({t.status})
              </option>
            ))}
          </select>

          {error && (
            <div className="mb-3 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 dark:border-red-500/20 dark:bg-red-500/10 dark:text-red-300">
              {error}
            </div>
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
                      {new Date(m.sentAt).toLocaleTimeString('es')}
                    </p>
                  </div>
                </div>
              );
            })}
            {messages.length === 0 && (
              <p className="py-10 text-center text-xs text-slate-400">Sin mensajes todavía.</p>
            )}
            <div ref={endRef} />
          </div>

          <form onSubmit={handleSend} className="mt-3 flex gap-2">
            <input
              type="text"
              value={input}
              onChange={(e) => setInput(e.target.value)}
              placeholder={connected ? 'Escribe un mensaje…' : 'Esperando conexión…'}
              disabled={!connected}
              className="flex-1 rounded-xl border border-slate-300 bg-white px-3.5 py-2.5 text-sm outline-none transition-shadow placeholder:text-slate-400 focus:border-brand-400 focus:ring-4 focus:ring-brand-500/15 disabled:cursor-not-allowed disabled:bg-slate-100 disabled:opacity-60 dark:border-white/10 dark:bg-white/5 dark:text-white dark:disabled:bg-white/[0.02]"
            />
            <button
              type="submit"
              disabled={!connected || !input.trim()}
              className="cursor-pointer rounded-xl bg-gradient-to-r from-brand-500 to-violet-500 px-5 py-2.5 text-sm font-semibold text-white shadow-lg shadow-brand-500/25 transition-all duration-200 hover:-translate-y-0.5 hover:shadow-xl hover:shadow-brand-500/30 disabled:cursor-not-allowed disabled:opacity-50 disabled:hover:translate-y-0"
            >
              Enviar
            </button>
          </form>
        </>
      )}
    </div>
  );
}
