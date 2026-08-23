/** Decodifica el payload de un JWT sin validar firma (solo para leer claims en el cliente). */
export function decodeJwt(token: string): {
  tenantId?: number;
  userId?: number;
  role?: string;
  email?: string;
} {
  try {
    return JSON.parse(atob(token.split('.')[1]));
  } catch {
    return {};
  }
}
