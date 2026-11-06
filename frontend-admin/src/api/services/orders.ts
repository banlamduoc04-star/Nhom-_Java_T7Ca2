import { api, data } from '../client'
import type { OrderChannel, OrderStatus, OrderView, Page, SimulationResult } from '../types'

export interface CreateOrderBody {
  branchId: string
  customerName?: string
  customerPhone?: string
  shippingAddress?: string
  lines: { skuId: string; quantity: number }[]
}

export const orderApi = {
  list: (params: { status?: OrderStatus; channel?: OrderChannel; branchId?: string; from?: string; to?: string; page?: number; size?: number }) =>
    data(api.get<Page<OrderView>>('/orders', { params })),
  get: (id: string) => data(api.get<OrderView>(`/orders/${id}`)),
  createDraft: (body: CreateOrderBody) => data(api.post<OrderView>('/orders', body)),
  createAndReserve: (body: CreateOrderBody & { channel: OrderChannel; externalOrderId?: string }) =>
    data(api.post<OrderView>('/orders/reserve', body)),
  reserve: (id: string) => data(api.post<OrderView>(`/orders/${id}/reserve`)),
  confirm: (id: string) => data(api.post<OrderView>(`/orders/${id}/confirm`)),
  complete: (id: string) => data(api.post<OrderView>(`/orders/${id}/complete`)),
  cancel: (id: string, reason: string) => data(api.post<OrderView>(`/orders/${id}/cancel`, { reason })),
}

export const simulatorApi = {
  simulate: (body: { branchId: string; channel: OrderChannel; orderCount: number; items: { skuId: string; quantity: number }[] }) =>
    data(api.post<SimulationResult>('/simulator/orders', body)),
  sendWebhook: (channel: string, body: { branchId: string; externalOrderId: string; customerName?: string; customerPhone?: string; shippingAddress?: string; items: { skuId: string; quantity: number }[] }) =>
    data(api.post<OrderView>(`/webhooks/${channel}`, body)),
}
