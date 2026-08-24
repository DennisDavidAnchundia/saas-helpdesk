/**
 * Convierte un timestamp del API a Date.
 *
 * El backend serializa LocalDateTime sin zona (ej: "2026-08-24T20:30:00") y
 * desde la migracion UTC esos valores son SIEMPRE hora UTC. Sin sufijo,
 * `new Date()` los interpretaria como hora local del navegador y mostraria
 * horas corridas. Agregar 'Z' le dice al navegador "esto es UTC" y el
 * toLocaleString final muestra la hora local correcta de cada usuario.
 */
export function apiDate(value: string): Date {
  const hasZone = /[zZ]$|[+-]\d{2}:?\d{2}$/.test(value);
  return new Date(hasZone ? value : `${value}Z`);
}
