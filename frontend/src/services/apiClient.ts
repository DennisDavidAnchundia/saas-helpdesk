import axios from 'axios';
import { API_URL } from '../lib/config';

/**
 * Cliente HTTP central:
 * - Inyecta el JWT en cada request
 * - Ante un 401 limpia la sesion y manda a /login
 */
export const apiClient = axios.create({
  baseURL: `${API_URL}/api`,
  timeout: 15000,
});

apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401 && !window.location.pathname.startsWith('/login')) {
      localStorage.clear();
      window.location.assign('/login');
    }
    const msg =
      error.response?.data?.message ||
      error.response?.data?.error ||
      error.message ||
      'Error de red';
    return Promise.reject(new Error(msg));
  },
);
