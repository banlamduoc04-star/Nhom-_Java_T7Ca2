import axios from 'axios'
import { API_URL, api, data } from '../client'
import type { OrderChannel, OrderStatus, OrderView, Page, ShippingSlip, SimulationResult, WebhookChannel } from '../types'

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
  /** FR-ORD-03: du lieu in phieu giao hang */
  shippingSlip: (id: string) => data(api.get<ShippingSlip>(`/orders/${id}/shipping-slip`)),
}

export const simulatorApi = {
  simulate: (body: { branchId: string; channel: OrderChannel; skuId: string; quantity: number; orderCount: number }) =>
    data(api.post<SimulationResult>('/simulator/orders', body)),
  /**
   * Goi webhook nhu mot san that (FR-SIM-01): chi gui X-Api-Key, khong gui JWT.
   * Dung axios rieng de 401 OD40101 (key sai) khong kich hoat lam moi token / dang xuat cua Admin.
   */
  sendWebhook: (channel: WebhookChannel, apiKey: string, body: { branchId: string; externalOrderId: string; customerName?: string; customerPhone?: string; shippingAddress?: string; items: { skuId: string; quantity: number }[] }) =>
    data(axios.post<OrderView>(`${API_URL}/webhooks/${channel.toLowerCase()}`, body, { headers: { 'X-Api-Key': apiKey } })),
}
