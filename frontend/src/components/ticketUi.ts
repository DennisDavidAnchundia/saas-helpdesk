import type { TicketPriority, TicketStatus } from '../services/api';

/**
 * Codigo unico de la verdad: maquina de estados del ticket,
 * espejo exacto del backend (OPEN -> IN_PROGRESS -> RESOLVED -> CLOSED, REOPENED desde RESOLVED/CLOSED).
 * Lo consumen la lista y el detalle; si cambia en Java, cambia aqui.
 */
export const TRANSITIONS: Record<TicketStatus, TicketStatus[]> = {
  OPEN: ['IN_PROGRESS', 'RESOLVED'],
  IN_PROGRESS: ['RESOLVED'],
  RESOLVED: ['CLOSED', 'REOPENED'],
  CLOSED: ['REOPENED'],
  REOPENED: ['IN_PROGRESS', 'RESOLVED'],
};

export const STATUS_STYLES: Record<TicketStatus, string> = {
  OPEN: 'bg-sky-100 text-sky-700 dark:bg-sky-500/15 dark:text-sky-300',
  IN_PROGRESS: 'bg-amber-100 text-amber-700 dark:bg-amber-500/15 dark:text-amber-300',
  RESOLVED: 'bg-emerald-100 text-emerald-700 dark:bg-emerald-500/15 dark:text-emerald-300',
  CLOSED: 'bg-slate-200 text-slate-600 dark:bg-white/10 dark:text-slate-400',
  REOPENED: 'bg-violet-100 text-violet-700 dark:bg-violet-500/15 dark:text-violet-300',
};

export const PRIORITY_STYLES: Record<TicketPriority, string> = {
  LOW: 'bg-slate-100 text-slate-600 dark:bg-white/10 dark:text-slate-400',
  MEDIUM: 'bg-yellow-100 text-yellow-700 dark:bg-yellow-500/15 dark:text-yellow-300',
  HIGH: 'bg-orange-100 text-orange-700 dark:bg-orange-500/15 dark:text-orange-300',
  URGENT: 'bg-red-100 text-red-700 dark:bg-red-500/15 dark:text-red-300',
};

export const ALL_STATUSES: (TicketStatus | '')[] = ['', 'OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED', 'REOPENED'];
export const ALL_PRIORITIES: (TicketPriority | '')[] = ['', 'LOW', 'MEDIUM', 'HIGH', 'URGENT'];
