import { useCallback, useEffect, useRef, useState, type FormEvent } from 'react'
import { errorText, logout, lookupBarcode, searchSkus, type Me } from './api'
import { addToCart, cartTotal, itemCount, looksLikeBarcode, removeLine, setQuantity, type CartLine, type SkuInfo } from './cart'

interface Props {
  user: Me
  onLogout: () => void
}

interface Notice {
  kind: 'error' | 'ok'
  text: string
}

const money = (v: number) => `${v.toLocaleString('vi-VN')} ₫`
const attrText = (sku: SkuInfo) => Object.values(sku.attributes).join(' / ')

/**
 * Man hinh ban hang (FR-POS-01, FR-POS-02): tim theo ten/SKU/ma vach; quet ma + Enter them ngay vao gio,
 * quet lai tang so luong; F2 tim, F9 thanh toan, Esc huy gio; chan vuot hang kha dung.
 * Thanh toan va in hoa don (FR-POS-03/04) lam o Sprint 4.
 */
export default function SaleScreen({ user, onLogout }: Props) {
  const searchRef = useRef<HTMLInputElement>(null)
  const [text, setText] = useState('')
  const [results, setResults] = useState<SkuInfo[]>([])
  const [cart, setCart] = useState<CartLine[]>([])
  const [notice, setNotice] = useState<Notice | null>(null)
  const [payOpen, setPayOpen] = useState(false)
  const [busy, setBusy] = useState(false)

  const add = useCallback((sku: SkuInfo) => {
    setCart((current) => {
      const result = addToCart(current, sku)
      setNotice(result.error ? { kind: 'error', text: result.error } : { kind: 'ok', text: `Đã thêm ${sku.code}` })
      return result.cart
    })
    setResults([])
    setText('')
    searchRef.current?.focus()
  }, [])

  const submit = async (e: FormEvent) => {
    e.preventDefault()
    const query = text.trim()
    if (!query) {
      return
    }
    setBusy(true)
    setNotice(null)
    try {
      if (looksLikeBarcode(query)) {
        add(await lookupBarcode(query, user.branchId))
        return
      }
      const found = await searchSkus(query, user.branchId)
      if (found.length === 1) {
        add(found[0])
      } else {
        setResults(found)
        setNotice(found.length === 0 ? { kind: 'error', text: 'Không tìm thấy sản phẩm nào' } : null)
      }
    } catch (err) {
      setNotice({ kind: 'error', text: errorText(err) })
      setText('')
    } finally {
      setBusy(false)
    }
  }

  const clearCart = useCallback(() => {
    if (cart.length === 0 || window.confirm('Hủy toàn bộ giỏ hàng?')) {
      setCart([])
      setResults([])
      setNotice(null)
      searchRef.current?.focus()
    }
  }, [cart.length])

  // Phim tat: F2 tim, F9 thanh toan, Esc huy gio (hoac dong hop thoai)
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'F2') {
        e.preventDefault()
        searchRef.current?.focus()
        searchRef.current?.select()
      } else if (e.key === 'F9') {
        e.preventDefault()
        if (cart.length > 0) {
          setPayOpen(true)
        } else {
          setNotice({ kind: 'error', text: 'Giỏ hàng đang trống' })
        }
      } else if (e.key === 'Escape') {
        if (payOpen) {
          setPayOpen(false)
        } else {
          clearCart()
        }
      }
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [cart.length, payOpen, clearCart])

  const changeQuantity = (skuId: string, quantity: number) => {
    const result = setQuantity(cart, skuId, quantity)
    setCart(result.cart)
    setNotice(result.error ? { kind: 'error', text: result.error } : null)
  }

  return (
    <div className="pos">
      <header className="bar">
        <strong>OISM POS</strong>
        <span className="muted">{user.tenantName} · {user.fullName}</span>
        <button onClick={() => logout().then(onLogout)}>Đăng xuất</button>
      </header>

      <main className="layout">
        <section className="card">
          <form onSubmit={submit} className="search">
            <input ref={searchRef} value={text} onChange={(e) => setText(e.target.value)} autoFocus disabled={busy}
              placeholder="Quét mã vạch + Enter, hoặc gõ tên / mã SKU (F2)" aria-label="Tìm sản phẩm" />
            <button type="submit" className="primary" disabled={busy}>Tìm</button>
          </form>
          {notice && <div className={`alert ${notice.kind}`} role="status">{notice.text}</div>}
          <ul className="results">
            {results.map((sku) => (
              <li key={sku.id}>
                <button onClick={() => add(sku)} disabled={sku.available <= 0}>
                  <span><strong>{sku.productName}</strong> {attrText(sku)}<br /><small>{sku.code}</small></span>
                  <span className="right">{money(sku.retailPrice)}<br /><small>{sku.available > 0 ? `Còn ${sku.available}` : 'Hết hàng'}</small></span>
                </button>
              </li>
            ))}
          </ul>
        </section>

        <section className="card cart">
          <h2>Giỏ hàng ({itemCount(cart)})</h2>
          {cart.length === 0 ? (
            <p className="muted">Chưa có sản phẩm. Quét mã vạch để thêm.</p>
          ) : (
            <table>
              <thead>
                <tr><th>Sản phẩm</th><th>Đơn giá</th><th>SL</th><th>Thành tiền</th><th /></tr>
              </thead>
              <tbody>
                {cart.map(({ sku, quantity }) => (
                  <tr key={sku.id}>
                    <td><strong>{sku.productName}</strong> {attrText(sku)}<br /><small>{sku.code}</small></td>
                    <td>{money(sku.retailPrice)}</td>
                    <td className="qty">
                      <button aria-label="Giảm" onClick={() => changeQuantity(sku.id, quantity - 1)}>−</button>
                      <input type="number" min={1} value={quantity} aria-label={`Số lượng ${sku.code}`}
                        onChange={(e) => changeQuantity(sku.id, Number(e.target.value))} />
                      <button aria-label="Tăng" onClick={() => changeQuantity(sku.id, quantity + 1)}>+</button>
                    </td>
                    <td>{money(sku.retailPrice * quantity)}</td>
                    <td><button aria-label={`Xóa ${sku.code}`} onClick={() => setCart(removeLine(cart, sku.id))}>✕</button></td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
          <div className="total">
            <span>Tổng cộng</span>
            <strong>{money(cartTotal(cart))}</strong>
          </div>
          <div className="actions">
            <button onClick={clearCart} disabled={cart.length === 0}>Hủy giỏ (Esc)</button>
            <button className="primary" onClick={() => setPayOpen(true)} disabled={cart.length === 0}>Thanh toán (F9)</button>
          </div>
        </section>
      </main>

      {payOpen && (
        <div className="modal" role="dialog" aria-modal="true" aria-label="Thanh toán">
          <div className="card">
            <h2>Thanh toán</h2>
            <p>Tổng tiền: <strong>{money(cartTotal(cart))}</strong></p>
            <p className="muted">Thanh toán tiền mặt/QR, tạo đơn một giao dịch và in hóa đơn 80 mm sẽ được nối API checkout ở Sprint 4.</p>
            <div className="actions"><button onClick={() => setPayOpen(false)}>Đóng (Esc)</button></div>
          </div>
        </div>
      )}
    </div>
  )
}
