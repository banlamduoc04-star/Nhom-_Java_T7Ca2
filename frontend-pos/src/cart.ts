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
  error?: string
}

const invalidQuantity = 'Số lượng phải là số nguyên dương'

const overStock = (sku: SkuInfo) =>
  `${sku.code}: chỉ còn ${sku.available} sản phẩm khả dụng`

/** Them san pham; quet lai cung SKU thi tang so luong. */
export function addToCart(
  cart: CartLine[],
  sku: SkuInfo,
  quantity = 1,
): CartResult {
  if (!Number.isInteger(quantity) || quantity <= 0) {
    return { cart, error: invalidQuantity }
  }

  const existing = cart.find((line) => line.sku.id === sku.id)
  const next = (existing?.quantity ?? 0) + quantity

  if (next > sku.available) {
    return { cart, error: overStock(sku) }
  }

  if (existing) {
    return {
      cart: cart.map((line) =>
        line.sku.id === sku.id
          ? { ...line, quantity: next }
          : line,
      ),
    }
  }

  return {
    cart: [...cart, { sku, quantity }],
  }
}

/** Sua so luong; <= 0 thi xoa dong. */
export function setQuantity(
  cart: CartLine[],
  skuId: string,
  quantity: number,
): CartResult {
  const line = cart.find((item) => item.sku.id === skuId)

  if (!line) {
    return { cart }
  }

  if (!Number.isFinite(quantity) || quantity <= 0) {
    return { cart: removeLine(cart, skuId) }
  }

  if (!Number.isInteger(quantity)) {
    return { cart, error: invalidQuantity }
  }

  if (quantity > line.sku.available) {
    return { cart, error: overStock(line.sku) }
  }

  return {
    cart: cart.map((item) =>
      item.sku.id === skuId
        ? { ...item, quantity }
        : item,
    ),
  }
}

export function removeLine(
  cart: CartLine[],
  skuId: string,
): CartLine[] {
  return cart.filter((line) => line.sku.id !== skuId)
}

export function cartTotal(cart: CartLine[]): number {
  return cart.reduce(
    (sum, line) => sum + line.sku.retailPrice * line.quantity,
    0,
  )
}

export function itemCount(cart: CartLine[]): number {
  return cart.reduce((sum, line) => sum + line.quantity, 0)
}

/** Nhan dien ma vach tu may quet USB hoat dong nhu ban phim. */
export function looksLikeBarcode(text: string): boolean {
  return /^\d{8,}$/.test(text.trim())
}
