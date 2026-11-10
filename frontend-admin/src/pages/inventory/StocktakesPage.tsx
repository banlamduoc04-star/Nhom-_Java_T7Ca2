import { useCallback, useEffect, useState } from 'react'
import {
  Button, Card, Col, Form, Input, InputNumber, Modal, Row, Select, Space, Table, Tag, Typography, message,
} from 'antd'
import { CheckOutlined, EditOutlined, EyeOutlined, PlusOutlined, SyncOutlined } from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import { branchApi, stocktakeApi } from '../../api/services'
import type { Branch, StocktakeStatus, StocktakeView } from '../../api/types'
import { errorText } from '../../api/client'

const STATUS_COLOR: Record<StocktakeStatus, string> = {
  OPEN: 'warning',
  APPROVED: 'success',
  CANCELLED: 'error',
}

const STATUS_LABEL: Record<StocktakeStatus, string> = {
  OPEN: 'Đang kiểm (Mở)',
  APPROVED: 'Đã duyệt (Đã cân chỉnh tồn)',
  CANCELLED: 'Đã hủy',
}

export default function StocktakesPage() {
  const [stocktakes, setStocktakes] = useState<StocktakeView[]>([])
  const [total, setTotal] = useState(0)
  const [loading, setLoading] = useState(false)
  const [page, setPage] = useState(1)
  const [statusFilter, setStatusFilter] = useState<StocktakeStatus>()
  const [branchFilter, setBranchFilter] = useState<string>()
  const [branches, setBranches] = useState<Branch[]>([])

  // Detail Modal
  const [selectedStocktake, setSelectedStocktake] = useState<StocktakeView | null>(null)
  const [detailOpen, setDetailOpen] = useState(false)

  // Record Counts Modal
  const [countModalOpen, setCountModalOpen] = useState(false)
  const [countInputs, setCountInputs] = useState<Record<string, number>>({})

  // Create Modal
  const [createOpen, setCreateOpen] = useState(false)
  const [createForm] = Form.useForm()

  const loadBranches = useCallback(async () => {
    try {
      const bs = await branchApi.list()
      setBranches(bs)
    } catch {
      // ignore
    }
  }, [])

  const loadStocktakes = useCallback(async () => {
    setLoading(true)
    try {
      const data = await stocktakeApi.list({
        status: statusFilter,
        branchId: branchFilter,
        page: page - 1,
        size: 10,
      })
      setStocktakes(data.content)
      setTotal(data.totalElements)
    } catch (err) {
      message.error(errorText(err))
    } finally {
      setLoading(false)
    }
  }, [statusFilter, branchFilter, page])

  useEffect(() => {
    loadBranches()
  }, [loadBranches])

  useEffect(() => {
    loadStocktakes()
  }, [loadStocktakes])

  const branchName = (id: string) => branches.find((b) => b.id === id)?.name || id

  const handleCreate = async (values: { branchId: string; note?: string }) => {
    try {
      await stocktakeApi.create({
        branchId: values.branchId,
        note: values.note,
      })
      message.success('Đã tạo đợt kiểm kê thành công (chụp số lượng tồn hệ thống)')
      setCreateOpen(false)
      createForm.resetFields()
      loadStocktakes()
    } catch (err) {
      message.error(errorText(err))
    }
  }

  const openCountModal = (st: StocktakeView) => {
    setSelectedStocktake(st)
    const initCounts: Record<string, number> = {}
    st.items.forEach((item) => {
      initCounts[item.skuId] = item.countedQty ?? item.systemQty
    })
    setCountInputs(initCounts)
    setCountModalOpen(true)
  }

  const handleSaveCounts = async () => {
    if (!selectedStocktake) return
    try {
      const counts = Object.entries(countInputs).map(([skuId, countedQty]) => ({
        skuId,
        countedQty,
      }))
      const updated = await stocktakeApi.recordCounts(selectedStocktake.id, counts)
      message.success('Đã ghi nhận số lượng thực tế kiểm kê')
      setSelectedStocktake(updated)
      setCountModalOpen(false)
      loadStocktakes()
    } catch (err) {
      message.error(errorText(err))
    }
  }

  const handleApprove = async (id: string) => {
    try {
      const updated = await stocktakeApi.approve(id)
      message.success('Đã phê duyệt đợt kiểm kê (sổ cái tồn kho đã được ghi bút toán điều chỉnh ADJUST)')
      setSelectedStocktake(updated)
      loadStocktakes()
    } catch (err) {
      message.error(errorText(err))
    }
  }

  const columns: ColumnsType<StocktakeView> = [
    {
      title: 'Mã đợt kiểm kê',
      dataIndex: 'code',
      key: 'code',
      render: (code, r) => (
        <a onClick={() => { setSelectedStocktake(r); setDetailOpen(true) }}>
          <strong>{code}</strong>
        </a>
      ),
    },
    {
      title: 'Chi nhánh kiểm kê',
      dataIndex: 'branchId',
      key: 'branchId',
      render: (id) => branchName(id),
    },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      key: 'status',
      render: (st: StocktakeStatus) => <Tag color={STATUS_COLOR[st]}>{STATUS_LABEL[st]}</Tag>,
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
          <Button size="small" icon={<EyeOutlined />} onClick={() => { setSelectedStocktake(r); setDetailOpen(true) }} />
          {r.status === 'OPEN' && (
            <>
              <Button size="small" icon={<EditOutlined />} onClick={() => openCountModal(r)}>
                Ghi số đếm
              </Button>
              <Button size="small" type="primary" icon={<CheckOutlined />} onClick={() => handleApprove(r.id)}>
                Duyệt cân chỉnh
              </Button>
            </>
          )}
        </Space>
      ),
    },
  ]

  return (
    <div>
      <Row justify="space-between" align="middle" style={{ marginBottom: 16 }}>
        <Col>
          <Typography.Title level={3} style={{ margin: 0 }}>Kiểm kê kho & Cân chỉnh tồn</Typography.Title>
        </Col>
        <Col>
          <Space>
            <Button icon={<SyncOutlined />} onClick={loadStocktakes}>Làm mới</Button>
            <Button type="primary" icon={<PlusOutlined />} onClick={() => setCreateOpen(true)}>Tạo đợt kiểm kê</Button>
          </Space>
        </Col>
      </Row>

      <Card style={{ marginBottom: 16 }}>
        <Space wrap>
          <Select
            placeholder="Lọc theo trạng thái"
            allowClear
            style={{ width: 220 }}
            value={statusFilter}
            onChange={setStatusFilter}
            options={[
              { value: 'OPEN', label: 'Đang kiểm (Mở)' },
              { value: 'APPROVED', label: 'Đã duyệt cân chỉnh' },
              { value: 'CANCELLED', label: 'Đã hủy' },
            ]}
          />
          <Select
            placeholder="Lọc theo chi nhánh"
            allowClear
            style={{ width: 220 }}
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
          dataSource={stocktakes}
          loading={loading}
          pagination={{
            current: page,
            pageSize: 10,
            total,
            onChange: setPage,
            showTotal: (t) => `Tổng cộng ${t} đợt kiểm kê`,
          }}
        />
      </Card>

      {/* Modal Chi tiết */}
      <Modal
        title={`Chi tiết kiểm kê: ${selectedStocktake?.code}`}
        open={detailOpen}
        onCancel={() => setDetailOpen(false)}
        width={800}
        footer={[
          selectedStocktake?.status === 'OPEN' && (
            <Button key="count" icon={<EditOutlined />} onClick={() => { setDetailOpen(false); openCountModal(selectedStocktake) }}>
              Ghi nhận số đếm thực tế
            </Button>
          ),
          selectedStocktake?.status === 'OPEN' && (
            <Button key="approve" type="primary" icon={<CheckOutlined />} onClick={() => handleApprove(selectedStocktake.id)}>
              Duyệt & Cân chỉnh tồn kho
            </Button>
          ),
          <Button key="close" onClick={() => setDetailOpen(false)}>Đóng</Button>,
        ]}
      >
        {selectedStocktake && (
          <div>
            <Row gutter={[16, 8]}>
              <Col span={12}><p><strong>Chi nhánh:</strong> {branchName(selectedStocktake.branchId)}</p></Col>
              <Col span={12}><p><strong>Trạng thái:</strong> <Tag color={STATUS_COLOR[selectedStocktake.status]}>{STATUS_LABEL[selectedStocktake.status]}</Tag></p></Col>
              <Col span={12}><p><strong>Ghi chú:</strong> {selectedStocktake.note || '—'}</p></Col>
              <Col span={12}><p><strong>Thời gian tạo:</strong> {new Date(selectedStocktake.createdAt).toLocaleString('vi-VN')}</p></Col>
              {selectedStocktake.approvedAt && (
                <Col span={12}><p><strong>Thời gian duyệt:</strong> {new Date(selectedStocktake.approvedAt).toLocaleString('vi-VN')}</p></Col>
              )}
            </Row>

            <Typography.Title level={5} style={{ marginTop: 16 }}>Danh sách mặt hàng kiểm kê</Typography.Title>
            <Table
              rowKey="skuId"
              pagination={false}
              dataSource={selectedStocktake.items}
              columns={[
                { title: 'STT', key: 'idx', render: (_, __, idx) => idx + 1, width: 60 },
                { title: 'Mã SKU', dataIndex: 'skuCode', key: 'skuCode' },
                { title: 'Tồn hệ thống', dataIndex: 'systemQty', key: 'systemQty', align: 'center' },
                {
                  title: 'Thực tế đếm',
                  dataIndex: 'countedQty',
                  key: 'countedQty',
                  align: 'center',
                  render: (v) => (v != null ? <strong>{v}</strong> : <span style={{ color: '#888' }}>Chưa đếm</span>),
                },
                {
                  title: 'Chênh lệch',
                  dataIndex: 'difference',
                  key: 'difference',
                  align: 'center',
                  render: (d) => {
                    if (d == null) return '—'
                    if (d > 0) return <strong style={{ color: '#52c41a' }}>+{d} (thừa)</strong>
                    if (d < 0) return <strong style={{ color: '#cf1322' }}>{d} (thiếu)</strong>
                    return <span style={{ color: '#888' }}>0 (khớp)</span>
                  },
                },
              ]}
            />
          </div>
        )}
      </Modal>

      {/* Modal Ghi số đếm thực tế */}
      <Modal
        title={`Ghi số đếm thực tế: ${selectedStocktake?.code}`}
        open={countModalOpen}
        onCancel={() => setCountModalOpen(false)}
        onOk={handleSaveCounts}
        width={750}
        okText="Lưu số đếm"
      >
        <p>Nhập số lượng thực tế đếm được cho từng SKU tại chi nhánh kiểm kê:</p>
        <Table
          rowKey="skuId"
          pagination={false}
          dataSource={selectedStocktake?.items || []}
          columns={[
            { title: 'Mã SKU', dataIndex: 'skuCode', key: 'skuCode' },
            { title: 'Tồn hệ thống', dataIndex: 'systemQty', key: 'systemQty', align: 'center' },
            {
              title: 'Thực tế đếm',
              key: 'countInput',
              render: (_, r) => (
                <InputNumber
                  min={0}
                  value={countInputs[r.skuId] ?? r.systemQty}
                  onChange={(val) => setCountInputs((prev) => ({ ...prev, [r.skuId]: val ?? 0 }))}
                />
              ),
            },
            {
              title: 'Chênh lệch dự kiến',
              key: 'diffCalc',
              render: (_, r) => {
                const counted = countInputs[r.skuId] ?? r.systemQty
                const diff = counted - r.systemQty
                if (diff > 0) return <span style={{ color: '#52c41a' }}>+{diff} (thừa)</span>
                if (diff < 0) return <span style={{ color: '#cf1322' }}>{diff} (thiếu)</span>
                return <span style={{ color: '#888' }}>0</span>
              },
            },
          ]}
        />
      </Modal>

      {/* Modal Tạo đợt kiểm kê */}
      <Modal
        title="Tạo đợt kiểm kê kho mới"
        open={createOpen}
        onCancel={() => { setCreateOpen(false); createForm.resetFields() }}
        onOk={() => createForm.submit()}
      >
        <Form form={createForm} layout="vertical" onFinish={handleCreate}>
          <Form.Item name="branchId" label="Chi nhánh kiểm kê" rules={[{ required: true, message: 'Chọn chi nhánh' }]}>
            <Select placeholder="Chọn chi nhánh" options={branches.map((b) => ({ value: b.id, label: b.name }))} />
          </Form.Item>
          <Form.Item name="note" label="Ghi chú đợt kiểm">
            <Input placeholder="Ví dụ: Kiểm kê định kỳ tháng 11..." />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
