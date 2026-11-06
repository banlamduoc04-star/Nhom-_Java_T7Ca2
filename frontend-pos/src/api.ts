import axios from 'axios'
import type { SkuInfo } from './cart'

export const API_URL: string = import.meta.env.VITE_API_URL ?? 'http://localhost:8080/api/v1'
/** Dung du lieu gia (theo dac ta OpenAPI) khi backend chua san sang: VITE_USE_MOCK=true. */
export const USE_MOCK: boolean = import.meta.env.VITE_USE_MOCK === 'true'

export interface Me {
  userId: string
  tenantName: string
  fullName: string
  role: 'OWNER' | 'STAFF' | 'CASHIER'
  branchId: string | null
}

interface Tokens {
  accessToken: string
  refreshToken: string
}

const STORAGE_KEY = 'oism.pos.tokens'

export const tokenStore = {
  get(): Tokens | null {
    try {
      const raw = localStorage.getItem(STORAGE_KEY)
      return raw ? (JSON.parse(raw) as Tokens) : null
    } catch {
      return null
    }
  },
  set: (t: Tokens) => localStorage.setItem(STORAGE_KEY, JSON.stringify(t)),
  clear: () => localStorage.removeItem(STORAGE_KEY),
}

const http = axios.create({ baseURL: API_URL })

http.interceptors.request.use((config) => {
  const tokens = tokenStore.get()
  if (tokens) {
    config.headers.Authorization = `Bearer ${tokens.accessToken}`
  }
  return config
})

let refreshing: Promise<string> | null = null

// 401 -> lam moi token mot lan roi gui lai request; that bai thi dang xuat
http.interceptors.response.use(
  (r) => r,
  async (error) => {
    const original = error.config
    const tokens = tokenStore.get()
    if (error.response?.status === 401 && tokens && original && !original._retried && !original.url?.startsWith('/auth/')) {
      original._retried = true
      try {
        refreshing ??= axios.post<Tokens>(`${API_URL}/auth/refresh`, { refreshToken: tokens.refreshToken })
          .then((r) => {
            tokenStore.set(r.data)
            return r.data.accessToken
          })
          .finally(() => {
            refreshing = null
          })
        original.headers.Authorization = `Bearer ${await refreshing}`
        return http(original)
      } catch {
        tokenStore.clear()
        window.location.reload()
      }
    }
    return Promise.reject(error)
  },
)

export function errorText(error: unknown): string {
  const data = (error as { response?: { data?: { message?: string; errorCode?: string } } })?.response?.data
  if (data?.message) {
    return `${data.message}${data.errorCode ? ` [${data.errorCode}]` : ''}`
  }
  return 'Không kết nối được máy chủ hoặc có lỗi xảy ra'
}

// ----- Du lieu gia -----
const MOCK: SkuInfo[] = [
  { id: 'm1', code: 'POLO-S', productName: 'Ao Polo Nam', attributes: { size: 'S' }, barcode: '8930000000012', retailPrice: 150000, available: 20 },
  { id: 'm2', code: 'POLO-M', productName: 'Ao Polo Nam', attributes: { size: 'M' }, barcode: '8930000000029', retailPrice: 150000, available: 3 },
  { id: 'm3', code: 'JEAN-L', productName: 'Quan Jean Nam', attributes: { size: 'L' }, barcode: '8930000000036', retailPrice: 320000, available: 8 },
  { id: 'm4', code: 'NON-01', productName: 'Non Luoi Trai', attributes: {}, barcode: '8930000000043', retailPrice: 90000, available: 0 },
]

interface SkuSearchItem {
  id: string
  productName: string
  code: string
  attributes: Record<string, string>
  barcode: string | null
  retailPrice: number
  available: number
}

const toInfo = (s: SkuSearchItem): SkuInfo => ({
  id: s.id, code: s.code, productName: s.productName, attributes: s.attributes, barcode: s.barcode,
  retailPrice: s.retailPrice, available: s.available,
})

export async function login(identifier: string, password: string): Promise<Me> {
  if (USE_MOCK) {
    return { userId: 'mock', tenantName: 'Cửa hàng demo (dữ liệu giả)', fullName: 'Thu ngân demo', role: 'CASHIER', branchId: null }
  }
  const { data } = await http.post<Tokens>('/auth/login', { identifier, password })
  tokenStore.set(data)
  return fetchMe()
}

export async function fetchMe(): Promise<Me> {
  const { data } = await http.get<Me>('/me')
  return data
}

export async function logout(): Promise<void> {
  const tokens = tokenStore.get()
  if (tokens && !USE_MOCK) {
    await http.post('/auth/logout', { refreshToken: tokens.refreshToken }).catch(() => undefined)
  }
  tokenStore.clear()
}

/** Tim theo ten / ma SKU / ma vach (FR-POS-01), kem hang kha dung theo chi nhanh cua thu ngan. */
export async function searchSkus(keyword: string, branchId: string | null): Promise<SkuInfo[]> {
  if (USE_MOCK) {
    const k = keyword.trim().toLowerCase()
    return MOCK.filter((s) => s.code.toLowerCase().includes(k) || s.productName.toLowerCase().includes(k) || s.barcode === keyword.trim())
  }
  const { data } = await http.get<{ content: SkuSearchItem[] }>('/skus', {
    params: { keyword, status: 'ACTIVE', branchId: branchId ?? undefined, size: 12 },
  })
  return data.content.map(toInfo)
}

/** Quet ma vach + Enter: tra 1 SKU kem hang kha dung. */
export async function lookupBarcode(barcode: string, branchId: string | null): Promise<SkuInfo> {
  if (USE_MOCK) {
    const found = MOCK.find((s) => s.barcode === barcode.trim())
    if (!found) {
      throw new Error('Không tìm thấy mã vạch')
    }
    return found
  }
  const { data } = await http.get<{
    sku: { id: string; code: string; attributes: Record<string, string>; barcode: string | null; retailPrice: number }
    productName: string
    available: number
  }>('/skus/lookup', { params: { barcode, branchId: branchId ?? undefined } })
  return {
    id: data.sku.id, code: data.sku.code, productName: data.productName, attributes: data.sku.attributes,
    barcode: data.sku.barcode, retailPrice: data.sku.retailPrice, available: data.available,
  }
}

export interface CheckoutPayload {
  branchId: string
  items: { skuId: string; quantity: number }[]
  paymentMethod: 'CASH' | 'QR'
  cashReceived?: number
  customerName?: string
  customerPhone?: string
  note?: string
}

export interface CheckoutReceiptItem {
  skuId: string
  skuCode: string
  productName: string
  quantity: number
  unitPrice: number
  lineTotal: number
}

export interface CheckoutReceipt {
  orderId: string
  orderCode: string
  status: string
  branchId: string
  branchName: string
  totalAmount: number
  paymentMethod: 'CASH' | 'QR'
  cashReceived: number
  changeAmount: number
  completedAt: string
  cashierName: string
  items: CheckoutReceiptItem[]
}

export async function posCheckout(payload: CheckoutPayload): Promise<CheckoutReceipt> {
  if (USE_MOCK) {
    const total = payload.items.reduce((s, i) => s + i.quantity * 100000, 0)
    return {
      orderId: 'mock-order-id',
      orderCode: 'POS-MOCK-001',
      status: 'COMPLETED',
      branchId: payload.branchId,
      branchName: 'Chi nhánh demo',
      totalAmount: total,
      paymentMethod: payload.paymentMethod,
      cashReceived: payload.cashReceived ?? total,
      changeAmount: (payload.cashReceived ?? total) - total,
      completedAt: new Date().toISOString(),
      cashierName: 'Thu ngân demo',
      items: payload.items.map((i) => ({
        skuId: i.skuId,
        skuCode: 'SKU-' + i.skuId,
        productName: 'Sản phẩm demo',
        quantity: i.quantity,
        unitPrice: 100000,
        lineTotal: i.quantity * 100000,
      })),
    }
  }
  const { data } = await http.post<CheckoutReceipt>('/pos/checkout', payload)
  return data
}
