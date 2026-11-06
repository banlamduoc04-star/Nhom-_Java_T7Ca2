import { api, data } from '../client'
import type { Brand, CatalogStatus, Category, Page, ProductDetail, ProductSummary, Sku, SkuSearchItem } from '../types'

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
