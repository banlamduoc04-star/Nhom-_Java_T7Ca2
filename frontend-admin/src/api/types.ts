export type Role = 'OWNER' | 'STAFF' | 'CASHIER'
export type CatalogStatus = 'ACTIVE' | 'INACTIVE'
export type ReceiptStatus = 'DRAFT' | 'CONFIRMED'

export interface Page<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface Me {
  userId: string
  tenantId: string
  tenantName: string
  email: string
  phone: string | null
  fullName: string
  role: Role
  branchId: string | null
}

export interface Branch {
  id: string
  name: string
  type: 'STORE' | 'WAREHOUSE'
  address: string | null
  active: boolean
}

export interface Category {
  id: string
  parentId: string | null
  name: string
}

export interface Brand {
  id: string
  name: string
}

export interface Sku {
  id: string
  productId: string
  code: string
  attributes: Record<string, string>
  barcode: string | null
  retailPrice: number
  wholesalePrice: number | null
  avgCost: number
  reorderPoint: number
  status: CatalogStatus
}

export interface ProductSummary {
  id: string
  name: string
  categoryId: string | null
  brandId: string | null
  hasVariants: boolean
  status: CatalogStatus
  skuCount: number
  minRetailPrice: number | null
  maxRetailPrice: number | null
}

export interface ProductDetail {
  id: string
  name: string
  description: string | null
  categoryId: string | null
  brandId: string | null
  hasVariants: boolean
  status: CatalogStatus
  skus: Sku[]
}

export interface SkuSearchItem {
  id: string
  productId: string
  productName: string
  code: string
  attributes: Record<string, string>
  barcode: string | null
  retailPrice: number
  avgCost: number
  status: CatalogStatus
  available: number
}

export interface Supplier {
  id: string
  name: string
  phone: string | null
  address: string | null
  active: boolean
}

export interface ReceiptItem {
  skuId: string
  skuCode: string
  quantity: number
  unitPrice: number
  lineTotal: number
}

export interface Receipt {
  id: string
  code: string
  branchId: string
  supplierId: string | null
  status: ReceiptStatus
  confirmedAt: string | null
  createdAt: string
  totalAmount: number
  items: ReceiptItem[]
}

export interface StockRow {
  branchId: string
  skuId: string
  skuCode: string
  onHand: number
  reserved: number
  available: number
}

export interface LedgerRow {
  id: string
  branchId: string
  skuId: string
  skuCode: string
  direction: 'IN' | 'OUT'
  reason: string
  quantity: number
  balanceAfter: number
  unitCost: number | null
  refType: string | null
  createdAt: string
}

export type OrderStatus = 'DRAFT' | 'RESERVED' | 'CONFIRMED' | 'COMPLETED' | 'CANCELLED'
export type OrderChannel = 'MANUAL' | 'POS' | 'SHOPEE' | 'LAZADA' | 'TIKTOK'

export interface OrderItemView {
  skuId: string
  skuCode: string
  quantity: number
  unitPrice: number
  costPrice: number | null
  lineTotal: number
}

export interface OrderHistoryView {
  fromStatus: OrderStatus | null
  toStatus: OrderStatus
  changedBy: string | null
  reason: string | null
  createdAt: string
}

export interface OrderView {
  id: string
  code: string
  branchId: string
  channel: OrderChannel
  externalOrderId: string | null
  status: OrderStatus
  customerName: string | null
  customerPhone: string | null
  shippingAddress: string | null
  totalAmount: number
  reservedUntil: string | null
  createdAt: string
  items: OrderItemView[]
  history: OrderHistoryView[]
}

export type TransferStatus = 'DRAFT' | 'DISPATCHED' | 'RECEIVED' | 'CANCELLED'

export interface TransferItemView {
  skuId: string
  skuCode: string
  quantity: number
}

export interface TransferView {
  id: string
  code: string
  fromBranchId: string
  toBranchId: string
  status: TransferStatus
  note: string | null
  createdBy: string | null
  createdAt: string
  dispatchedAt: string | null
  receivedAt: string | null
  items: TransferItemView[]
}

export type StocktakeStatus = 'OPEN' | 'APPROVED' | 'CANCELLED'

export interface StocktakeItemView {
  skuId: string
  skuCode: string
  systemQty: number
  countedQty: number | null
  difference: number | null
}

export interface StocktakeView {
  id: string
  code: string
  branchId: string
  status: StocktakeStatus
  note: string | null
  createdBy: string | null
  createdAt: string
  approvedAt: string | null
  items: StocktakeItemView[]
}

export interface ProfitSummary {
  totalRevenue: number
  totalCogs: number
  grossProfit: number
  grossMargin: number
}

export interface ProfitBySku {
  skuId: string
  skuCode: string
  quantitySold: number
  revenue: number
  cogs: number
  grossProfit: number
  grossMargin: number
}

export interface ProfitReportView {
  summary: ProfitSummary
  bySku: ProfitBySku[]
}

/** Khop SimulatorController.SimulationResult. */
export interface SimulationResult {
  total: number
  successful: number
  failed: number
  /** errorCode -> so don loi, vi du { IV40901: 49 } */
  errorCounts: Record<string, number>
  successfulOrderCodes: string[]
  durationMs: number
}

/** San nhan webhook theo ke hoach (channel_credential.channel). */
export type WebhookChannel = 'SHOPEE' | 'TIKTOK'

/** Khop ChannelCredentialService.ChannelKeyView: khong bao gio chua key. */
export interface ChannelKeyView {
  channel: WebhookChannel
  configured: boolean
  createdAt: string | null
}

/** Khop ChannelCredentialService.IssuedKey: key tho chi tra ve mot lan. */
export interface IssuedChannelKey {
  channel: WebhookChannel
  apiKey: string
  createdAt: string
}


export interface StockAlert {
  id: string
  branchId: string
  branchName: string
  skuId: string
  skuCode: string
  productName: string
  status: StockAlertStatus
  threshold: number
  /** Hang kha dung luc mo canh bao (cot stock_alert.available) */
  available: number
  createdAt: string
  closedAt: string | null
}

/** Ke hoach muc 4, bang stock_alert: OPEN/CLOSED. */
export type StockAlertStatus = 'OPEN' | 'CLOSED'

/** Khop domain/event/StockAlertEvent (kenh /topic/tenants/{id}/alerts). */
export interface StockAlertEvent {
  alertId: string
  branchId: string
  skuId: string
  skuCode: string
  status: StockAlertStatus
  threshold: number
  available: number
  createdAt: string
}

/** Khop domain/event/OrderCreatedEvent (kenh /topic/tenants/{id}/orders). */
export interface OrderCreatedEvent {
  orderId: string
  orderCode: string
  totalAmount: number
  channel: string
  createdBy: string
  createdAt: string
}

export interface ValuationItem {
  branchId: string
  branchName: string
  skuId: string
  skuCode: string
  productName: string
  onHand: number
  avgCost: number
  totalValue: number
}

export interface ValuationSummary {
  totalInventoryValue: number
  totalOnHand: number
  totalActiveSkus: number
  items: ValuationItem[]
}

export interface TopSellingItem {
  skuId: string
  skuCode: string
  productName: string
  quantitySold: number
  totalRevenue: number
}

export interface SlowMovingItem {
  branchId: string
  branchName: string
  skuId: string
  skuCode: string
  productName: string
  onHand: number
  avgCost: number
  totalValue: number
  daysWithoutSale: number
}

export interface DailyReport {
  id: string
  reportDate: string
  totalRevenue: number
  totalCogs: number
  grossProfit: number
  totalOrders: number
  completedOrders: number
  cancelledOrders: number
  createdAt: string
}

export interface UserView {
  id: string
  email: string
  fullName: string
  phone: string | null
  role: Role
  branchId: string | null
  branchName: string | null
  active: boolean
}

export interface TenantSettings {
  tenantId: string
  tenantName: string
  reservationMinutes: number
}
