/** Tao du lieu nen qua API that (khong dung du lieu cua test khac). */
export const API_URL = process.env.E2E_API_URL ?? 'http://localhost:8080/api/v1'
export const ADMIN_URL = process.env.E2E_ADMIN_URL ?? 'http://localhost:3000'
export const POS_URL = process.env.E2E_POS_URL ?? 'http://localhost:3001'
export const PASSWORD = 'E2e@12345'

type Json = Record<string, unknown>

export async function api<T = Json>(method: string, path: string, token?: string, body?: unknown, headers: Record<string, string> = {}): Promise<T> {
  const res = await fetch(API_URL + path, {
    method,
    headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}), ...headers },
    body: body === undefined ? undefined : JSON.stringify(body),
  })
  const text = await res.text()
  if (!res.ok) {
    throw new Error(`${method} ${path} -> ${res.status}: ${text}`)
  }
  return (text ? JSON.parse(text) : undefined) as T
}

export interface Shop {
  runId: string
  email: string
  token: string
  tenantId: string
  branchId: string
}

export async function newShop(prefix: string): Promise<Shop> {
  const runId = `${prefix}${Date.now().toString(36)}${Math.floor(Math.random() * 1000)}`
  const email = `owner-${runId}@e2e.test`
  const reg = await api<{ tenantId: string; branchId: string }>('POST', '/auth/register',
    undefined, { storeName: `Shop ${runId}`, fullName: 'Chu E2E', email, password: PASSWORD })
  const login = await api<{ accessToken: string }>('POST', '/auth/login', undefined, { identifier: email, password: PASSWORD })
  return { runId, email, token: login.accessToken, tenantId: reg.tenantId, branchId: reg.branchId }
}

/** San pham 1 SKU co ma vach, da nhap kho qty cai. */
export async function stockedSku(shop: Shop, code: string, barcode: string, qty: number): Promise<string> {
  const product = await api<{ skus: { id: string }[] }>('POST', '/products', shop.token,
    { name: 'Ao E2E', hasVariants: false, skus: [{ code, barcode, retailPrice: 150000 }] })
  const skuId = product.skus[0].id
  const receipt = await api<{ id: string }>('POST', '/purchase-receipts', shop.token,
    { branchId: shop.branchId, items: [{ skuId, quantity: qty, unitPrice: 100000 }] })
  await api('POST', `/purchase-receipts/${receipt.id}/confirm`, shop.token)
  return skuId
}

export async function cashier(shop: Shop): Promise<string> {
  const email = `cashier-${shop.runId}@e2e.test`
  await api('POST', '/users', shop.token, { email, password: PASSWORD, fullName: 'Thu ngan E2E', role: 'CASHIER', branchId: shop.branchId })
  return email
}
