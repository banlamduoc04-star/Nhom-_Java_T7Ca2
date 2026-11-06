import { api, data } from '../client'
import type { Branch, TenantSettings, UserView } from '../types'

export const branchApi = {
  list: () => data(api.get<Branch[]>('/branches')),
}

export const userAdminApi = {
  list: () => data(api.get<UserView[]>('/users')),
  create: (body: { email: string; fullName: string; phone?: string; password: string; role: 'STAFF' | 'CASHIER'; branchId?: string }) =>
    data(api.post<UserView>('/users', body)),
  update: (id: string, body: { fullName: string; phone?: string; role: 'STAFF' | 'CASHIER'; branchId?: string }) =>
    data(api.put<UserView>(`/users/${id}`, body)),
  setStatus: (id: string, active: boolean) => data(api.patch<UserView>(`/users/${id}/status`, { active })),
  resetPassword: (id: string, password: string) => data(api.put(`/users/${id}/password`, { password })),
}

export const branchAdminApi = {
  list: () => data(api.get<Branch[]>('/branches')),
  create: (body: { name: string; type: 'STORE' | 'WAREHOUSE'; address?: string }) =>
    data(api.post<Branch>('/branches', body)),
  update: (id: string, body: { name: string; type: 'STORE' | 'WAREHOUSE'; address?: string }) =>
    data(api.put<Branch>(`/branches/${id}`, body)),
  setStatus: (id: string, active: boolean) => data(api.patch<Branch>(`/branches/${id}/status`, { active })),
}

export const settingsApi = {
  get: () => data(api.get<TenantSettings>('/settings')),
  update: (reservationMinutes: number) =>
    data(api.put<TenantSettings>('/settings', { reservationMinutes })),
}
