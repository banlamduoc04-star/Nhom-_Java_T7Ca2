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
