import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App'

if ('serviceWorker' in navigator && !window.location.host.includes('localhost:5173')) {
  // Trang dang do service worker cu dieu khien: khi ban moi kich hoat thi tai lai mot lan de hien giao dien moi
  const hadController = navigator.serviceWorker.controller !== null
  let reloaded = false
  navigator.serviceWorker.addEventListener('controllerchange', () => {
    if (hadController && !reloaded) {
      reloaded = true
      window.location.reload()
    }
  })
  window.addEventListener('load', () => {
    navigator.serviceWorker.register('/sw.js', { updateViaCache: 'none' }).catch(() => undefined)
  })
}

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
