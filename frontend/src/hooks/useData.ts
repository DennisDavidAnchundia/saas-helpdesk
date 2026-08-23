import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '../services/apiClient';
import type {
  Article,
  DashboardSummary,
  Ticket,
  TicketPriority,
  TicketStatus,
} from '../services/api';

export const queryKeys = {
  tickets: ['tickets'] as const,
  articles: (q?: string) => ['articles', q ?? ''] as const,
  dashboardSummary: ['dashboard', 'summary'] as const,
};

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
    onSuccess: () => qc.invalidateQueries({ queryKey: queryKeys.tickets }),
  });
}

export function useChangeTicketStatus() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async ({ id, status }: { id: number; status: TicketStatus }) => {
      const { data } = await apiClient.patch<Ticket>(`/tickets/${id}/status?status=${status}`);
      return data;
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: queryKeys.tickets }),
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
