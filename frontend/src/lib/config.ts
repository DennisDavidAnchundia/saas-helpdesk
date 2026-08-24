/**
 * URL base del backend.
 * - Dev: http://localhost:8080 (fallback)
 * - Prod: string vacio (mismo origen; el proxy sirve /api, /ws y /actuator)
 */
export const API_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8080';
