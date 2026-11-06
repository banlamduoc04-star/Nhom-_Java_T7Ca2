import { useCallback, useEffect, useRef, useState, type FormEvent } from 'react'
import { errorText, logout, lookupBarcode, posCheckout, searchSkus, type CheckoutReceipt, type Me } from './api'
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

export default function SaleScreen({ user, onLogout }: Props) {
  const searchRef = useRef<HTMLInputElement>(null)
  const cashInputRef = useRef<HTMLInputElement>(null)
  const [text, setText] = useState('')
  const [results, setResults] = useState<SkuInfo[]>([])
  const [cart, setCart] = useState<CartLine[]>([])
  const [notice, setNotice] = useState<Notice | null>(null)

  // Payment states
  const [payOpen, setPayOpen] = useState(false)
  const [paymentMethod, setPaymentMethod] = useState<'CASH' | 'QR'>('CASH')
  const [cashGiven, setCashGiven] = useState<number>(0)
  const [busy, setBusy] = useState(false)

  // Receipt modal state
  const [receipt, setReceipt] = useState<CheckoutReceipt | null>(null)

  const total = cartTotal(cart)

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
    if (!query) return

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

  const openCheckout = useCallback(() => {
    if (cart.length === 0) {
      setNotice({ kind: 'error', text: 'Giỏ hàng đang trống' })
      return
    }
    setCashGiven(total)
    setPayOpen(true)
    setTimeout(() => cashInputRef.current?.select(), 100)
  }, [cart.length, total])

  const handleCheckout = async () => {
    if (!user.branchId) {
      setNotice({ kind: 'error', text: 'Tài khoản chưa được gán chi nhánh để thực hiện thanh toán' })
      return
    }
    if (paymentMethod === 'CASH' && cashGiven < total) {
      setNotice({ kind: 'error', text: 'Tiền khách đưa chưa đủ!' })
      return
    }

    setBusy(true)
    try {
      const receiptData = await posCheckout({
        branchId: user.branchId,
        items: cart.map((c) => ({ skuId: c.sku.id, quantity: c.quantity })),
        paymentMethod,
        cashReceived: paymentMethod === 'CASH' ? cashGiven : total,
      })
      setCart([])
      setPayOpen(false)
      setReceipt(receiptData)
      setNotice({ kind: 'ok', text: 'Thanh toán thành công!' })
    } catch (err) {
      // Hiển thị lỗi trực quan hơn thay vì alert()
      setNotice({ kind: 'error', text: `Lỗi thanh toán: ${errorText(err)}` })
    } finally {
      setBusy(false)
    }
  }

  // Phím tắt: F2 tìm, F9 thanh toán, Esc hủy giỏ (hoặc đóng hộp thoại)
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'F2') {
        e.preventDefault()
        searchRef.current?.focus()
        searchRef.current?.select()
      } else if (e.key === 'F9') {
        e.preventDefault()
        if (payOpen) {
          if (!busy) handleCheckout()
        } else if (!receipt) {
          openCheckout()
        }
      } else if (e.key === 'Escape') {
        if (receipt) {
          setReceipt(null)
          searchRef.current?.focus()
        } else if (payOpen) {
          setPayOpen(false)
          searchRef.current?.focus()
        } else {
          clearCart()
        }
      }
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [payOpen, receipt, busy, openCheckout, clearCart])

  const changeQuantity = (skuId: string, quantity: number) => {
    const result = setQuantity(cart, skuId, quantity)
    setCart(result.cart)
    setNotice(result.error ? { kind: 'error', text: result.error } : null)
  }

  const changeDue = Math.max(0, cashGiven - total)

  return (
    <div className="pos">
      <header className="bar no-print">
        <strong>OISM POS</strong>
        <span className="muted">{user.tenantName} · {user.fullName}</span>
        <button onClick={() => logout().then(onLogout)}>Đăng xuất</button>
      </header>

      <main className="layout no-print">
        <section className="card">
          <form onSubmit={submit} className="search">
            <input
              ref={searchRef}
              value={text}
              onChange={(e) => setText(e.target.value)}
              autoFocus
              disabled={busy}
              placeholder="Quét mã vạch + Enter, hoặc gõ tên / mã SKU (F2)"
              aria-label="Tìm sản phẩm"
            />
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
                      <input
                        type="number"
                        min={1}
                        value={quantity}
                        aria-label={`Số lượng ${sku.code}`}
                        onChange={(e) => changeQuantity(sku.id, Number(e.target.value))}
                      />
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
            <strong>{money(total)}</strong>
          </div>
          <div className="actions">
            <button onClick={clearCart} disabled={cart.length === 0 || busy}>Hủy giỏ (Esc)</button>
            <button className="primary" onClick={openCheckout} disabled={cart.length === 0 || busy}>
              Thanh toán (F9)
            </button>
          </div>
        </section>
      </main>

      {/* Modal thanh toán */}
      {payOpen && (
        <div className="modal no-print" role="dialog" aria-modal="true" aria-label="Thanh toán">
          <div className="card" style={{ width: 460 }}>
            <h2>Thanh toán đơn hàng</h2>
            <div style={{ fontSize: '1.2rem', marginBottom: 12 }}>
              Tổng tiền thanh toán: <strong style={{ color: '#1677ff' }}>{money(total)}</strong>
            </div>

            <div className="pay-methods">
              <button
                type="button"
                className={paymentMethod === 'CASH' ? 'active' : ''}
                onClick={() => setPaymentMethod('CASH')}
              >
                💵 Tiền mặt
              </button>
              <button
                type="button"
                className={paymentMethod === 'QR' ? 'active' : ''}
                onClick={() => setPaymentMethod('QR')}
              >
                📱 Chuyển khoản QR
              </button>
            </div>

            {paymentMethod === 'CASH' ? (
              <div>
                <label style={{ display: 'block', marginBottom: 6, fontWeight: 600 }}>Tiền khách đưa (₫):</label>
                <input
                  ref={cashInputRef}
                  type="number"
                  step={1000}
                  value={cashGiven}
                  onChange={(e) => setCashGiven(Number(e.target.value) || 0)}
                  style={{ fontSize: '1.2rem', fontWeight: 'bold' }}
                />
                <div className="quick-cash">
                  <button type="button" onClick={() => setCashGiven(total)}>Đúng tiền</button>
                  <button type="button" onClick={() => setCashGiven(100000)}>100k</button>
                  <button type="button" onClick={() => setCashGiven(200000)}>200k</button>
                  <button type="button" onClick={() => setCashGiven(500000)}>500k</button>
                  <button type="button" onClick={() => setCashGiven((c) => c + 50000)}>+50k</button>
                  <button type="button" onClick={() => setCashGiven((c) => c + 100000)}>+100k</button>
                </div>
                <div style={{ marginTop: 12, padding: 8, background: '#fafafa', borderRadius: 6 }}>
                  Tiền thối lại:{' '}
                  <strong style={{ color: cashGiven >= total ? '#389e0d' : '#cf1322', fontSize: '1.1rem' }}>
                    {cashGiven >= total ? money(changeDue) : 'Khách đưa thiếu'}
                  </strong>
                </div>
              </div>
            ) : (
              <div className="qr-box">
                <img
                  src={`https://api.qrserver.com/v1/create-qr-code/?size=160x160&data=OISM_POS_${user.branchId}_${total}`}
                  alt="Mã QR thanh toán"
                />
                <p style={{ margin: '8px 0 0', fontWeight: 600 }}>Quét mã QR để chuyển khoản</p>
                <small className="muted">Thu ngân xác nhận tiền đã vào tài khoản trước khi hoàn tất</small>
              </div>
            )}

            <div className="actions" style={{ marginTop: 20 }}>
              <button type="button" onClick={() => setPayOpen(false)} disabled={busy}>Đóng (Esc)</button>
              <button
                type="button"
                className="primary"
                onClick={handleCheckout}
                disabled={busy || (paymentMethod === 'CASH' && cashGiven < total)}
              >
                {busy ? 'Đang xử lý...' : 'Xác nhận & In bill (F9)'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Modal hóa đơn 80mm */}
      {receipt && (
        <div className="modal" role="dialog" aria-modal="true" aria-label="Hóa đơn thanh toán">
          <div className="card" style={{ width: 380 }}>
            <div className="receipt-paper">
              <h3>{user.tenantName}</h3>
              <div style={{ textAlign: 'center', fontSize: 11 }}>Chi nhánh: {receipt.branchName}</div>
              <div className="dashed" />
              <div className="row"><span>Mã đơn:</span><strong>{receipt.orderCode}</strong></div>
              <div className="row"><span>Ngày giờ:</span><span>{new Date(receipt.completedAt).toLocaleString('vi-VN')}</span></div>
              <div className="row"><span>Thu ngân:</span><span>{receipt.cashierName}</span></div>
              <div className="dashed" />
              <table>
                <thead>
                  <tr><th>Hàng hóa</th><th style={{ textAlign: 'right' }}>SL</th><th style={{ textAlign: 'right' }}>T.Tiền</th></tr>
                </thead>
                <tbody>
                  {receipt.items.map((it) => (
                    <tr key={it.skuId}>
                      <td>{it.productName} ({it.skuCode})</td>
                      <td style={{ textAlign: 'right' }}>{it.quantity}</td>
                      <td style={{ textAlign: 'right' }}>{money(it.lineTotal)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
              <div className="dashed" />
              <div className="row total-row"><span>TỔNG TIỀN:</span><span>{money(receipt.totalAmount)}</span></div>
              <div className="row"><span>PT Thanh toán:</span><span>{receipt.paymentMethod === 'CASH' ? 'Tiền mặt' : 'QR chuyển khoản'}</span></div>
              <div className="row"><span>Tiền khách đưa:</span><span>{money(receipt.cashReceived)}</span></div>
              <div className="row"><span>Tiền thối lại:</span><span>{money(receipt.changeAmount)}</span></div>
              <div className="dashed" />
              <div style={{ textAlign: 'center', marginTop: 8, fontStyle: 'italic', fontSize: 11 }}>
                Cảm ơn quý khách và hẹn gặp lại!
              </div>
            </div>

            <div className="actions no-print" style={{ marginTop: 16, justifyContent: 'center' }}>
              <button type="button" className="primary" onClick={() => window.print()}>🖨️ In hóa đơn</button>
              <button type="button" onClick={() => setReceipt(null)}>Đơn mới (Esc)</button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}