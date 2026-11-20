import { useEffect, useState } from 'react'
import QRCode from 'qrcode'

/**
 * Ma QR sinh ngay tren may (FR-POS-03): khong gui ma chi nhanh / so tien ra dich vu ben ngoai.
 */
export default function QrCode({ value, size = 160 }: { value: string; size?: number }) {
  const [src, setSrc] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false
    QRCode.toDataURL(value, { width: size, margin: 1, errorCorrectionLevel: 'M' })
      .then((url) => {
        if (!cancelled) setSrc(url)
      })
      .catch(() => {
        if (!cancelled) setSrc(null)
      })
    return () => {
      cancelled = true
    }
  }, [value, size])

  return src
    ? <img src={src} width={size} height={size} alt="Mã QR thanh toán" />
    : <div style={{ width: size, height: size }} aria-label="Đang tạo mã QR" />
}
