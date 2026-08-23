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
    <div className="bg-white rounded-xl shadow-lg border border-slate-200 p-6">
      <div className="flex flex-wrap items-center gap-3 justify-between mb-4">
        <h2 className="text-lg font-semibold text-slate-800">Chat del Ticket</h2>
        <div className="flex items-center gap-2 text-xs">
          <span className={`w-2 h-2 rounded-full ${connected ? 'bg-green-500' : 'bg-red-500'}`} />
          <span className={connected ? 'text-green-600' : 'text-red-500'}>
            {connected ? 'Conectado' : 'Desconectado'}
          </span>
          <span className="text-slate-400">· En linea: {onlineIds.length}</span>
        </div>
      </div>

      {tickets.length === 0 ? (
        <p className="text-sm text-slate-400 text-center py-6">
          Crea un ticket primero para poder chatear.
        </p>
      ) : (
        <>
          <select
            value={selectedId ?? ''}
            onChange={(e) => setSelectedId(Number(e.target.value))}
            className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm bg-white mb-4"
          >
            {tickets.map((t) => (
              <option key={t.id} value={t.id}>
                #{t.id} · {t.title} ({t.status})
              </option>
            ))}
          </select>

          {error && (
            <div className="bg-red-50 text-red-700 text-sm p-3 rounded-lg border border-red-200 mb-3">
              {error}
            </div>
          )}

          <div className="h-72 overflow-y-auto border border-slate-200 rounded-lg p-3 space-y-2 bg-slate-50">
            {messages.map((m) => {
              const mine = m.senderId === userId;
              return (
                <div key={m.id} className={`flex ${mine ? 'justify-end' : 'justify-start'}`}>
                  <div
                    className={`max-w-[75%] px-3 py-2 rounded-xl text-sm ${
                      mine
                        ? 'bg-blue-600 text-white rounded-br-sm'
                        : 'bg-white border border-slate-200 text-slate-800 rounded-bl-sm'
                    }`}
                  >
                    {!mine && (
                      <p className="text-xs font-semibold text-indigo-600 mb-0.5">{m.senderName}</p>
                    )}
                    <p className="whitespace-pre-wrap break-words">{m.content}</p>
                    <p className={`text-[10px] mt-1 ${mine ? 'text-blue-200' : 'text-slate-400'}`}>
                      {new Date(m.sentAt).toLocaleTimeString('es')}
                    </p>
                  </div>
                </div>
              );
            })}
            {messages.length === 0 && (
              <p className="text-xs text-slate-400 text-center py-8">Sin mensajes todavia.</p>
            )}
            <div ref={endRef} />
          </div>

          <form onSubmit={handleSend} className="flex gap-2 mt-3">
            <input
              type="text"
              value={input}
              onChange={(e) => setInput(e.target.value)}
              placeholder={connected ? 'Escribe un mensaje...' : 'Esperando conexion...'}
              disabled={!connected}
              className="flex-1 px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-blue-500 outline-none text-sm disabled:bg-slate-100"
            />
            <button
              type="submit"
              disabled={!connected || !input.trim()}
              className="px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-lg hover:bg-blue-700 transition-colors disabled:opacity-50"
            >
              Enviar
            </button>
          </form>
        </>
      )}
    </div>
  );
}
