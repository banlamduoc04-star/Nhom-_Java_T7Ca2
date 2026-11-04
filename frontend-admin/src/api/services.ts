import { api, tokenStore } from './client'
import type {
  Branch, Brand, CatalogStatus, Category, LedgerRow, Me, Page, ProductDetail, ProductSummary, Receipt,
  ReceiptStatus, Sku, SkuSearchItem, StockRow, Supplier,
} from './types'

const data = <T>(p: Promise<{ data: T }>): Promise<T> => p.then((r) => r.data)

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
}

export const branchApi = {
  list: () => data(api.get<Branch[]>('/branches')),
}

export const categoryApi = {
  list: () => data(api.get<Category[]>('/categories')),
  create: (body: { name: string; parentId?: string | null }) => data(api.post<Category>('/categories', body)),
  update: (id: string, body: { name: string; parentId?: string | null }) => data(api.put<Category>(`/categories/${id}`, body)),
  remove: (id: string) => api.delete(`/categories/${id}`),
}

export const brandApi = {
  list: () => data(api.get<Brand[]>('/brands')),
  create: (body: { name: string }) => data(api.post<Brand>('/brands', body)),
  update: (id: string, body: { name: string }) => data(api.put<Brand>(`/brands/${id}`, body)),
  remove: (id: string) => api.delete(`/brands/${id}`),
}

export interface SkuInput {
  code: string
  attributes?: Record<string, string>
  barcode?: string
  retailPrice: number
  wholesalePrice?: number | null
  reorderPoint?: number
}

export const productApi = {
  search: (params: { keyword?: string; categoryId?: string; brandId?: string; status?: CatalogStatus; page?: number; size?: number }) =>
    data(api.get<Page<ProductSummary>>('/products', { params })),
  get: (id: string) => data(api.get<ProductDetail>(`/products/${id}`)),
  create: (body: {
    name: string; description?: string; categoryId?: string; brandId?: string; hasVariants: boolean; skus: SkuInput[]
  }) => data(api.post<ProductDetail>('/products', body)),
  update: (id: string, body: { name: string; description?: string; categoryId?: string | null; brandId?: string | null }) =>
    data(api.put<ProductDetail>(`/products/${id}`, body)),
  setStatus: (id: string, status: CatalogStatus) => data(api.patch<ProductDetail>(`/products/${id}/status`, { status })),
  addSku: (productId: string, body: SkuInput) => data(api.post<Sku>(`/products/${productId}/skus`, body)),
}

export const skuApi = {
  search: (params: { keyword?: string; status?: CatalogStatus; branchId?: string; page?: number; size?: number }) =>
    data(api.get<Page<SkuSearchItem>>('/skus', { params })),
  changePrices: (id: string, body: { retailPrice: number; wholesalePrice?: number | null }) =>
    data(api.put<Sku>(`/skus/${id}/prices`, body)),
  update: (id: string, body: { status?: CatalogStatus; reorderPoint?: number }) => data(api.put<Sku>(`/skus/${id}`, body)),
  generateBarcode: (id: string) => data(api.post<Sku>(`/skus/${id}/barcode`)),
}

export const supplierApi = {
  list: () => data(api.get<Supplier[]>('/suppliers')),
  create: (body: { name: string; phone?: string; address?: string }) => data(api.post<Supplier>('/suppliers', body)),
  update: (id: string, body: { name: string; phone?: string; address?: string; active: boolean }) =>
    data(api.put<Supplier>(`/suppliers/${id}`, body)),
}

export interface ReceiptBody {
  branchId: string
  supplierId?: string | null
  items: { skuId: string; quantity: number; unitPrice: number }[]
}

export const receiptApi = {
  list: (params: { status?: ReceiptStatus; branchId?: string; page?: number; size?: number }) =>
    data(api.get<Page<Receipt>>('/purchase-receipts', { params })),
  create: (body: ReceiptBody) => data(api.post<Receipt>('/purchase-receipts', body)),
  update: (id: string, body: ReceiptBody) => data(api.put<Receipt>(`/purchase-receipts/${id}`, body)),
  confirm: (id: string) => data(api.post<Receipt>(`/purchase-receipts/${id}/confirm`)),
}

export const stockApi = {
  stock: (params: { branchId?: string; skuId?: string; page?: number; size?: number }) =>
    data(api.get<Page<StockRow>>('/stock', { params })),
  ledger: (params: { branchId?: string; skuId?: string; from?: string; to?: string; page?: number; size?: number }) =>
    data(api.get<Page<LedgerRow>>('/inventory-transactions', { params })),
}
