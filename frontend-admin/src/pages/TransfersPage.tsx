import { useCallback, useEffect, useState } from 'react'
import {
  Button, Card, Col, Form, Input, InputNumber, Modal, Row, Select, Space, Table, Tag, Typography, message,
} from 'antd'
import { CheckOutlined, CloseOutlined, EyeOutlined, PlusOutlined, SendOutlined, SyncOutlined } from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import { branchApi, skuApi, transferApi } from '../api/services'
import type { Branch, SkuSearchItem, TransferStatus, TransferView } from '../api/types'

const STATUS_COLOR: Record<TransferStatus, string> = {
  DRAFT: 'default',
  DISPATCHED: 'processing',
  RECEIVED: 'success',
  CANCELLED: 'error',
}

const STATUS_LABEL: Record<TransferStatus, string> = {
  DRAFT: 'Nháp (chưa xuất)',
  DISPATCHED: 'Đang chuyển (đã xuất kho)',
  RECEIVED: 'Đã nhận (nhập kho đích)',
  CANCELLED: 'Đã hủy',
}

export default function TransfersPage() {
  const [transfers, setTransfers] = useState<TransferView[]>([])
  const [total, setTotal] = useState(0)
  const [loading, setLoading] = useState(false)
  const [page, setPage] = useState(1)
  const [statusFilter, setStatusFilter] = useState<TransferStatus>()
  const [branches, setBranches] = useState<Branch[]>([])

  // Detail Modal
  const [selectedTransfer, setSelectedTransfer] = useState<TransferView | null>(null)
  const [detailOpen, setDetailOpen] = useState(false)

  // Cancel Modal
  const [cancelOpen, setCancelOpen] = useState(false)
  const [transferToCancel, setTransferToCancel] = useState<TransferView | null>(null)
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

  const loadTransfers = useCallback(async () => {
    setLoading(true)
    try {
      const data = await transferApi.list({
        status: statusFilter,
        page: page - 1,
        size: 10,
      })
      setTransfers(data.content)
      setTotal(data.totalElements)
    } catch (err: any) {
      message.error(err.response?.data?.message || 'Không thể tải danh sách chuyển kho')
    } finally {
      setLoading(false)
    }
  }, [statusFilter, page])

  useEffect(() => {
    loadBranches()
  }, [loadBranches])

  useEffect(() => {
    loadTransfers()
  }, [loadTransfers])

  const branchName = (id: string) => branches.find((b) => b.id === id)?.name || id

  const handleDispatch = async (id: string) => {
    try {
      const updated = await transferApi.dispatch(id)
      message.success('Đã xuất kho chuyển hàng thành công')
      if (selectedTransfer?.id === id) setSelectedTransfer(updated)
      loadTransfers()
    } catch (err: any) {
      message.error(err.response?.data?.message || 'Xuất chuyển kho thất bại')
    }
  }

  const handleReceive = async (id: string) => {
    try {
      const updated = await transferApi.receive(id)
      message.success('Đã nhập kho đích thành công')
      if (selectedTransfer?.id === id) setSelectedTransfer(updated)
      loadTransfers()
    } catch (err: any) {
      message.error(err.response?.data?.message || 'Nhận hàng thất bại')
    }
  }

  const handleCancel = async () => {
    if (!transferToCancel || !cancelReason.trim()) {
      message.warning('Vui lòng nhập lý do hủy')
      return
    }
    try {
      const updated = await transferApi.cancel(transferToCancel.id, cancelReason.trim())
      message.success('Đã hủy phiếu chuyển kho')
      setCancelOpen(false)
      setCancelReason('')
      setTransferToCancel(null)
      if (selectedTransfer?.id === updated.id) setSelectedTransfer(updated)
      loadTransfers()
    } catch (err: any) {
      message.error(err.response?.data?.message || 'Hủy thất bại')
    }
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

  const handleCreate = async (values: any) => {
    if (values.fromBranchId === values.toBranchId) {
      message.error('Chi nhánh nguồn và chi nhánh đích phải khác nhau')
      return
    }
    try {
      await transferApi.create({
        fromBranchId: values.fromBranchId,
        toBranchId: values.toBranchId,
        note: values.note,
        items: values.items.map((i: any) => ({ skuId: i.skuId, quantity: i.quantity })),
      })
      message.success('Đã tạo phiếu chuyển kho thành công')
      setCreateOpen(false)
      createForm.resetFields()
      loadTransfers()
    } catch (err: any) {
      message.error(err.response?.data?.message || 'Tạo phiếu chuyển kho thất bại')
    }
  }

  const columns: ColumnsType<TransferView> = [
    {
      title: 'Mã phiếu',
      dataIndex: 'code',
      key: 'code',
      render: (code, r) => (
        <a onClick={() => { setSelectedTransfer(r); setDetailOpen(true) }}>
          <strong>{code}</strong>
        </a>
      ),
    },
    {
      title: 'Kho nguồn',
      dataIndex: 'fromBranchId',
      key: 'fromBranchId',
      render: (id) => branchName(id),
    },
    {
      title: 'Kho đích',
      dataIndex: 'toBranchId',
      key: 'toBranchId',
      render: (id) => branchName(id),
    },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      key: 'status',
      render: (st: TransferStatus) => <Tag color={STATUS_COLOR[st]}>{STATUS_LABEL[st]}</Tag>,
    },
    {
      title: 'Ghi chú',
      dataIndex: 'note',
      key: 'note',
      render: (n) => n || '—',
    },
    {
      title: 'Ngày tạo',
      dataIndex: 'createdAt',
      key: 'createdAt',
      render: (t) => new Date(t).toLocaleString('vi-VN'),
    },
    {
      title: 'Thao tác',
      key: 'actions',
      render: (_, r) => (
        <Space size="small">
          <Button size="small" icon={<EyeOutlined />} onClick={() => { setSelectedTransfer(r); setDetailOpen(true) }} />
          {r.status === 'DRAFT' && (
            <Button size="small" type="primary" icon={<SendOutlined />} onClick={() => handleDispatch(r.id)}>
              Xuất chuyển
            </Button>
          )}
          {r.status === 'DISPATCHED' && (
            <Button size="small" type="primary" icon={<CheckOutlined />} style={{ background: '#52c41a', borderColor: '#52c41a' }} onClick={() => handleReceive(r.id)}>
              Nhận hàng
            </Button>
          )}
          {r.status === 'DRAFT' && (
            <Button size="small" danger onClick={() => { setTransferToCancel(r); setCancelOpen(true) }}>
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
          <Typography.Title level={3} style={{ margin: 0 }}>Chuyển kho liên chi nhánh</Typography.Title>
        </Col>
        <Col>
          <Space>
            <Button icon={<SyncOutlined />} onClick={loadTransfers}>Làm mới</Button>
            <Button type="primary" icon={<PlusOutlined />} onClick={() => setCreateOpen(true)}>Tạo phiếu chuyển kho</Button>
          </Space>
        </Col>
      </Row>

      <Card style={{ marginBottom: 16 }}>
        <Select
          placeholder="Lọc theo trạng thái"
          allowClear
          style={{ width: 220 }}
          value={statusFilter}
          onChange={setStatusFilter}
          options={[
            { value: 'DRAFT', label: 'Nháp' },
            { value: 'DISPATCHED', label: 'Đang chuyển (Đã xuất)' },
            { value: 'RECEIVED', label: 'Đã nhận' },
            { value: 'CANCELLED', label: 'Đã hủy' },
          ]}
        />
      </Card>

      <Card>
        <Table
          rowKey="id"
          columns={columns}
          dataSource={transfers}
          loading={loading}
          pagination={{
            current: page,
            pageSize: 10,
            total,
            onChange: setPage,
            showTotal: (t) => `Tổng cộng ${t} phiếu chuyển`,
          }}
        />
      </Card>

      {/* Modal Chi tiết */}
      <Modal
        title={`Chi tiết phiếu chuyển kho: ${selectedTransfer?.code}`}
        open={detailOpen}
        onCancel={() => setDetailOpen(false)}
        width={750}
        footer={[
          selectedTransfer?.status === 'DRAFT' && (
            <Button key="dispatch" type="primary" icon={<SendOutlined />} onClick={() => handleDispatch(selectedTransfer.id)}>
              Xuất chuyển kho
            </Button>
          ),
          selectedTransfer?.status === 'DISPATCHED' && (
            <Button key="receive" type="primary" style={{ background: '#52c41a' }} icon={<CheckOutlined />} onClick={() => handleReceive(selectedTransfer.id)}>
              Xác nhận nhận hàng
            </Button>
          ),
          selectedTransfer?.status === 'DRAFT' && (
            <Button key="cancel" danger onClick={() => { setTransferToCancel(selectedTransfer); setCancelOpen(true) }}>
              Hủy phiếu
            </Button>
          ),
          <Button key="close" onClick={() => setDetailOpen(false)}>Đóng</Button>,
        ]}
      >
        {selectedTransfer && (
          <div>
            <Row gutter={[16, 8]}>
              <Col span={12}><p><strong>Kho nguồn (xuất):</strong> {branchName(selectedTransfer.fromBranchId)}</p></Col>
              <Col span={12}><p><strong>Kho đích (nhập):</strong> {branchName(selectedTransfer.toBranchId)}</p></Col>
              <Col span={12}><p><strong>Trạng thái:</strong> <Tag color={STATUS_COLOR[selectedTransfer.status]}>{STATUS_LABEL[selectedTransfer.status]}</Tag></p></Col>
              <Col span={12}><p><strong>Ghi chú:</strong> {selectedTransfer.note || '—'}</p></Col>
              <Col span={12}><p><strong>Thời gian tạo:</strong> {new Date(selectedTransfer.createdAt).toLocaleString('vi-VN')}</p></Col>
              {selectedTransfer.dispatchedAt && (
                <Col span={12}><p><strong>Thời gian xuất:</strong> {new Date(selectedTransfer.dispatchedAt).toLocaleString('vi-VN')}</p></Col>
              )}
              {selectedTransfer.receivedAt && (
                <Col span={12}><p><strong>Thời gian nhận:</strong> {new Date(selectedTransfer.receivedAt).toLocaleString('vi-VN')}</p></Col>
              )}
            </Row>

            <Typography.Title level={5} style={{ marginTop: 16 }}>Danh sách hàng chuyển</Typography.Title>
            <Table
              rowKey="skuId"
              pagination={false}
              dataSource={selectedTransfer.items}
              columns={[
                { title: 'STT', key: 'idx', render: (_, __, idx) => idx + 1, width: 60 },
                { title: 'Mã SKU', dataIndex: 'skuCode', key: 'skuCode' },
                { title: 'Số lượng chuyển', dataIndex: 'quantity', key: 'quantity', align: 'center' },
              ]}
            />
          </div>
        )}
      </Modal>

      {/* Modal Hủy */}
      <Modal
        title={`Hủy phiếu chuyển kho: ${transferToCancel?.code}`}
        open={cancelOpen}
        onOk={handleCancel}
        onCancel={() => { setCancelOpen(false); setTransferToCancel(null); setCancelReason('') }}
        okText="Xác nhận hủy"
        okButtonProps={{ danger: true }}
      >
        <p>Vui lòng nhập lý do hủy:</p>
        <Input.TextArea
          rows={3}
          value={cancelReason}
          onChange={(e) => setCancelReason(e.target.value)}
          placeholder="Lý do hủy phiếu..."
        />
      </Modal>

      {/* Modal Tạo phiếu chuyển */}
      <Modal
        title="Tạo phiếu chuyển kho liên chi nhánh"
        open={createOpen}
        onCancel={() => { setCreateOpen(false); createForm.resetFields() }}
        onOk={() => createForm.submit()}
        width={700}
      >
        <Form form={createForm} layout="vertical" onFinish={handleCreate}>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="fromBranchId" label="Chi nhánh xuất (Nguồn)" rules={[{ required: true, message: 'Chọn chi nhánh nguồn' }]}>
                <Select placeholder="Chọn chi nhánh nguồn" options={branches.map((b) => ({ value: b.id, label: b.name }))} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="toBranchId" label="Chi nhánh nhận (Đích)" rules={[{ required: true, message: 'Chọn chi nhánh đích' }]}>
                <Select placeholder="Chọn chi nhánh đích" options={branches.map((b) => ({ value: b.id, label: b.name }))} />
              </Form.Item>
            </Col>
          </Row>

          <Form.Item name="note" label="Ghi chú">
            <Input placeholder="Ví dụ: Chuyển hàng bổ sung cho chi nhánh..." />
          </Form.Item>

          <Typography.Text strong>Mặt hàng cần chuyển</Typography.Text>
          <Form.List name="items" initialValue={[{ skuId: undefined, quantity: 1 }]}>
            {(fields, { add, remove }) => (
              <div style={{ marginTop: 8 }}>
                {fields.map(({ key, name, ...restField }) => (
                  <Space key={key} style={{ display: 'flex', marginBottom: 8 }} align="baseline">
                    <Form.Item
                      {...restField}
                      name={[name, 'skuId']}
                      rules={[{ required: true, message: 'Chọn SKU' }]}
                      style={{ width: 420 }}
                    >
                      <Select
                        showSearch
                        placeholder="Tìm SKU theo mã hoặc tên"
                        filterOption={false}
                        onSearch={handleSearchSku}
                        options={availableSkus.map((s) => ({
                          value: s.id,
                          label: `${s.code} - ${s.productName} (Còn khả dụng: ${s.available})`,
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
                  Thêm dòng SKU
                </Button>
              </div>
            )}
          </Form.List>
        </Form>
      </Modal>
    </div>
  )
}
