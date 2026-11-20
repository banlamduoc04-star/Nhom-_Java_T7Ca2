import { useState, type FormEvent } from 'react'
import { errorText, login, logout, USE_MOCK, type Me } from './api'

interface Props {
  onLogin: (user: Me) => void
}

/** Chi Owner va Cashier duoc ban tai POS (FR-AUTH-03). */
export default function LoginScreen({ onLogin }: Props) {
  const [identifier, setIdentifier] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  const submit = async (e: FormEvent) => {
    e.preventDefault()
    setBusy(true)
    setError(null)
    try {
      const user = await login(identifier, password)
      if (user.role === 'STAFF') {
        await logout()
        setError('Tài khoản Nhân viên không dùng POS. Hãy dùng trang quản trị.')
        return
      }
      onLogin(user)
    } catch (err) {
      setError(errorText(err))
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="center login-bg">
      <form className="card login" onSubmit={submit}>
        <div className="login-brand">
          <img src="/icon.svg" alt="" width={48} height={48} />
          <div>
            <h1>OISM POS</h1>
            <p className="muted">Bán hàng tại quầy · Thu ngân &amp; Chủ cửa hàng</p>
          </div>
        </div>
        {USE_MOCK && <p className="hint">Đang chạy bằng dữ liệu giả (VITE_USE_MOCK). Bấm Đăng nhập để vào.</p>}
        {error && <div className="alert error" role="alert">{error}</div>}
        <label>
          Email hoặc số điện thoại
          <input value={identifier} onChange={(e) => setIdentifier(e.target.value)} autoFocus autoComplete="username" required={!USE_MOCK} />
        </label>
        <label>
          Mật khẩu
          <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} autoComplete="current-password" required={!USE_MOCK} />
        </label>
        <button type="submit" className="primary" disabled={busy}>{busy ? 'Đang đăng nhập…' : 'Đăng nhập'}</button>
      </form>
    </div>
  )
}
