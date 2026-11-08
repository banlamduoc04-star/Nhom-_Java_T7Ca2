import { websocketUrl } from './api'

export interface NewOrderNotice {
  orderCode?: string
  totalAmount?: number
}

/** Native WebSocket STOMP client for the backend's /ws endpoint. */
export function subscribeToNewOrders(
  tenantId: string,
  getAccessToken: () => string | null,
  onOrder: (event: NewOrderNotice) => void,
  onError?: () => void,
): () => void {
  let socket: WebSocket | null = null
  let reconnectTimer: number | undefined
  let stopped = false
  let connected = false
  let buffer = ''
  const subscriptionId = 'pos-new-orders'

  const sendFrame = (command: string, headers: Record<string, string> = {}, body = '') => {
    if (!socket || socket.readyState !== WebSocket.OPEN) return
    const headerText = Object.entries(headers).map(([key, value]) => `${key}:${value}`).join('\n')
    socket.send(`${command}\n${headerText}\n\n${body}\0`)
  }

  const processFrames = () => {
    let boundary = buffer.indexOf('\0')
    while (boundary >= 0) {
      const raw = buffer.slice(0, boundary)
      buffer = buffer.slice(boundary + 1)
      const frame = raw.replace(/^\n+/, '')
      const separator = frame.indexOf('\n\n')
      if (separator >= 0) {
        const headerLines = frame.slice(0, separator).split('\n')
        const command = headerLines.shift()
        const body = frame.slice(separator + 2)
        if (command === 'CONNECTED') {
          connected = true
          sendFrame('SUBSCRIBE', {
            id: subscriptionId,
            destination: `/topic/tenants/${tenantId}/orders/new`,
            ack: 'auto',
          })
        } else if (command === 'MESSAGE') {
          try { onOrder(JSON.parse(body) as NewOrderNotice) } catch { onError?.() }
        } else if (command === 'ERROR') {
          onError?.()
        }
      }
      boundary = buffer.indexOf('\0')
    }
  }

  const connect = () => {
    if (stopped) return
    try {
      const accessToken = getAccessToken()
      if (!accessToken) {
        if (!stopped) reconnectTimer = window.setTimeout(connect, 5000)
        return
      }
      buffer = ''
      socket = new WebSocket(websocketUrl(), ['v12.stomp'])
      socket.onopen = () => {
        const headers = [
          'accept-version:1.2',
          'host:oism',
          `Authorization:Bearer ${accessToken}`,
          '',
          '',
        ].join('\n')
        socket?.send(`CONNECT\n${headers}\0`)
      }
      socket.onmessage = (event) => {
        buffer += typeof event.data === 'string' ? event.data : ''
        processFrames()
      }
      socket.onerror = () => onError?.()
      socket.onclose = () => {
        connected = false
        if (!stopped) reconnectTimer = window.setTimeout(connect, 5000)
      }
    } catch {
      onError?.()
      if (!stopped) reconnectTimer = window.setTimeout(connect, 5000)
    }
  }

  connect()
  return () => {
    stopped = true
    if (reconnectTimer !== undefined) window.clearTimeout(reconnectTimer)
    if (socket?.readyState === WebSocket.OPEN && connected) {
      sendFrame('UNSUBSCRIBE', { id: subscriptionId })
      sendFrame('DISCONNECT', { receipt: 'pos-disconnect' })
    }
    socket?.close()
  }
}
