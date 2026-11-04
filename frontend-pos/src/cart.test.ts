import { describe, expect, it } from 'vitest'
import { addToCart, cartTotal, itemCount, looksLikeBarcode, removeLine, setQuantity, type SkuInfo } from './cart'

const sku = (id: string, available: number, price = 100_000): SkuInfo => ({
  id, code: `SKU-${id}`, productName: `San pham ${id}`, attributes: {}, barcode: null, retailPrice: price, available,
})

describe('gio hang POS', () => {
  it('them moi roi quet lai thi tang so luong', () => {
    let { cart } = addToCart([], sku('1', 5))
    cart = addToCart(cart, sku('1', 5)).cart
    expect(cart).toHaveLength(1)
    expect(cart[0].quantity).toBe(2)
  })

  it('chan khi vuot hang kha dung va bao so con lai', () => {
    const s = sku('1', 2)
    const first = addToCart([], s, 2)
    const over = addToCart(first.cart, s)
    expect(over.error).toContain('chỉ còn 2')
    expect(over.cart[0].quantity).toBe(2)
  })

  it('khong them duoc hang het (available = 0)', () => {
    const result = addToCart([], sku('1', 0))
    expect(result.cart).toHaveLength(0)
    expect(result.error).toBeDefined()
  })

  it('sua so luong hop le, vuot ton thi giu nguyen, <= 0 thi xoa dong', () => {
    const { cart } = addToCart([], sku('1', 5), 2)
    expect(setQuantity(cart, '1', 4).cart[0].quantity).toBe(4)
    expect(setQuantity(cart, '1', 9).error).toContain('chỉ còn 5')
    expect(setQuantity(cart, '1', 9).cart[0].quantity).toBe(2)
    expect(setQuantity(cart, '1', 0).cart).toHaveLength(0)
    expect(setQuantity(cart, '1', Number.NaN).cart).toHaveLength(0)
  })

  it('tinh tong tien va tong so luong', () => {
    let cart = addToCart([], sku('1', 9, 150_000), 2).cart
    cart = addToCart(cart, sku('2', 9, 50_000), 3).cart
    expect(cartTotal(cart)).toBe(2 * 150_000 + 3 * 50_000)
    expect(itemCount(cart)).toBe(5)
    expect(cartTotal(removeLine(cart, '1'))).toBe(150_000)
  })

  it('nhan dien ma vach tu may quet', () => {
    expect(looksLikeBarcode('8936001112225')).toBe(true)
    expect(looksLikeBarcode(' 12345678 ')).toBe(true)
    expect(looksLikeBarcode('ao thun')).toBe(false)
    expect(looksLikeBarcode('1234')).toBe(false)
  })
})
