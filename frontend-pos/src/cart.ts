export interface SkuInfo {
  id: string
  code: string
  productName: string
  attributes: Record<string, string>
  barcode: string | null
  retailPrice: number
  /** Hang kha dung (on_hand - reserved) tai thoi diem tra cuu. */
  available: number
}

export interface CartLine {
  sku: SkuInfo
  quantity: number
}

export interface CartResult {
  cart: CartLine[]
  /** Co loi khi vuot ton: nguoi dung duoc bao con bao nhieu (FR-POS-02). */
  error?: string
}

const overStock = (sku: SkuInfo) => `${sku.code}: chỉ còn ${sku.available} sản phẩm khả dụng`

/** Them 1 san pham; quet lai cung ma thi tang so luong (FR-POS-01). Khong vuot hang kha dung (FR-POS-02). */
export function addToCart(cart: CartLine[], sku: SkuInfo, quantity = 1): CartResult {
  const existing = cart.find((l) => l.sku.id === sku.id)
  const next = (existing?.quantity ?? 0) + quantity
  if (next > sku.available) {
    return { cart, error: overStock(sku) }
  }
  if (existing) {
    return { cart: cart.map((l) => (l.sku.id === sku.id ? { ...l, quantity: next } : l)) }
  }
  return { cart: [...cart, { sku, quantity }] }
}

/** Sua so luong: <= 0 thi xoa dong; vuot hang kha dung thi giu nguyen va bao loi. */
export function setQuantity(cart: CartLine[], skuId: string, quantity: number): CartResult {
  const line = cart.find((l) => l.sku.id === skuId)
  if (!line) {
    return { cart }
  }
  if (!Number.isFinite(quantity) || quantity <= 0) {
    return { cart: removeLine(cart, skuId) }
  }
  if (quantity > line.sku.available) {
    return { cart, error: overStock(line.sku) }
  }
  return { cart: cart.map((l) => (l.sku.id === skuId ? { ...l, quantity: Math.floor(quantity) } : l)) }
}

export function removeLine(cart: CartLine[], skuId: string): CartLine[] {
  return cart.filter((l) => l.sku.id !== skuId)
}

export function cartTotal(cart: CartLine[]): number {
  return cart.reduce((sum, l) => sum + l.sku.retailPrice * l.quantity, 0)
}

export function itemCount(cart: CartLine[]): number {
  return cart.reduce((sum, l) => sum + l.quantity, 0)
}

/** May quet ma vach go nhu ban phim: chuoi toan chu so tu 8 ky tu tro len coi la ma vach. */
export function looksLikeBarcode(text: string): boolean {
  return /^\d{8,}$/.test(text.trim())
}
