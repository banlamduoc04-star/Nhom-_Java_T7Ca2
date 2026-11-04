import { useEffect, useRef } from 'react'
import JsBarcode from 'jsbarcode'

interface Props {
  value: string
  label?: string
}

/** Tem ma vach (FR-PROD-03): EAN-13 neu du 13 chu so, nguoc lai Code128. */
export default function BarcodeLabel({ value, label }: Props) {
  const ref = useRef<SVGSVGElement>(null)

  useEffect(() => {
    if (ref.current) {
      const format = /^\d{13}$/.test(value) ? 'EAN13' : 'CODE128'
      try {
        JsBarcode(ref.current, value, { format, width: 2, height: 60, fontSize: 14, margin: 8 })
      } catch {
        JsBarcode(ref.current, value, { format: 'CODE128', width: 2, height: 60, fontSize: 14, margin: 8 })
      }
    }
  }, [value])

  return (
    <div className="barcode-label" style={{ textAlign: 'center' }}>
      {label && <div style={{ fontWeight: 600 }}>{label}</div>}
      <svg ref={ref} />
    </div>
  )
}

/** Mo hop thoai in cua trinh duyet chi voi tem ma vach. */
export function printLabel(elementId: string) {
  const node = document.getElementById(elementId)
  if (!node) {
    return
  }
  const win = window.open('', '_blank', 'width=400,height=300')
  if (!win) {
    return
  }
  win.document.write(`<html><body style="margin:0;font-family:sans-serif">${node.innerHTML}</body></html>`)
  win.document.close()
  win.focus()
  win.print()
  win.close()
}
