import { api, data } from '../client'
import type { LedgerRow, Page, Receipt, ReceiptStatus, StockAlert, StockAlertStatus, StockRow, StocktakeStatus, StocktakeView, Supplier, TransferStatus, TransferView } from '../types'

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

export interface CreateTransferBody {
  fromBranchId: string
  toBranchId: string
  note?: string
  items: { skuId: string; quantity: number }[]
}

export const transferApi = {
  list: (params: { status?: TransferStatus; page?: number; size?: number }) =>
    data(api.get<Page<TransferView>>('/transfers', { params })),
  get: (id: string) => data(api.get<TransferView>(`/transfers/${id}`)),
  create: (body: CreateTransferBody) => data(api.post<TransferView>('/transfers', body)),
  dispatch: (id: string) => data(api.post<TransferView>(`/transfers/${id}/dispatch`)),
  receive: (id: string) => data(api.post<TransferView>(`/transfers/${id}/receive`)),
  cancel: (id: string, reason: string) => data(api.post<TransferView>(`/transfers/${id}/cancel`, { reason })),
}

export interface CreateStocktakeBody {
  branchId: string
  note?: string
}

export const stocktakeApi = {
  list: (params: { status?: StocktakeStatus; branchId?: string; page?: number; size?: number }) =>
    data(api.get<Page<StocktakeView>>('/stocktakes', { params })),
  get: (id: string) => data(api.get<StocktakeView>(`/stocktakes/${id}`)),
  create: (body: CreateStocktakeBody) => data(api.post<StocktakeView>('/stocktakes', body)),
  recordCounts: (id: string, counts: { skuId: string; countedQty: number }[]) =>
    data(api.put<StocktakeView>(`/stocktakes/${id}/counts`, { counts })),
  approve: (id: string) => data(api.post<StocktakeView>(`/stocktakes/${id}/approve`)),
}

export const alertApi = {
  list: (params?: { status?: StockAlertStatus; branchId?: string }) =>
    data(api.get<StockAlert[]>('/alerts/stock', { params })),
  /** Danh sach API cua ke hoach: PATCH /alerts/stock/{id}/read -> dong canh bao (CLOSED) */
  markRead: (id: string) => data(api.patch<void>(`/alerts/stock/${id}/read`)),
  count: () => data(api.get<{ openAlerts: number }>('/alerts/stock/count')),
}
