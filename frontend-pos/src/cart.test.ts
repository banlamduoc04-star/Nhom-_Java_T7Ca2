import { describe, expect, it } from 'vitest'
import {
  addToCart,
  cartTotal,
  itemCount,
  looksLikeBarcode,
  removeLine,
  setQuantity,
  type SkuInfo,
} from './cart'

function sku(
  id: string,
  available = 10,
  retailPrice = 100_000,
): SkuInfo {
  return {
    id,
    code: `SKU-${id}`,
    productName: `San pham ${id}`,
    attributes: {},
    barcode: null,
    retailPrice,
    available,
  }
}

describe('gio hang POS', () => {
  it('them san pham vao gio hang', () => {
    const result = addToCart([], sku('1'))

    expect(result.cart).toHaveLength(1)
    expect(result.cart[0].quantity).toBe(1)
    expect(result.error).toBeUndefined()
  })

  it('quet lai cung SKU thi tang so luong', () => {
    const product = sku('1')
    const first = addToCart([], product)
    const second = addToCart(first.cart, product)

    expect(second.cart).toHaveLength(1)
    expect(second.cart[0].quantity).toBe(2)
  })

  it('khong cho phep vuot ton kho', () => {
    const product = sku('1', 2)
    const first = addToCart([], product, 2)
    const second = addToCart(first.cart, product)

    expect(second.cart[0].quantity).toBe(2)
    expect(second.error).toContain('chỉ còn 2')
  })

  it('tu choi so luong am, bang 0 hoac so thap phan khi them', () => {
    const product = sku('1')

    expect(addToCart([], product, 0).error).toContain('số nguyên dương')
    expect(addToCart([], product, -1).error).toContain('số nguyên dương')
    expect(addToCart([], product, 1.5).error).toContain('số nguyên dương')
  })

  it('giu nguyen gio hang khi sua so luong thanh so thap phan', () => {
    const initial = addToCart([], sku('1'), 2).cart
    const result = setQuantity(initial, '1', 1.5)

    expect(result.error).toContain('số nguyên dương')
    expect(result.cart[0].quantity).toBe(2)
  })

  it('xoa san pham khi so luong bang 0', () => {
    const initial = addToCart([], sku('1')).cart
    const result = setQuantity(initial, '1', 0)

    expect(result.cart).toHaveLength(0)
  })

  it('giu nguyen gio hang khi vuot ton kho luc sua so luong', () => {
    const initial = addToCart([], sku('1', 3), 2).cart
    const result = setQuantity(initial, '1', 4)

    expect(result.cart[0].quantity).toBe(2)
    expect(result.error).toContain('chỉ còn 3')
  })

  it('tinh tong tien va tong so luong', () => {
    let cart = addToCart([], sku('1', 9, 150_000), 2).cart
    cart = addToCart(cart, sku('2', 9, 50_000), 3).cart

    expect(cartTotal(cart)).toBe(450_000)
    expect(itemCount(cart)).toBe(5)
  })

  it('xoa dung san pham duoc chon', () => {
    let cart = addToCart([], sku('1')).cart
    cart = addToCart(cart, sku('2')).cart

    const result = removeLine(cart, '1')

    expect(result).toHaveLength(1)
    expect(result[0].sku.id).toBe('2')
  })

  it('nhan dien ma vach', () => {
    expect(looksLikeBarcode('8930000000012')).toBe(true)
    expect(looksLikeBarcode('1234567')).toBe(false)
    expect(looksLikeBarcode('ABC12345678')).toBe(false)
  })
})
