import { API_URL } from '../lib/config';

const API = API_URL;

export interface LoginRequest {
  email: string;
  password: string;
  tenantSlug: string;
}

export interface RegisterRequest {
  fullName: string;
  email: string;
  password: string;
  tenantName: string;
}

export interface AuthResponse {
  token: string;
  userId: number;
  email: string;
  fullName: string;
  role: string;
  tenantId: number;
  tenantName: string;
  tenantSlug?: string;
  tokenType?: string;
}

export interface TestResponse {
  message: string;
  userId: number;
  tenantId: number;
  email: string;
  role: string;
}

export type TicketStatus = 'OPEN' | 'IN_PROGRESS' | 'RESOLVED' | 'CLOSED' | 'REOPENED';
export type TicketPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';

export interface AgentInfo {
  id: number;
  fullName: string;
  email: string;
}

export type UserRole = 'ADMIN' | 'AGENT' | 'CUSTOMER';

export interface CreateUserRequest {
  fullName: string;
  email: string;
  password: string;
}

export interface UserInfo {
  id: number;
  fullName: string;
  email: string;
  role: UserRole;
  active: boolean;
  provider?: string;
  createdAt?: string;
}

export interface SlaPolicy {
  urgentHours: number;
  highHours: number;
  mediumHours: number;
  lowHours: number;
}

export interface Ticket {
  id: number;
  title: string;
  description: string;
  status: TicketStatus;
  priority: TicketPriority;
  customerId: number;
  customerName: string;
  agentId: number | null;
  agentName: string | null;
  firstResponseAt: string | null;
  resolvedAt: string | null;
  closedAt: string | null;
  slaDueAt: string | null;
  slaBreached: boolean | null;
  createdAt: string;
  updatedAt: string;
}

export async function listTickets(token: string): Promise<Ticket[]> {
  const res = await fetch(`${API}/api/tickets`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!res.ok) throw new Error('Error al cargar tickets');
  return res.json();
}

export async function createTicket(
  token: string,
  data: { title: string; description: string; priority: TicketPriority }
): Promise<Ticket> {
  const res = await fetch(`${API}/api/tickets`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
    body: JSON.stringify(data),
  });
  if (!res.ok) {
    const err = await res.json().catch(() => ({ message: res.statusText }));
    throw new Error(err.message || 'Error al crear ticket');
  }
  return res.json();
}

export async function changeTicketStatus(token: string, id: number, status: TicketStatus): Promise<Ticket> {
  const res = await fetch(`${API}/api/tickets/${id}/status?status=${status}`, {
    method: 'PATCH',
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!res.ok) {
    const err = await res.json().catch(() => ({ message: res.statusText }));
    throw new Error(err.message || 'Transicion invalida');
  }
  return res.json();
}

export async function register(data: RegisterRequest): Promise<AuthResponse> {
  const res = await fetch(`${API}/api/auth/register`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  });
  if (!res.ok) {
    const err = await res.json().catch(() => ({ message: res.statusText }));
    throw new Error(err.message || 'Error en registro');
  }
  return res.json();
}

export interface JoinRequest {
  fullName: string;
  email: string;
  password: string;
  tenantSlug: string;
}

export interface JoinResponse {
  userId: number;
  email: string;
  fullName: string;
  role: string;
  tenantName: string;
  tenantSlug: string;
}

export async function join(data: JoinRequest): Promise<JoinResponse> {
  const res = await fetch(`${API}/api/auth/join`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  });
  if (!res.ok) {
    const err = await res.json().catch(() => ({ message: res.statusText }));
    throw new Error(err.message || 'Error al unirse');
  }
  return res.json();
}

export async function login(data: LoginRequest): Promise<AuthResponse> {
  const res = await fetch(`${API}/api/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  });
  if (!res.ok) {
    const err = await res.json().catch(() => ({ message: res.statusText }));
    throw new Error(err.message || 'Error en login');
  }
  return res.json();
}

export async function testEndpoint(
  path: string,
  token: string
): Promise<{ status: number; data: TestResponse | { error: string; message: string } | null }> {
  const res = await fetch(`${API}${path}`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  const data = await res.json().catch(() => null);
  return { status: res.status, data };
}

export function googleLoginUrl(): string {
  return `${API}/oauth2/authorization/google`;
}

// ---------- Knowledge Base ----------

export interface Article {
  id: number;
  title: string;
  content: string;
  category: string | null;
  isPublished: boolean;
  authorId: number | null;
  authorName: string | null;
  createdAt: string;
  updatedAt: string;
}

export async function listArticles(token: string, q?: string): Promise<Article[]> {
  const url = q ? `${API}/api/articles?q=${encodeURIComponent(q)}` : `${API}/api/articles`;
  const res = await fetch(url, { headers: { Authorization: `Bearer ${token}` } });
  if (!res.ok) throw new Error('Error al cargar articulos');
  return res.json();
}

export async function createArticle(
  token: string,
  data: { title: string; content: string; category?: string; isPublished?: boolean }
): Promise<Article> {
  const res = await fetch(`${API}/api/articles`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
    body: JSON.stringify(data),
  });
  if (!res.ok) {
    const err = await res.json().catch(() => ({ message: res.statusText }));
    throw new Error(err.message || JSON.stringify(err.errors) || 'Error al crear articulo');
  }
  return res.json();
}

export async function updateArticle(
  token: string,
  id: number,
  data: { title?: string; content?: string; category?: string; isPublished?: boolean }
): Promise<Article> {
  const res = await fetch(`${API}/api/articles/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
    body: JSON.stringify(data),
  });
  if (!res.ok) throw new Error('Error al actualizar articulo');
  return res.json();
}

export async function deleteArticle(token: string, id: number): Promise<void> {
  const res = await fetch(`${API}/api/articles/${id}`, {
    method: 'DELETE',
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!res.ok) throw new Error('Error al borrar articulo');
}

// ---------- Chat ----------

export interface ChatAttachment {
  id: number;
  fileName: string;
  contentType: string | null;
  sizeBytes: number;
}

export interface ChatMessage {
  id: number;
  ticketId: number;
  senderId: number | null;
  senderName: string | null;
  content: string;
  sentAt: string;
  attachment?: ChatAttachment | null;
}

export interface SendChatPayload {
  content: string;
  attachmentId?: number;
}

export interface ChatMessagePage {
  content: ChatMessage[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

/** Historial paginado: la pagina 0 trae los mensajes mas recientes. */
export async function getMessages(
  token: string,
  ticketId: number,
  page = 0,
  size = 50,
): Promise<ChatMessagePage> {
  const res = await fetch(`${API}/api/tickets/${ticketId}/messages?page=${page}&size=${size}`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!res.ok) {
    const err = await res.json().catch(() => ({ message: res.statusText }));
    throw new Error(err.message || 'Error al cargar mensajes');
  }
  return res.json();
}

export interface OnlineUser {
  id: number;
  fullName: string;
  role: string;
}

export async function getPresence(token: string, ticketId: number): Promise<OnlineUser[]> {
  const res = await fetch(`${API}/api/tickets/${ticketId}/presence`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!res.ok) return [];
  const data = await res.json();
  return data.online ?? [];
}

/** Mapa ticketId -> mensajes no leidos para el usuario del token. */
export async function getUnreadCounts(token: string): Promise<Record<string, number>> {
  const res = await fetch(`${API}/api/tickets/unread`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!res.ok) return {};
  return res.json();
}

export async function markTicketRead(token: string, ticketId: number): Promise<void> {
  await fetch(`${API}/api/tickets/${ticketId}/read`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${token}` },
  });
}

export interface AgentStat {
  agentName: string;
  assignedTickets: number;
}

export interface DashboardSummary {
  totalTickets: number;
  ticketsByStatus: Record<string, number>;
  avgResolutionSeconds: number | null;
  avgFirstResponseSeconds: number | null;
  slaBreachedCount: number;
  topAgents: AgentStat[];
}

export async function getDashboardSummary(token: string): Promise<DashboardSummary> {
  const res = await fetch(`${API}/api/dashboard/summary`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!res.ok) {
    const err = await res.json().catch(() => ({ message: res.statusText }));
    throw new Error(err.message || 'Error al cargar métricas');
  }
  return res.json();
}
