import { useCallback, useEffect, useState } from 'react'
import { App, Card, DatePicker, Select, Space, Table, Tabs, Tag } from 'antd'
import type { Dayjs } from 'dayjs'
import { branchApi, stockApi } from '../api/services'
import { errorText } from '../api/client'
import SkuSelect from '../components/SkuSelect'
import type { Branch, LedgerRow, StockRow } from '../api/types'

const REASON_LABEL: Record<string, string> = {
  PURCHASE: 'Nhập kho', SALE: 'Bán', RETURN: 'Trả', ADJUST: 'Kiểm kê', TRANSFER_OUT: 'Chuyển đi', TRANSFER_IN: 'Chuyển đến',
}

/** FR-INV-01, FR-RSE-01: ton kho (on_hand, reserved, available) va so cai chi ghi them, loc theo chi nhanh/SKU/ngay. */
export default function StockPage() {
  const { message } = App.useApp()
  const [branches, setBranches] = useState<Branch[]>([])
  const [branchId, setBranchId] = useState<string | undefined>()
  const [skuId, setSkuId] = useState<string | undefined>()
  const [range, setRange] = useState<[Dayjs | null, Dayjs | null] | null>(null)

  const [stock, setStock] = useState<StockRow[]>([])
  const [stockTotal, setStockTotal] = useState(0)
  const [stockPage, setStockPage] = useState(0)
  const [ledger, setLedger] = useState<LedgerRow[]>([])
  const [ledgerTotal, setLedgerTotal] = useState(0)
  const [ledgerPage, setLedgerPage] = useState(0)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    branchApi.list().then(setBranches).catch((e) => message.error(errorText(e)))
  }, [message])

  const loadStock = useCallback(() => {
    setLoading(true)
    stockApi.stock({ branchId, skuId, page: stockPage, size: 20 })
      .then((p) => { setStock(p.content); setStockTotal(p.totalElements) })
      .catch((e) => message.error(errorText(e)))
      .finally(() => setLoading(false))
  }, [branchId, skuId, stockPage, message])

  const loadLedger = useCallback(() => {
    setLoading(true)
    stockApi.ledger({
      branchId, skuId, page: ledgerPage, size: 20,
      from: range?.[0]?.startOf('day').toISOString(),
      to: range?.[1]?.add(1, 'day').startOf('day').toISOString(),
    })
      .then((p) => { setLedger(p.content); setLedgerTotal(p.totalElements) })
      .catch((e) => message.error(errorText(e)))
      .finally(() => setLoading(false))
  }, [branchId, skuId, range, ledgerPage, message])

  useEffect(loadStock, [loadStock])
  useEffect(loadLedger, [loadLedger])

  const branchName = (id: string) => branches.find((b) => b.id === id)?.name ?? id
  const reset = () => { setStockPage(0); setLedgerPage(0) }

  return (
    <Card title="Tồn kho & sổ cái">
      <Space wrap style={{ marginBottom: 16 }}>
        <Select allowClear placeholder="Tất cả chi nhánh" style={{ width: 200 }} options={branches.map((b) => ({ value: b.id, label: b.name }))}
          onChange={(v) => { setBranchId(v); reset() }} />
        <div style={{ width: 320 }}><SkuSelect allowClear placeholder="Lọc theo SKU" onChange={(v) => { setSkuId(v); reset() }} /></div>
      </Space>
      <Tabs items={[
        {
          key: 'stock', label: 'Tồn kho',
          children: (
            <Table<StockRow> rowKey={(r) => `${r.branchId}-${r.skuId}`} loading={loading} dataSource={stock} locale={{ emptyText: 'Chưa có tồn kho' }}
              pagination={{ current: stockPage + 1, pageSize: 20, total: stockTotal, showSizeChanger: false, onChange: (p) => setStockPage(p - 1) }}
              columns={[
                { title: 'Mã SKU', dataIndex: 'skuCode' },
                { title: 'Chi nhánh', dataIndex: 'branchId', render: branchName },
                { title: 'Tồn thực tế (on hand)', dataIndex: 'onHand' },
                { title: 'Đang giữ (reserved)', dataIndex: 'reserved' },
                { title: 'Khả dụng (available)', dataIndex: 'available', render: (v: number) => <Tag color={v <= 0 ? 'red' : 'green'}>{v}</Tag> },
              ]} />
          ),
        },
        {
          key: 'ledger', label: 'Sổ cái',
          children: (
            <>
              <DatePicker.RangePicker style={{ marginBottom: 16 }} onChange={(v) => { setRange(v); setLedgerPage(0) }} />
              <Table<LedgerRow> rowKey="id" loading={loading} dataSource={ledger} locale={{ emptyText: 'Chưa có bút toán nào' }}
                pagination={{ current: ledgerPage + 1, pageSize: 20, total: ledgerTotal, showSizeChanger: false, onChange: (p) => setLedgerPage(p - 1) }}
                columns={[
                  { title: 'Thời gian', dataIndex: 'createdAt', render: (d: string) => new Date(d).toLocaleString('vi-VN') },
                  { title: 'Mã SKU', dataIndex: 'skuCode' },
                  { title: 'Chi nhánh', dataIndex: 'branchId', render: branchName },
                  { title: 'Lý do', dataIndex: 'reason', render: (r: string) => REASON_LABEL[r] ?? r },
                  {
                    title: 'Số lượng', dataIndex: 'quantity',
                    render: (q: number, r) => <strong style={{ color: r.direction === 'IN' ? '#389e0d' : '#cf1322' }}>{r.direction === 'IN' ? '+' : '−'}{q}</strong>,
                  },
                  { title: 'Tồn sau', dataIndex: 'balanceAfter' },
                  { title: 'Đơn giá', dataIndex: 'unitCost', render: (v: number | null) => (v == null ? '' : v.toLocaleString('vi-VN')) },
                  { title: 'Chứng từ', dataIndex: 'refType' },
                ]} />
            </>
          ),
        },
      ]} />
    </Card>
  )
}
