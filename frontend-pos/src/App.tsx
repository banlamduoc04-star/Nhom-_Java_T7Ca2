import { useEffect, useState } from 'react'
import { fetchMe, tokenStore, USE_MOCK, type Me } from './api'
import LoginScreen from './LoginScreen'
import SaleScreen from './SaleScreen'

export default function App() {
  const [user, setUser] = useState<Me | null>(null)
  const [checking, setChecking] = useState(() => tokenStore.get() !== null && !USE_MOCK)

  useEffect(() => {
    if (!checking) {
      return
    }
    fetchMe().then(setUser).catch(() => tokenStore.clear()).finally(() => setChecking(false))
  }, [checking])

  if (checking) {
    return <div className="center">Đang tải…</div>
  }
  if (!user) {
    return <LoginScreen onLogin={setUser} />
  }
  return <SaleScreen user={user} onLogout={() => setUser(null)} />
}
