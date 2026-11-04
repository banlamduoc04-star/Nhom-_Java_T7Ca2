import { useCallback, useEffect, useState } from 'react'
import {
  Button, Card, Col, Form, Input, InputNumber, Modal, Row,
  Select, Space, Table, Tag, Typography, message,
} from 'antd'
import { CloseOutlined, EyeOutlined, PlusOutlined, PrinterOutlined, SyncOutlined } from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import { branchApi, orderApi, skuApi } from '../api/services'
import type { Branch, OrderChannel, OrderStatus, OrderView, SkuSearchItem } from '../api/types'

const STATUS_COLOR: Record<OrderStatus, string> = {
  DRAFT: 'default',
  RESERVED: 'warning',
  CONFIRMED: 'processing',
  COMPLETED: 'success',
  CANCELLED: 'error',
}

const STATUS_LABEL: Record<OrderStatus, string> = {
  DRAFT: 'Nháp',
  RESERVED: 'Đã giữ hàng',
  CONFIRMED: 'Đã xác nhận (trừ kho)',
  COMPLETED: 'Hoàn tất',
  CANCELLED: 'Đã hủy',
}

const CHANNEL_COLOR: Record<OrderChannel, string> = {
  MANUAL: 'default',
  POS: 'blue',
  SHOPEE: 'orange',
  LAZADA: 'geekblue',
  TIKTOK: 'purple',
}

const money = (v: number | null | undefined) => (v != null ? `${v.toLocaleString('vi-VN')} ₫` : '—')

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
  const [createForm] = Form.useForm()
  const [availableSkus, setAvailableSkus] = useState<SkuSearchItem[]>([])

  const loadBranches = useCallback(async () => {
    try {
      const bs = await branchApi.list()
      setBranches(bs)
    } catch {
      // ignore
    }
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
    } catch (err: any) {
      message.error(err.response?.data?.message || 'Không thể tải danh sách đơn hàng')
    } finally {
      setLoading(false)
    }
  }, [statusFilter, channelFilter, branchFilter, page])

  useEffect(() => {
    loadBranches()
  }, [loadBranches])

  useEffect(() => {
    loadOrders()
  }, [loadOrders])

  const handleAction = async (action: 'reserve' | 'confirm' | 'complete', orderId: string) => {
    try {
      let updated: OrderView
      if (action === 'reserve') {
        updated = await orderApi.reserve(orderId)
        message.success('Đã giữ hàng thành công')
      } else if (action === 'confirm') {
        updated = await orderApi.confirm(orderId)
        message.success('Đã xác nhận đơn hàng (ghi sổ xuất kho & chụp giá vốn)')
      } else {
        updated = await orderApi.complete(orderId)
        message.success('Đã hoàn tất đơn hàng')
      }
      if (selectedOrder && selectedOrder.id === orderId) {
        setSelectedOrder(updated)
      }
      loadOrders()
    } catch (err: any) {
      message.error(err.response?.data?.message || 'Thao tác thất bại')
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
      if (selectedOrder && selectedOrder.id === updated.id) {
        setSelectedOrder(updated)
      }
      loadOrders()
    } catch (err: any) {
      message.error(err.response?.data?.message || 'Hủy đơn hàng thất bại')
    }
  }

  const handlePrintSlip = (order: OrderView) => {
    const printWindow = window.open('', '_blank', 'width=700,height=800')
    if (!printWindow) return
    const itemsHtml = order.items
      .map(
        (i, idx) => `
      <tr>
        <td style="padding: 8px; border-bottom: 1px solid #ddd;">${idx + 1}</td>
        <td style="padding: 8px; border-bottom: 1px solid #ddd;"><strong>${i.skuCode}</strong></td>
        <td style="padding: 8px; border-bottom: 1px solid #ddd; text-align: center;">${i.quantity}</td>
        <td style="padding: 8px; border-bottom: 1px solid #ddd; text-align: right;">${i.unitPrice.toLocaleString('vi-VN')} ₫</td>
        <td style="padding: 8px; border-bottom: 1px solid #ddd; text-align: right;">${i.lineTotal.toLocaleString('vi-VN')} ₫</td>
      </tr>
    `,
      )
      .join('')

    printWindow.document.write(`
      <!DOCTYPE html>
      <html>
        <head>
          <title>Phiếu đóng gói - ${order.code}</title>
          <style>
            body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; padding: 24px; color: #333; }
            h2 { margin-bottom: 4px; }
            .meta { margin-bottom: 16px; font-size: 14px; }
            table { width: 100%; border-collapse: collapse; margin-top: 16px; }
            th { background: #f5f5f5; padding: 8px; text-align: left; border-bottom: 2px solid #ccc; }
            .total { text-align: right; margin-top: 20px; font-size: 16px; font-weight: bold; }
            .footer { margin-top: 40px; display: flex; justify-content: space-between; text-align: center; }
            @media print {
              button { display: none; }
            }
          </style>
        </head>
        <body>
          <h2>PHIẾU ĐÓNG GÓI & GIAO HÀNG</h2>
          <div class="meta">
            <p><strong>Mã đơn hàng:</strong> ${order.code} | <strong>Kênh:</strong> ${order.channel}</p>
            <p><strong>Ngày tạo:</strong> ${new Date(order.createdAt).toLocaleString('vi-VN')}</p>
            <p><strong>Khách hàng:</strong> ${order.customerName || 'Khách lẻ'} - SĐT: ${order.customerPhone || '—'}</p>
            <p><strong>Địa chỉ giao:</strong> ${order.shippingAddress || 'Nhận tại cửa hàng'}</p>
          </div>
          <table>
            <thead>
              <tr><th>STT</th><th>Mã SKU</th><th style="text-align: center;">Số lượng</th><th style="text-align: right;">Đơn giá</th><th style="text-align: right;">Thành tiền</th></tr>
            </thead>
            <tbody>${itemsHtml}</tbody>
          </table>
          <div class="total">
            Tổng tiền thanh toán: ${order.totalAmount.toLocaleString('vi-VN')} ₫
          </div>
          <div class="footer">
            <div><p>Người lập phiếu</p><small>(Ký, họ tên)</small></div>
            <div><p>Người giao hàng</p><small>(Ký, họ tên)</small></div>
            <div><p>Người nhận hàng</p><small>(Ký, họ tên)</small></div>
          </div>
          <script>
            window.onload = function() { window.print(); }
          </script>
        </body>
      </html>
    `)
    printWindow.document.close()
  }

  const handleSearchSku = async (keyword: string) => {
    if (!keyword) return
    try {
      const data = await skuApi.search({ keyword, status: 'ACTIVE', size: 10 })
      setAvailableSkus(data.content)
    } catch {
      // ignore
    }
  }

  const handleCreateOrder = async (values: any) => {
    try {
      const lines = values.items.map((i: any) => ({
        skuId: i.skuId,
        quantity: i.quantity,
      }))
      await orderApi.createAndReserve({
        branchId: values.branchId,
        channel: 'MANUAL',
        customerName: values.customerName,
        customerPhone: values.customerPhone,
        shippingAddress: values.shippingAddress,
        lines,
      })
      message.success('Đã tạo và giữ hàng đơn hàng thành công')
      setCreateOpen(false)
      createForm.resetFields()
      loadOrders()
    } catch (err: any) {
      message.error(err.response?.data?.message || 'Tạo đơn hàng thất bại')
    }
  }

  const columns: ColumnsType<OrderView> = [
    {
      title: 'Mã đơn',
      dataIndex: 'code',
      key: 'code',
      render: (code, record) => (
        <a onClick={() => { setSelectedOrder(record); setDetailOpen(true) }}>
          <strong>{code}</strong>
        </a>
      ),
    },
    {
      title: 'Kênh',
      dataIndex: 'channel',
      key: 'channel',
      render: (ch: OrderChannel) => <Tag color={CHANNEL_COLOR[ch]}>{ch}</Tag>,
    },
    {
      title: 'Khách hàng',
      key: 'customer',
      render: (_, r) => (
        <div>
          <div>{r.customerName || '—'}</div>
          <small style={{ color: '#888' }}>{r.customerPhone || ''}</small>
        </div>
      ),
    },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      key: 'status',
      render: (st: OrderStatus) => <Tag color={STATUS_COLOR[st]}>{STATUS_LABEL[st]}</Tag>,
    },
    {
      title: 'Tổng tiền',
      dataIndex: 'totalAmount',
      key: 'totalAmount',
      align: 'right',
      render: (v) => <strong>{money(v)}</strong>,
    },
    {
      title: 'Thời gian tạo',
      dataIndex: 'createdAt',
      key: 'createdAt',
      render: (t) => new Date(t).toLocaleString('vi-VN'),
    },
    {
      title: 'Thao tác',
      key: 'actions',
      render: (_, record) => (
        <Space size="small">
          <Button size="small" icon={<EyeOutlined />} onClick={() => { setSelectedOrder(record); setDetailOpen(true) }} />
          <Button size="small" icon={<PrinterOutlined />} title="In phiếu đóng gói" onClick={() => handlePrintSlip(record)} />
          {record.status === 'DRAFT' && (
            <Button size="small" type="primary" onClick={() => handleAction('reserve', record.id)}>
              Giữ hàng
            </Button>
          )}
          {record.status === 'RESERVED' && (
            <Button size="small" type="primary" onClick={() => handleAction('confirm', record.id)}>
              Xác nhận
            </Button>
          )}
          {record.status === 'CONFIRMED' && (
            <Button size="small" type="primary" style={{ background: '#52c41a', borderColor: '#52c41a' }} onClick={() => handleAction('complete', record.id)}>
              Hoàn tất
            </Button>
          )}
          {['DRAFT', 'RESERVED', 'CONFIRMED'].includes(record.status) && (
            <Button size="small" danger onClick={() => { setOrderToCancel(record); setCancelModalOpen(true) }}>
              Hủy
            </Button>
          )}
        </Space>
      ),
    },
  ]

  return (
    <div>
      <Row justify="space-between" align="middle" style={{ marginBottom: 16 }}>
        <Col>
          <Typography.Title level={3} style={{ margin: 0 }}>Quản lý Đơn hàng</Typography.Title>
        </Col>
        <Col>
          <Space>
            <Button icon={<SyncOutlined />} onClick={loadOrders}>Làm mới</Button>
            <Button type="primary" icon={<PlusOutlined />} onClick={() => setCreateOpen(true)}>Tạo đơn hàng</Button>
          </Space>
        </Col>
      </Row>

      <Card style={{ marginBottom: 16 }}>
        <Space wrap>
          <Select
            placeholder="Lọc theo trạng thái"
            allowClear
            style={{ width: 200 }}
            value={statusFilter}
            onChange={setStatusFilter}
            options={[
              { value: 'DRAFT', label: 'Nháp' },
              { value: 'RESERVED', label: 'Đã giữ hàng' },
              { value: 'CONFIRMED', label: 'Đã xác nhận' },
              { value: 'COMPLETED', label: 'Hoàn tất' },
              { value: 'CANCELLED', label: 'Đã hủy' },
            ]}
          />
          <Select
            placeholder="Lọc theo kênh"
            allowClear
            style={{ width: 160 }}
            value={channelFilter}
            onChange={setChannelFilter}
            options={[
              { value: 'MANUAL', label: 'Thủ công' },
              { value: 'POS', label: 'Tại quầy (POS)' },
              { value: 'SHOPEE', label: 'Shopee' },
              { value: 'LAZADA', label: 'Lazada' },
              { value: 'TIKTOK', label: 'TikTok Shop' },
            ]}
          />
          <Select
            placeholder="Lọc theo chi nhánh"
            allowClear
            style={{ width: 200 }}
            value={branchFilter}
            onChange={setBranchFilter}
            options={branches.map((b) => ({ value: b.id, label: b.name }))}
          />
        </Space>
      </Card>

      <Card>
        <Table
          rowKey="id"
          columns={columns}
          dataSource={orders}
          loading={loading}
          pagination={{
            current: page,
            pageSize: 10,
            total,
            onChange: setPage,
            showTotal: (t) => `Tổng cộng ${t} đơn hàng`,
          }}
        />
      </Card>

      {/* Chi tiết đơn hàng modal */}
      <Modal
        title={`Chi tiết đơn hàng: ${selectedOrder?.code}`}
        open={detailOpen}
        onCancel={() => setDetailOpen(false)}
        width={800}
        footer={[
          <Button key="print" icon={<PrinterOutlined />} onClick={() => selectedOrder && handlePrintSlip(selectedOrder)}>
            In phiếu đóng gói
          </Button>,
          selectedOrder?.status === 'DRAFT' && (
            <Button key="reserve" type="primary" onClick={() => handleAction('reserve', selectedOrder.id)}>
              Giữ hàng
            </Button>
          ),
          selectedOrder?.status === 'RESERVED' && (
            <Button key="confirm" type="primary" onClick={() => handleAction('confirm', selectedOrder.id)}>
              Xác nhận (Trừ kho)
            </Button>
          ),
          selectedOrder?.status === 'CONFIRMED' && (
            <Button key="complete" type="primary" style={{ background: '#52c41a' }} onClick={() => handleAction('complete', selectedOrder.id)}>
              Hoàn tất đơn
            </Button>
          ),
          selectedOrder && ['DRAFT', 'RESERVED', 'CONFIRMED'].includes(selectedOrder.status) && (
            <Button key="cancel" danger onClick={() => { setOrderToCancel(selectedOrder); setCancelModalOpen(true) }}>
              Hủy đơn
            </Button>
          ),
          <Button key="close" onClick={() => setDetailOpen(false)}>Đóng</Button>,
        ]}
      >
        {selectedOrder && (
          <div>
            <Row gutter={[16, 16]}>
              <Col span={12}>
                <p><strong>Trạng thái:</strong> <Tag color={STATUS_COLOR[selectedOrder.status]}>{STATUS_LABEL[selectedOrder.status]}</Tag></p>
                <p><strong>Kênh bán:</strong> <Tag color={CHANNEL_COLOR[selectedOrder.channel]}>{selectedOrder.channel}</Tag></p>
                {selectedOrder.externalOrderId && (
                  <p><strong>Mã đơn sàn:</strong> {selectedOrder.externalOrderId}</p>
                )}
                {selectedOrder.reservedUntil && (
                  <p><strong>Hạn giữ hàng:</strong> {new Date(selectedOrder.reservedUntil).toLocaleString('vi-VN')}</p>
                )}
              </Col>
              <Col span={12}>
                <p><strong>Khách hàng:</strong> {selectedOrder.customerName || 'Khách lẻ'}</p>
                <p><strong>SĐT:</strong> {selectedOrder.customerPhone || '—'}</p>
                <p><strong>Địa chỉ giao:</strong> {selectedOrder.shippingAddress || 'Tại quầy'}</p>
                <p><strong>Ngày tạo:</strong> {new Date(selectedOrder.createdAt).toLocaleString('vi-VN')}</p>
              </Col>
            </Row>

            <Typography.Title level={5} style={{ marginTop: 16 }}>Dòng sản phẩm</Typography.Title>
            <Table
              rowKey="skuId"
              pagination={false}
              dataSource={selectedOrder.items}
              columns={[
                { title: 'SKU', dataIndex: 'skuCode', key: 'skuCode' },
                { title: 'Số lượng', dataIndex: 'quantity', key: 'quantity', align: 'center' },
                { title: 'Đơn giá bán', dataIndex: 'unitPrice', key: 'unitPrice', align: 'right', render: (v) => money(v) },
                {
                  title: 'Giá vốn chụp',
                  dataIndex: 'costPrice',
                  key: 'costPrice',
                  align: 'right',
                  render: (v) => (v != null ? <span style={{ color: '#0958d9' }}>{money(v)}</span> : <span style={{ color: '#888' }}>Chưa chụp</span>),
                },
                { title: 'Thành tiền', dataIndex: 'lineTotal', key: 'lineTotal', align: 'right', render: (v) => <strong>{money(v)}</strong> },
              ]}
              summary={() => (
                <Table.Summary.Row>
                  <Table.Summary.Cell index={0} colSpan={4}><strong>Tổng tiền đơn hàng:</strong></Table.Summary.Cell>
                  <Table.Summary.Cell index={1} align="right">
                    <strong style={{ color: '#cf1322', fontSize: 16 }}>{money(selectedOrder.totalAmount)}</strong>
                  </Table.Summary.Cell>
                </Table.Summary.Row>
              )}
            />

            <Typography.Title level={5} style={{ marginTop: 16 }}>Lịch sử trạng thái</Typography.Title>
            <Table
              rowKey="createdAt"
              pagination={false}
              dataSource={selectedOrder.history}
              columns={[
                {
                  title: 'Chuyển trạng thái',
                  key: 'transition',
                  render: (_, h) => (
                    <span>
                      {h.fromStatus ? <Tag color={STATUS_COLOR[h.fromStatus]}>{STATUS_LABEL[h.fromStatus]}</Tag> : 'Khởi tạo'}
                      {' → '}
                      <Tag color={STATUS_COLOR[h.toStatus]}>{STATUS_LABEL[h.toStatus]}</Tag>
                    </span>
                  ),
                },
                { title: 'Lý do / Ghi chú', dataIndex: 'reason', key: 'reason' },
                { title: 'Thời gian', dataIndex: 'createdAt', key: 'createdAt', render: (t) => new Date(t).toLocaleString('vi-VN') },
              ]}
            />
          </div>
        )}
      </Modal>

      {/* Modal Hủy đơn */}
      <Modal
        title={`Hủy đơn hàng: ${orderToCancel?.code}`}
        open={cancelModalOpen}
        onOk={handleCancel}
        onCancel={() => { setCancelModalOpen(false); setOrderToCancel(null); setCancelReason('') }}
        okText="Xác nhận hủy"
        okButtonProps={{ danger: true }}
      >
        <p>Vui lòng nhập lý do hủy đơn (hệ thống sẽ tự động nhả tồn giữ hàng hoặc hoàn lại tồn kho nếu đơn đã xác nhận):</p>
        <Input.TextArea
          rows={3}
          placeholder="Ví dụ: Khách đổi ý, hết hàng, sai địa chỉ..."
          value={cancelReason}
          onChange={(e) => setCancelReason(e.target.value)}
        />
      </Modal>

      {/* Modal Tạo đơn */}
      <Modal
        title="Tạo đơn hàng thủ công"
        open={createOpen}
        onCancel={() => { setCreateOpen(false); createForm.resetFields() }}
        onOk={() => createForm.submit()}
        width={700}
      >
        <Form form={createForm} layout="vertical" onFinish={handleCreateOrder}>
          <Form.Item name="branchId" label="Chi nhánh xuất bán" rules={[{ required: true, message: 'Chọn chi nhánh' }]}>
            <Select options={branches.map((b) => ({ value: b.id, label: b.name }))} placeholder="Chọn chi nhánh" />
          </Form.Item>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="customerName" label="Tên khách hàng">
                <Input placeholder="Nguyễn Văn A" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="customerPhone" label="Số điện thoại">
                <Input placeholder="0901234567" />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="shippingAddress" label="Địa chỉ giao hàng">
            <Input placeholder="Địa chỉ chi tiết" />
          </Form.Item>

          <Typography.Text strong>Danh sách sản phẩm</Typography.Text>
          <Form.List name="items" initialValue={[{ skuId: undefined, quantity: 1 }]}>
            {(fields, { add, remove }) => (
              <div style={{ marginTop: 8 }}>
                {fields.map(({ key, name, ...restField }) => (
                  <Space key={key} style={{ display: 'flex', marginBottom: 8 }} align="baseline">
                    <Form.Item
                      {...restField}
                      name={[name, 'skuId']}
                      rules={[{ required: true, message: 'Chọn sản phẩm SKU' }]}
                      style={{ width: 400 }}
                    >
                      <Select
                        showSearch
                        placeholder="Gõ mã hoặc tên SKU để tìm"
                        filterOption={false}
                        onSearch={handleSearchSku}
                        options={availableSkus.map((s) => ({
                          value: s.id,
                          label: `${s.code} - ${s.productName} (Còn ${s.available}) - ${s.retailPrice.toLocaleString('vi-VN')} ₫`,
                        }))}
                      />
                    </Form.Item>
                    <Form.Item
                      {...restField}
                      name={[name, 'quantity']}
                      rules={[{ required: true, message: 'Số lượng' }]}
                    >
                      <InputNumber min={1} placeholder="SL" />
                    </Form.Item>
                    {fields.length > 1 && (
                      <CloseOutlined onClick={() => remove(name)} style={{ color: 'red' }} />
                    )}
                  </Space>
                ))}
                <Button type="dashed" onClick={() => add()} block icon={<PlusOutlined />}>
                  Thêm dòng sản phẩm
                </Button>
              </div>
            )}
          </Form.List>
        </Form>
      </Modal>
    </div>
  )
}
