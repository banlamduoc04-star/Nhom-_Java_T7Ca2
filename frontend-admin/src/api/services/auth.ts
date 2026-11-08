import { api, data, tokenStore } from '../client'
import type { Me } from '../types'

export const authApi = {
  async login(identifier: string, password: string) {
    const tokens = await data(api.post<{ accessToken: string; refreshToken: string }>('/auth/login', { identifier, password }))
    tokenStore.set(tokens)
  },
  register: (body: { storeName: string; fullName: string; email: string; phone?: string; password: string }) =>
    data(api.post('/auth/register', body)),
  async logout() {
    const tokens = tokenStore.get()
    if (tokens) {
      await api.post('/auth/logout', { refreshToken: tokens.refreshToken }).catch(() => undefined)
    }
    tokenStore.clear()
  },
   me: () => data(api.get<Me>('/me')),
  changePassword: (currentPassword: string, newPassword: string) =>
    data(api.put('/me/password', { currentPassword, newPassword })),
}
