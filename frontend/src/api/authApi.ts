import api from './axios';
import type { LoginRequest, TokenResponse } from '../types';

export const authApi = {
  login: (data: LoginRequest) =>
    api.post<TokenResponse>('/auth/login', data).then((r) => r.data),

  refresh: (refreshToken: string) =>
    api.post<TokenResponse>('/auth/refresh', { refreshToken }).then((r) => r.data),
};
