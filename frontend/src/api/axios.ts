import axios from 'axios';

const BASE_URL: string = import.meta.env.VITE_API_URL ?? '/api/v1';

export const api = axios.create({
  baseURL: BASE_URL,
  timeout: 30_000,
  headers: { 'Content-Type': 'application/json' },
});

// ── Attach JWT on every request ──────────────────────────────────────────────
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// ── Auto-refresh on 401 ──────────────────────────────────────────────────────
let isRefreshing = false;
let queue: Array<(token: string) => void> = [];

api.interceptors.response.use(
  (res) => res,
  async (err) => {
    const original = err.config;

    if (err.response?.status === 401 && !original._retry) {
      original._retry = true;
      const refreshToken = localStorage.getItem('refreshToken');

      if (!refreshToken) {
        localStorage.clear();
        window.location.href = '/login';
        return Promise.reject(err);
      }

      if (isRefreshing) {
        return new Promise((resolve) => {
          queue.push((token) => {
            original.headers.Authorization = `Bearer ${token}`;
            resolve(api(original));
          });
        });
      }

      isRefreshing = true;
      try {
        const { data } = await axios.post(`${BASE_URL}/auth/refresh`, { refreshToken });
        localStorage.setItem('accessToken', data.accessToken);
        queue.forEach((cb) => cb(data.accessToken));
        queue = [];
        original.headers.Authorization = `Bearer ${data.accessToken}`;
        return api(original);
      } catch {
        localStorage.clear();
        window.location.href = '/login';
        return Promise.reject(err);
      } finally {
        isRefreshing = false;
      }
    }

    return Promise.reject(err);
  }
);

export default api;
