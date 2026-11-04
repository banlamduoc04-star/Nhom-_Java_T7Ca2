import { useEffect, useMemo, useRef, useState } from 'react'
import { Select } from 'antd'
import { skuApi } from '../api/services'
import type { SkuSearchItem } from '../api/types'

interface Props {
  value?: string
  onChange?: (skuId: string | undefined, sku?: SkuSearchItem) => void
  placeholder?: string
  allowClear?: boolean
}

/** O chon SKU: go ma SKU / ten san pham / ma vach de tim (goi GET /skus, debounce 300 ms). */
export default function SkuSelect({ value, onChange, placeholder = 'Gõ mã SKU, tên sản phẩm hoặc mã vạch', allowClear }: Props) {
  const [options, setOptions] = useState<SkuSearchItem[]>([])
  const [loading, setLoading] = useState(false)
  const timer = useRef<number | undefined>(undefined)

  const load = (keyword: string) => {
    window.clearTimeout(timer.current)
    timer.current = window.setTimeout(() => {
      setLoading(true)
      skuApi.search({ keyword, status: 'ACTIVE', size: 20 })
        .then((page) => setOptions((prev) => merge(prev, page.content)))
        .finally(() => setLoading(false))
    }, 300)
  }

  useEffect(() => {
    load('')
    return () => window.clearTimeout(timer.current)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const selectOptions = useMemo(
    () => options.map((s) => ({
      value: s.id,
      label: `${s.code} — ${s.productName}${Object.values(s.attributes).length ? ` (${Object.values(s.attributes).join(', ')})` : ''}`,
    })),
    [options],
  )

  return (
    <Select showSearch value={value} placeholder={placeholder} allowClear={allowClear} loading={loading}
      filterOption={false} onSearch={load} options={selectOptions} style={{ width: '100%' }}
      onChange={(id) => onChange?.(id, options.find((o) => o.id === id))} />
  )
}

function merge(prev: SkuSearchItem[], next: SkuSearchItem[]): SkuSearchItem[] {
  const seen = new Map(prev.map((p) => [p.id, p]))
  next.forEach((n) => seen.set(n.id, n))
  return [...seen.values()]
}
