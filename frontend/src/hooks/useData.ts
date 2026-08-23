import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '../services/apiClient';
import type {
  AgentInfo,
  Article,
  CreateUserRequest,
  DashboardSummary,
  SlaPolicy,
  Ticket,
  TicketPriority,
  TicketStatus,
  UserInfo,
} from '../services/api';

export const queryKeys = {
  tickets: ['tickets'] as const,
  ticket: (id: number) => ['ticket', id] as const,
  agents: ['users', 'agents'] as const,
  users: ['users', 'list'] as const,
  slaPolicy: ['tenants', 'sla'] as const,
  articles: (q?: string) => ['articles', q ?? ''] as const,
  dashboardSummary: ['dashboard', 'summary'] as const,
};

/** Invalida listados paginados + detalles tras un cambio de estado/asignacion. */
function invalidateTicketQueries(qc: ReturnType<typeof useQueryClient>) {
  qc.invalidateQueries({ queryKey: queryKeys.tickets });
  qc.invalidateQueries({ queryKey: ['ticket'] });
}

// ===================== Tickets =====================

export interface TicketFilters {
  status?: TicketStatus;
  priority?: TicketPriority;
  page?: number;
  size?: number;
}

export interface TicketPage {
  content: Ticket[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export function useTickets(filters: TicketFilters = {}) {
  const { status, priority, page, size } = filters;
  return useQuery({
    queryKey: [...queryKeys.tickets, status ?? '', priority ?? '', page ?? 0, size ?? ''] as const,
    queryFn: async () => {
      const { data } = await apiClient.get<TicketPage>('/tickets', {
        params: { status, priority, page, size },
      });
      return data;
    },
    placeholderData: (prev) => prev,
  });
}

export function useCreateTicket() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (payload: { title: string; description: string; priority: TicketPriority }) => {
      const { data } = await apiClient.post<Ticket>('/tickets', payload);
      return data;
    },
    onSuccess: () => invalidateTicketQueries(qc),
  });
}

/** Detalle de un ticket concreto (enabled=false mientras no haya seleccion). */
export function useTicket(id: number | null) {
  return useQuery({
    queryKey: queryKeys.ticket(id ?? 0),
    queryFn: async () => {
      const { data } = await apiClient.get<Ticket>(`/tickets/${id}`);
      return data;
    },
    enabled: id !== null,
  });
}

export function useAgents(enabled = true) {
  return useQuery({
    queryKey: queryKeys.agents,
    queryFn: async () => {
      const { data } = await apiClient.get<AgentInfo[]>('/users/agents');
      return data;
    },
    enabled,
  });
}

export function useChangeTicketStatus() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async ({ id, status }: { id: number; status: TicketStatus }) => {
      const { data } = await apiClient.patch<Ticket>(`/tickets/${id}/status?status=${status}`);
      return data;
    },
    onSuccess: () => invalidateTicketQueries(qc),
  });
}

export function useAssignAgent() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async ({ id, agentId }: { id: number; agentId: number }) => {
      const { data } = await apiClient.patch<Ticket>(`/tickets/${id}/assign/${agentId}`);
      return data;
    },
    onSuccess: () => invalidateTicketQueries(qc),
  });
}

/** Round-robin: el backend elige al agente con menos tickets activos. */
export function useAutoAssignAgent() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (id: number) => {
      const { data } = await apiClient.patch<Ticket>(`/tickets/${id}/assign`);
      return data;
    },
    onSuccess: () => invalidateTicketQueries(qc),
  });
}

// ===================== Admin: usuarios y SLA =====================

export function useUsers(enabled = true) {
  return useQuery({
    queryKey: queryKeys.users,
    queryFn: async () => {
      const { data } = await apiClient.get<UserInfo[]>('/users');
      return data;
    },
    enabled,
  });
}

export function useCreateUser() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (payload: CreateUserRequest) => {
      const { data } = await apiClient.post<UserInfo>('/users', payload);
      return data;
    },
    // El nuevo agente debe aparecer en el listado y ser asignable al toque
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: queryKeys.users });
      qc.invalidateQueries({ queryKey: queryKeys.agents });
    },
  });
}

export function useChangeOwnPassword() {
  return useMutation({
    mutationFn: async ({ currentPassword, newPassword }: { currentPassword: string; newPassword: string }) => {
      await apiClient.patch('/auth/password', { currentPassword, newPassword });
      return true;
    },
  });
}

export function useResetUserPassword() {
  return useMutation({
    mutationFn: async ({ id, newPassword }: { id: number; newPassword: string }) => {
      await apiClient.patch(`/users/${id}/password`, { newPassword });
      return true;
    },
  });
}

export function useSetUserActive() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async ({ id, isActive }: { id: number; isActive: boolean }) => {
      const { data } = await apiClient.patch<UserInfo>(`/users/${id}/active`, {
        isActive,
      });
      return data;
    },
    // Al desactivar un agente cambia tanto el listado como los agentes asignables
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: queryKeys.users });
      qc.invalidateQueries({ queryKey: queryKeys.agents });
    },
  });
}

export function useSlaPolicy(enabled = true) {
  return useQuery({
    queryKey: queryKeys.slaPolicy,
    queryFn: async () => {
      const { data } = await apiClient.get<SlaPolicy>('/tenants/sla');
      return data;
    },
    enabled,
  });
}

export function useUpdateSlaPolicy() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (payload: Partial<SlaPolicy>) => {
      const { data } = await apiClient.put<SlaPolicy>('/tenants/sla', payload);
      return data;
    },
    onSuccess: (saved) => qc.setQueryData(queryKeys.slaPolicy, saved),
  });
}

// ===================== Articulos =====================

export function useArticles(query?: string) {
  return useQuery({
    queryKey: queryKeys.articles(query),
    queryFn: async () => {
      const { data } = await apiClient.get<Article[]>('/articles', {
        params: query ? { q: query } : undefined,
      });
      return data;
    },
  });
}

interface ArticlePayload {
  title?: string;
  content?: string;
  category?: string;
  isPublished?: boolean;
}

export function useCreateArticle() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (payload: ArticlePayload) => {
      const { data } = await apiClient.post<Article>('/articles', payload);
      return data;
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: ['articles'] }),
  });
}

export function useUpdateArticle() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async ({ id, ...payload }: ArticlePayload & { id: number }) => {
      const { data } = await apiClient.put<Article>(`/articles/${id}`, payload);
      return data;
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: ['articles'] }),
  });
}

export function useDeleteArticle() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (id: number) => {
      await apiClient.delete(`/articles/${id}`);
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: ['articles'] }),
  });
}

// ===================== Billing =====================

export interface BillingInfo {
  plan: 'FREE' | 'PRO' | 'ENTERPRISE';
  status: string;
  ticketsUsed: number;
  ticketsLimit: number;
  currentPeriodStart?: string;
  currentPeriodEnd?: string;
}

export function useBillingMe() {
  return useQuery({
    queryKey: ['billing', 'me'],
    queryFn: async () => {
      const { data } = await apiClient.get<BillingInfo>('/billing/me');
      return data;
    },
  });
}

export function useCheckoutSession() {
  return useMutation({
    mutationFn: async (targetPlan: 'PRO' | 'ENTERPRISE') => {
      const { data } = await apiClient.post<{ url: string }>('/billing/checkout-session', {
        targetPlan,
      });
      return data;
    },
  });
}

// ===================== Dashboard =====================

export function useDashboardSummary() {
  return useQuery({
    queryKey: queryKeys.dashboardSummary,
    queryFn: async () => {
      const { data } = await apiClient.get<DashboardSummary>('/dashboard/summary');
      return data;
    },
  });
}
