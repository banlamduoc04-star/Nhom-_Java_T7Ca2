import { Client } from '@stomp/stompjs'
import { API_URL, tokenStore } from './api'

/** Khop domain/event/OrderCreatedEvent cua backend (kenh /topic/tenants/{tenantId}/orders). */
export interface OrderCreatedEvent {
  orderId: string
  orderCode: string
  totalAmount: number
  channel: string
  createdAt: string
}

/** Dia chi STOMP cua backend: cung host voi API, duong dan /ws (vd http://localhost:8080/api/v1 -> ws://localhost:8080/ws). */
export function websocketUrl(apiUrl: string = API_URL): string {
  const url = new URL(apiUrl, window.location.origin)
  url.protocol = url.protocol === 'https:' ? 'wss:' : 'ws:'
  url.pathname = '/ws'
  url.search = ''
  url.hash = ''
  return url.toString()
}

/**
 * FR-SIM-02 (Sprint 4, M7 viec 5): POS nhan thong bao don moi cua cung cua hang.
 * Backend bat buoc JWT luc CONNECT va chi cho nghe kenh cua tenant trong token.
 * Tra ve ham huy ket noi.
 */
export function subscribeNewOrders(tenantId: string, onOrder: (event: OrderCreatedEvent) => void): () => void {
  const client = new Client({
    brokerURL: websocketUrl(),
    beforeConnect: (c) => {
      c.connectHeaders = { Authorization: `Bearer ${tokenStore.get()?.accessToken ?? ''}` }
    },
    debug: () => {},
    reconnectDelay: 5000,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
  })
  client.onConnect = () => {
    client.subscribe(`/topic/tenants/${tenantId}/orders`, (message) => {
      try {
        onOrder(JSON.parse(message.body) as OrderCreatedEvent)
      } catch {
        // bo qua tin khong phai JSON
      }
    })
  }
  client.activate()
  return () => {
    void client.deactivate()
  }
}
