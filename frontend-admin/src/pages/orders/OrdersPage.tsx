import { useCallback, useEffect, useState } from 'react'
import {
  Button, Card, Col, Form, Input, InputNumber, Modal, Row,
  Select, Space, Table, Tag, Typography, message, Popconfirm
} from 'antd'
import {
  CloseCircleOutlined, EyeOutlined,
  PlusOutlined, PrinterOutlined, SyncOutlined, ClockCircleOutlined
} from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import { branchApi, orderApi, skuApi } from '../../api/services'
import type { Branch, OrderChannel, OrderStatus, OrderView, ShippingSlip, SkuSearchItem } from '../../api/types'
import { errorText } from '../../api/client'

//  ĐẾM NGƯỢC THỜI GIAN GIỮ HÀNG 
function ReservedCountdown({ targetDate }: { targetDate: string }) {
  const [timeLeft, setTimeLeft] = useState<number>(new Date(targetDate).getTime() - Date.now())

  useEffect(() => {
    const timer = setInterval(() => setTimeLeft(new Date(targetDate).getTime() - Date.now()), 1000)
    return () => clearInterval(timer)
  }, [targetDate])

  if (timeLeft <= 0) return <Tag color="error">Đã quá hạn</Tag>

  const minutes = Math.floor((timeLeft / 1000 / 60) % 60)
  const seconds = Math.floor((timeLeft / 1000) % 60)
  const isUrgent = timeLeft < 5 * 60 * 1000 // Dưới 5 phút sẽ báo đỏ

  return (
    <Tag color={isUrgent ? 'volcano' : 'warning'} icon={<ClockCircleOutlined />}>
      Còn {minutes}p {seconds}s
    </Tag>
  )
}

const STATUS_COLOR: Record<OrderStatus, string> = {
  DRAFT: 'default',
  RESERVED: 'warning',
  CONFIRMED: 'processing',
  COMPLETED: 'success',
  CANCELLED: 'error',
}

const STATUS_LABEL: Record<OrderStatus, string> = {
  DRAFT: 'Nháp',
  RESERVED: 'Đang giữ hàng',
  CONFIRMED: 'Đã xác nhận',
  COMPLETED: 'Hoàn tất',
  CANCELLED: 'Đã hủy',
}

const money = (v: number | null | undefined) => (v != null ? `${v.toLocaleString('vi-VN')} ₫` : '—')

interface CreateOrderFormValues {
  branchId: string
  customerName?: string
  customerPhone?: string
  shippingAddress?: string
  items: { skuId: string; quantity: number }[]
}

function escapeHtml(str?: string | null): string {
  if (!str) return ''
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

export default function OrdersPage() {
  const [orders, setOrders] = useState<OrderView[]>([])
  const [total, setTotal] = useState(0)
  const [loading, setLoading] = useState(false)
  const [page, setPage] = useState(1)
  const [branches, setBranches] = useState<Branch[]>([])

  // Filters
  const [statusFilter, setStatusFilter] = useState<OrderStatus>()
  const [channelFilter, setChannelFilter] = useState<OrderChannel>()
  const [branchFilter, setBranchFilter] = useState<string>()

  // Detail Modal
  const [selectedOrder, setSelectedOrder] = useState<OrderView | null>(null)
  const [detailOpen, setDetailOpen] = useState(false)

  // Cancel Modal
  const [cancelModalOpen, setCancelModalOpen] = useState(false)
  const [orderToCancel, setOrderToCancel] = useState<OrderView | null>(null)
  const [cancelReason, setCancelReason] = useState('')

  // Create Modal
  const [createOpen, setCreateOpen] = useState(false)
  const [createForm] = Form.useForm<CreateOrderFormValues>()
  const [availableSkus, setAvailableSkus] = useState<SkuSearchItem[]>([])

  const loadBranches = useCallback(async () => {
    try {
      const bs = await branchApi.list()
      setBranches(bs)
    } catch { /* ignore */ }
  }, [])

  const loadOrders = useCallback(async () => {
    setLoading(true)
    try {
      const data = await orderApi.list({
        status: statusFilter,
        channel: channelFilter,
        branchId: branchFilter,
        page: page - 1,
        size: 10,
      })
      setOrders(data.content)
      setTotal(data.totalElements)
    } catch (err) {
      message.error(errorText(err))
    } finally {
      setLoading(false)
    }
  }, [statusFilter, channelFilter, branchFilter, page])

  useEffect(() => { loadBranches() }, [loadBranches])
  useEffect(() => { loadOrders() }, [loadOrders])

  // Hàm xử lý Action
  const handleAction = async (action: 'reserve' | 'confirm' | 'complete', orderId: string) => {
    try {
      let updated: OrderView
      if (action === 'reserve') {
        updated = await orderApi.reserve(orderId)
        message.success('Đã giữ hàng thành công')
      } else if (action === 'confirm') {
        updated = await orderApi.confirm(orderId)
        message.success('Đã xác nhận đơn (Ghi sổ xuất kho & Chụp giá vốn)')
      } else {
        updated = await orderApi.complete(orderId)
        message.success('Đã hoàn tất đơn hàng')
      }
      if (selectedOrder && selectedOrder.id === orderId) setSelectedOrder(updated)
      loadOrders()
    } catch (err) {
      message.error(errorText(err))
    }
  }

  const handleCancel = async () => {
    if (!orderToCancel || !cancelReason.trim()) {
      message.warning('Vui lòng nhập lý do hủy')
      return
    }
    try {
      const updated = await orderApi.cancel(orderToCancel.id, cancelReason.trim())
      message.success('Đã hủy đơn hàng thành công')
      setCancelModalOpen(false)
      setCancelReason('')
      setOrderToCancel(null)
      if (selectedOrder && selectedOrder.id === updated.id) setSelectedOrder(updated)
      loadOrders()
    } catch (err) {
      message.error(errorText(err))
    }
  }

  // FR-ORD-03: du lieu phieu lay tu GET /orders/{id}/shipping-slip (ten san pham, chi nhanh); moi truong deu escape
  const handlePrintSlip = async (order: OrderView) => {
    let slip: ShippingSlip
    try {
      slip = await orderApi.shippingSlip(order.id)
    } catch (err) {
      message.error(errorText(err))
      return
    }
    const printWindow = document.createElement('iframe')
    printWindow.style.position = 'fixed'
    printWindow.style.right = '0'
    printWindow.style.bottom = '0'
    printWindow.style.width = '0'
    printWindow.style.height = '0'
    printWindow.style.border = '0'
    document.body.appendChild(printWindow)

    const doc = printWindow.contentWindow?.document
    if (!doc) return

    const safeCode = escapeHtml(slip.orderCode)
    const safeChannel = escapeHtml(slip.channel)
    const safeBranch = escapeHtml(slip.branchName || '')
    const safeCustomer = escapeHtml(slip.customerName || 'Khách lẻ')
    const safePhone = escapeHtml(slip.customerPhone || '—')
    const safeAddress = escapeHtml(slip.shippingAddress || 'Nhận tại cửa hàng')

    const itemsHtml = slip.lines.map((i, idx) => `
      <tr>
        <td style="padding: 8px; border-bottom: 1px solid #ddd; text-align: center;">${idx + 1}</td>
        <td style="padding: 8px; border-bottom: 1px solid #ddd;"><strong>${escapeHtml(i.skuCode)}</strong><br/>${escapeHtml(i.productName)}</td>
        <td style="padding: 8px; border-bottom: 1px solid #ddd; text-align: center;">${i.quantity}</td>
        <td style="padding: 8px; border-bottom: 1px solid #ddd; text-align: right;">${i.unitPrice.toLocaleString('vi-VN')} ₫</td>
        <td style="padding: 8px; border-bottom: 1px solid #ddd; text-align: right;">${i.lineTotal.toLocaleString('vi-VN')} ₫</td>
      </tr>
    `).join('')

    doc.write(`
      <!DOCTYPE html>
      <html>
        <head>
          <title>Phiếu giao hàng - ${safeCode}</title>
          <style>
            body { font-family: 'Segoe UI', Tahoma, sans-serif; padding: 20px; color: #333; }
            .header { text-align: center; margin-bottom: 20px; }
            .meta { margin-bottom: 16px; font-size: 14px; line-height: 1.6; }
            table { width: 100%; border-collapse: collapse; margin-top: 16px; font-size: 14px; }
            th { background: #f5f5f5; padding: 10px; text-align: left; border-bottom: 2px solid #333; }
            .total { text-align: right; margin-top: 20px; font-size: 16px; font-weight: bold; }
            .footer { margin-top: 40px; display: flex; justify-content: space-between; text-align: center; }
          </style>
        </head>
        <body>
          <div class="header">
            <h2>PHIẾU ĐÓNG GÓI & GIAO HÀNG</h2>
            <p>Mã đơn: <strong>${safeCode}</strong></p>
          </div>
          <div class="meta">
            <p><strong>Chi nhánh xuất:</strong> ${safeBranch} | <strong>Kênh:</strong> ${safeChannel} | <strong>Ngày tạo:</strong> ${new Date(slip.createdAt).toLocaleString('vi-VN')}</p>
            <p><strong>Khách hàng:</strong> ${safeCustomer} - SĐT: ${safePhone}</p>
            <p><strong>Địa chỉ giao:</strong> ${safeAddress}</p>
          </div>
          <table>
            <thead>
              <tr><th>STT</th><th>Mã SKU</th><th style="text-align: center;">SL</th><th style="text-align: right;">Đơn giá</th><th style="text-align: right;">Thành tiền</th></tr>
            </thead>
            <tbody>${itemsHtml}</tbody>
          </table>
          <div class="total">Tổng thanh toán: ${slip.totalAmount.toLocaleString('vi-VN')} ₫</div>
          <div class="footer">
            <div><p>Người lập phiếu</p><small>(Ký, họ tên)</small></div>
            <div><p>Người giao hàng</p><small>(Ký, họ tên)</small></div>
            <div><p>Người nhận hàng</p><small>(Ký, họ tên)</small></div>
          </div>
        </body>
      </html>
    `)
    doc.close()
    printWindow.contentWindow?.focus()
    setTimeout(() => {
      printWindow.contentWindow?.print()
      document.body.removeChild(printWindow)
    }, 500)
  }

  const handleSearchSku = async (keyword: string) => {
    if (!keyword) return
    try {
      const data = await skuApi.search({ keyword, status: 'ACTIVE', size: 10 })
      setAvailableSkus(data.content)
    } catch { /* ignore */ }
  }

  const handleCreateOrder = async (values: CreateOrderFormValues) => {
    try {
      const lines = values.items.map((i) => ({ skuId: i.skuId, quantity: i.quantity }))
      await orderApi.createAndReserve({
        branchId: values.branchId,
        channel: 'MANUAL',
        customerName: values.customerName,
        customerPhone: values.customerPhone,
        shippingAddress: values.shippingAddress,
        lines,
      })
      message.success('Đã tạo và giữ hàng thành công')
      setCreateOpen(false)
      createForm.resetFields()
      loadOrders()
    } catch (err) {
      message.error(errorText(err))
    }
  }

  const columns: ColumnsType<OrderView> = [
    {
      title: 'Mã đơn', dataIndex: 'code', key: 'code',
      render: (code, record) => (
        <a onClick={() => { setSelectedOrder(record); setDetailOpen(true) }}><strong>{code}</strong></a>
      ),
    },
    { title: 'Kênh', dataIndex: 'channel', key: 'channel', render: (ch: OrderChannel) => <Tag>{ch}</Tag> },
    {
      title: 'Trạng thái', dataIndex: 'status', key: 'status',
      render: (st: OrderStatus, record) => (
        <Space direction="vertical" size={0}>
          <Tag color={STATUS_COLOR[st]}>{STATUS_LABEL[st]}</Tag>
          {st === 'RESERVED' && record.reservedUntil && (
            <ReservedCountdown targetDate={record.reservedUntil} />
          )}
        </Space>
      ),
    },
    {
      title: 'Khách hàng', key: 'customer',
      render: (_, r) => (
        <div>
          <div>{r.customerName || '—'}</div>
          <small style={{ color: '#888' }}>{r.customerPhone || ''}</small>
        </div>
      ),
    },
    { title: 'Tổng tiền', dataIndex: 'totalAmount', key: 'totalAmount', align: 'right', render: (v) => <strong>{money(v)}</strong> },
    {
      title: 'Thao tác', key: 'actions', width: 220,
      render: (_, record) => (
        <Space size="small" wrap>
          {record.status === 'DRAFT' && (
            <Button size="small" type="primary" onClick={() => handleAction('reserve', record.id)}>Giữ hàng</Button>
          )}
          {record.status === 'RESERVED' && (
            <Button size="small" type="primary" onClick={() => handleAction('confirm', record.id)}>Duyệt (Trừ kho)</Button>
          )}
          {record.status === 'CONFIRMED' && (
            <Button size="small" style={{ background: '#52c41a', color: '#fff' }} onClick={() => handleAction('complete', record.id)}>Hoàn tất</Button>
          )}
          {['DRAFT', 'RESERVED', 'CONFIRMED'].includes(record.status) && (
            <Popconfirm
              title="Hủy đơn hàng?"
              description="Hành động này không thể hoàn tác."
              onConfirm={() => { setOrderToCancel(record); setCancelModalOpen(true) }}
              okText="Đồng ý"
              cancelText="Đóng"
            >
              <Button size="small" danger>Hủy</Button>
            </Popconfirm>
          )}
          <Button size="small" icon={<EyeOutlined />} onClick={() => { setSelectedOrder(record); setDetailOpen(true) }} />
          <Button size="small" icon={<PrinterOutlined />} onClick={() => handlePrintSlip(record)} />
        </Space>
      ),
    },
  ]

  return (
    <div>
      <Row justify="space-between" align="middle" style={{ marginBottom: 16 }}>
        <Col><Typography.Title level={3} style={{ margin: 0 }}>Quản lý Đơn hàng</Typography.Title></Col>
        <Col>
          <Space>
            <Button icon={<SyncOutlined />} onClick={loadOrders}>Làm mới</Button>
            <Button type="primary" icon={<PlusOutlined />} onClick={() => setCreateOpen(true)}>Tạo đơn hàng</Button>
          </Space>
        </Col>
      </Row>

      <Card style={{ marginBottom: 16 }}>
        <Space wrap>
          <Select placeholder="Trạng thái" allowClear style={{ width: 200 }} value={statusFilter} onChange={setStatusFilter}
            options={Object.entries(STATUS_LABEL).map(([value, label]) => ({ value, label }))} />
          <Select placeholder="Kênh" allowClear style={{ width: 160 }} value={channelFilter} onChange={setChannelFilter}
            options={['MANUAL', 'POS', 'SHOPEE', 'TIKTOK'].map((v) => ({ value: v, label: v }))} />
          <Select placeholder="Chi nhánh" allowClear style={{ width: 200 }} value={branchFilter} onChange={setBranchFilter}
            options={branches.map((b) => ({ value: b.id, label: b.name }))} />
        </Space>
      </Card>

      <Card>
        <Table rowKey="id" columns={columns} dataSource={orders} loading={loading}
          pagination={{ current: page, pageSize: 10, total, onChange: setPage, showTotal: (t) => `Tổng ${t} đơn` }} />
      </Card>

      {/* Modal Chi tiết */}
      <Modal title={`Chi tiết đơn hàng: ${selectedOrder?.code}`} open={detailOpen} onCancel={() => setDetailOpen(false)} width={800}
        footer={[
          <Button key="print" icon={<PrinterOutlined />} onClick={() => selectedOrder && handlePrintSlip(selectedOrder)}>In phiếu</Button>,
          <Button key="close" onClick={() => setDetailOpen(false)}>Đóng</Button>
        ]}>
        {selectedOrder && (
          <div>
            <Row gutter={[16, 16]}>
              <Col span={12}>
                <p><strong>Trạng thái:</strong> <Tag color={STATUS_COLOR[selectedOrder.status]}>{STATUS_LABEL[selectedOrder.status]}</Tag></p>
                <p><strong>Kênh bán:</strong> <Tag>{selectedOrder.channel}</Tag></p>
                {selectedOrder.reservedUntil && selectedOrder.status === 'RESERVED' && (
                  <p><strong>Hạn giữ hàng:</strong> <ReservedCountdown targetDate={selectedOrder.reservedUntil} /></p>
                )}
              </Col>
              <Col span={12}>
                <p><strong>Khách hàng:</strong> {selectedOrder.customerName || 'Khách lẻ'}</p>
                <p><strong>SĐT:</strong> {selectedOrder.customerPhone || '—'}</p>
                <p><strong>Địa chỉ:</strong> {selectedOrder.shippingAddress || 'Tại quầy'}</p>
              </Col>
            </Row>

            <Typography.Title level={5} style={{ marginTop: 16 }}>Sản phẩm</Typography.Title>
            <Table rowKey="skuId" pagination={false} dataSource={selectedOrder.items}
              columns={[
                { title: 'SKU', dataIndex: 'skuCode' },
                { title: 'SL', dataIndex: 'quantity', align: 'center' },
                { title: 'Đơn giá', dataIndex: 'unitPrice', align: 'right', render: money },
                { title: 'Giá vốn chụp', dataIndex: 'costPrice', align: 'right', render: (v) => v ? <span style={{ color: '#0958d9' }}>{money(v)}</span> : <span style={{ color: '#888' }}>Chưa có</span> },
                { title: 'Thành tiền', dataIndex: 'lineTotal', align: 'right', render: (v) => <strong>{money(v)}</strong> },
              ]} />
            <div style={{ textAlign: 'right', marginTop: 16, fontSize: 18 }}>
              Tổng cộng: <strong style={{ color: '#cf1322' }}>{money(selectedOrder.totalAmount)}</strong>
            </div>

            <Typography.Title level={5} style={{ marginTop: 24 }}>Lịch sử trạng thái (State Machine)</Typography.Title>
            <Table rowKey="createdAt" pagination={false} dataSource={selectedOrder.history}
              columns={[
                {
                  title: 'Chuyển trạng thái', key: 'transition',
                  render: (_, h) => (
                    <span>
                      {h.fromStatus ? <Tag color={STATUS_COLOR[h.fromStatus]}>{STATUS_LABEL[h.fromStatus]}</Tag> : 'Khởi tạo'}
                      {' → '}
                      <Tag color={STATUS_COLOR[h.toStatus]}>{STATUS_LABEL[h.toStatus]}</Tag>
                    </span>
                  ),
                },
                { title: 'Người làm / Lý do', dataIndex: 'reason', render: (r) => r || '—' },
                { title: 'Thời gian', dataIndex: 'createdAt', render: (t) => new Date(t).toLocaleString('vi-VN') },
              ]} />
          </div>
        )}
      </Modal>

      {/* Modal Hủy */}
      <Modal title={`Hủy đơn hàng: ${orderToCancel?.code}`} open={cancelModalOpen} onOk={handleCancel}
        onCancel={() => { setCancelModalOpen(false); setOrderToCancel(null); setCancelReason('') }}
        okText="Xác nhận hủy" okButtonProps={{ danger: true }}>
        <p>Vui lòng nhập lý do hủy. Hệ thống sẽ tự động <strong>nhả tồn kho</strong> (nếu đang giữ) hoặc <strong>hoàn kho</strong> (nếu đã trừ kho).</p>
        <Input.TextArea rows={3} placeholder="Ví dụ: Khách đổi ý..." value={cancelReason} onChange={(e) => setCancelReason(e.target.value)} />
      </Modal>

      {/* Modal Tạo đơn */}
      <Modal title="Tạo đơn hàng thủ công" open={createOpen} onCancel={() => { setCreateOpen(false); createForm.resetFields() }} onOk={() => createForm.submit()} width={700}>
        <Form form={createForm} layout="vertical" onFinish={handleCreateOrder}>
          <Form.Item name="branchId" label="Chi nhánh xuất bán" rules={[{ required: true, message: 'Chọn chi nhánh' }]}>
            <Select options={branches.map((b) => ({ value: b.id, label: b.name }))} placeholder="Chọn chi nhánh" />
          </Form.Item>
          <Row gutter={16}>
            <Col span={12}><Form.Item name="customerName" label="Tên khách hàng"><Input /></Form.Item></Col>
            <Col span={12}><Form.Item name="customerPhone" label="Số điện thoại"><Input /></Form.Item></Col>
          </Row>
          <Form.Item name="shippingAddress" label="Địa chỉ"><Input /></Form.Item>

          <Typography.Text strong>Danh sách sản phẩm</Typography.Text>
          <Form.List name="items" initialValue={[{ skuId: undefined, quantity: 1 }]}>
            {(fields, { add, remove }) => (
              <div style={{ marginTop: 8 }}>
                {fields.map(({ key, name, ...restField }) => (
                  <Space key={key} style={{ display: 'flex', marginBottom: 8 }} align="baseline">
                    <Form.Item {...restField} name={[name, 'skuId']} rules={[{ required: true, message: 'Chọn SKU' }]} style={{ width: 400 }}>
                      <Select showSearch placeholder="Tìm SKU" filterOption={false} onSearch={handleSearchSku}
                        options={availableSkus.map((s) => ({ value: s.id, label: `${s.code} - ${s.productName} (Còn ${s.available})` }))} />
                    </Form.Item>
                    <Form.Item {...restField} name={[name, 'quantity']} rules={[{ required: true }]}>
                      <InputNumber min={1} placeholder="SL" />
                    </Form.Item>
                    {fields.length > 1 && <CloseCircleOutlined onClick={() => remove(name)} style={{ color: 'red' }} />}
                  </Space>
                ))}
                <Button type="dashed" onClick={() => add()} block icon={<PlusOutlined />}>Thêm SKU</Button>
              </div>
            )}
          </Form.List>
        </Form>
      </Modal>
    </div>
  )
}
