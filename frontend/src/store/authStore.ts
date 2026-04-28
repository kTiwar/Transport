import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import { jwtDecode } from '../utils/jwt';
import type { AuthUser } from '../types';

interface AuthState {
  accessToken: string | null;
  refreshToken: string | null;
  user: AuthUser | null;
  isAuthenticated: boolean;
  setTokens: (access: string, refresh: string) => void;
  logout: () => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      accessToken: null,
      refreshToken: null,
      user: null,
      isAuthenticated: false,

      setTokens: (access, refresh) => {
        localStorage.setItem('accessToken', access);
        localStorage.setItem('refreshToken', refresh);
        const decoded = jwtDecode(access);
        set({
          accessToken: access,
          refreshToken: refresh,
          user: {
            username: String(decoded?.sub ?? ''),
            roles: Array.isArray(decoded?.roles) ? (decoded.roles as string[]) : [],
          },
          isAuthenticated: true,
        });
      },

      logout: () => {
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
        set({ accessToken: null, refreshToken: null, user: null, isAuthenticated: false });
      },
    }),
    { name: 'tms-auth' }
  )
);
