import { useEffect, useState } from 'react'
import { Button, Card, Col, Row, Select, Statistic, Table, Tabs, Tag, Typography, message } from 'antd'
import { SyncOutlined } from '@ant-design/icons'
import { branchApi, stockReportApi } from '../../api/services'
import { errorText } from '../../api/client'
import { useAuth } from '../../auth/AuthContext'
import type { Branch, DailyReport, SlowMovingItem, TopSellingItem, ValuationItem, ValuationSummary } from '../../api/types'

const money = (v: number) => `${v.toLocaleString('vi-VN')} ₫`

export default function StockReportsPage() {
  // Chot so ngay (bo sung FR-SIM-03) chi Owner; 3 bao cao ton FR-REP-02 cho Owner + Staff
  const isOwner = useAuth().hasRole('OWNER')
  const [branches, setBranches] = useState<Branch[]>([])
  const [selectedBranch, setSelectedBranch] = useState<string | undefined>()

  // Valuation
  const [valLoading, setValLoading] = useState(false)
  const [valuation, setValuation] = useState<ValuationSummary | null>(null)

  // Top Selling
  const [topLoading, setTopLoading] = useState(false)
  const [topItems, setTopItems] = useState<TopSellingItem[]>([])
  const [topDays, setTopDays] = useState(30)

  // Slow Moving
  const [slowLoading, setSlowLoading] = useState(false)
  const [slowItems, setSlowItems] = useState<SlowMovingItem[]>([])
  const [slowDays, setSlowDays] = useState(30)

  // Daily Reports
  const [dailyLoading, setDailyLoading] = useState(false)
  const [dailyReports, setDailyReports] = useState<DailyReport[]>([])

  useEffect(() => {
    branchApi.list().then(setBranches).catch(() => undefined)
  }, [])

  const loadValuation = async () => {
    setValLoading(true)
    try {
      const data = await stockReportApi.valuation(selectedBranch)
      setValuation(data)
    } catch {
      message.error('Không thể tải báo cáo định giá tồn kho')
    } finally {
      setValLoading(false)
    }
  }

  const loadTopSelling = async () => {
    setTopLoading(true)
    try {
      const data = await stockReportApi.topSelling(topDays, 20)
      setTopItems(data)
    } catch {
      message.error('Không thể tải top sản phẩm bán chạy')
    } finally {
      setTopLoading(false)
    }
  }

  const loadSlowMoving = async () => {
    setSlowLoading(true)
    try {
      const data = await stockReportApi.slowMoving(slowDays)
      setSlowItems(data)
    } catch {
      message.error('Không thể tải danh sách hàng tồn chậm')
    } finally {
      setSlowLoading(false)
    }
  }

  const loadDailyReports = async () => {
    setDailyLoading(true)
    try {
      const data = await stockReportApi.dailyReports()
      setDailyReports(data)
    } catch {
      message.error('Không thể tải lịch sử chốt sổ ngày')
    } finally {
      setDailyLoading(false)
    }
  }

  useEffect(() => {
    loadValuation()
  }, [selectedBranch])

  useEffect(() => {
    loadTopSelling()
  }, [topDays])

  useEffect(() => {
    loadSlowMoving()
  }, [slowDays])

  useEffect(() => {
    if (isOwner) loadDailyReports()
  }, [isOwner])

  const triggerDaily = async () => {
    try {
      await stockReportApi.triggerDaily()
      message.success('Đã kích hoạt chốt sổ thành công')
      loadDailyReports()
    } catch (err) {
      message.error(errorText(err))
    }
  }

  const tabItems = [
    {
      key: 'valuation',
      label: 'Định giá tồn kho',
      children: (
        <div>
          <Card style={{ marginBottom: 16 }}>
            <Row gutter={[16, 16]} align="middle">
              <Col xs={24} sm={8}>
                <Select
                  allowClear
                  placeholder="Xem theo chi nhánh (Tất cả)"
                  style={{ width: '100%' }}
                  value={selectedBranch}
                  onChange={setSelectedBranch}
                  options={branches.map((b) => ({ value: b.id, label: b.name }))}
                />
              </Col>
              <Col xs={24} sm={8}>
                <Statistic title="Tổng giá trị tồn kho" value={money(valuation?.totalInventoryValue || 0)} />
              </Col>
              <Col xs={24} sm={8}>
                <Statistic title="Tổng số lượng tồn" value={valuation?.totalOnHand || 0} />
              </Col>
            </Row>
          </Card>

          <Table<ValuationItem>
            loading={valLoading}
            dataSource={valuation?.items || []}
            rowKey={(r) => `${r.branchId}-${r.skuId}`}
            pagination={{ pageSize: 15 }}
            columns={[
              { title: 'Chi nhánh', dataIndex: 'branchName', key: 'branchName' },
              { title: 'Sản phẩm', dataIndex: 'productName', key: 'productName' },
              { title: 'Mã SKU', dataIndex: 'skuCode', key: 'skuCode', render: (c) => <Tag>{c}</Tag> },
              { title: 'Tồn thực tế', dataIndex: 'onHand', key: 'onHand', align: 'right' },
              { title: 'Giá vốn BQGQ', dataIndex: 'avgCost', key: 'avgCost', render: money, align: 'right' },
              { title: 'Tổng giá trị', dataIndex: 'totalValue', key: 'totalValue', render: money, align: 'right' },
            ]}
          />
        </div>
      ),
    },
    {
      key: 'top-selling',
      label: 'Top bán chạy',
      children: (
        <div>
          <div style={{ marginBottom: 16, display: 'flex', gap: 12, alignItems: 'center' }}>
            <span>Khoảng thời gian:</span>
            <Select
              value={topDays}
              onChange={setTopDays}
              options={[
                { value: 7, label: '7 ngày qua' },
                { value: 30, label: '30 ngày qua' },
                { value: 90, label: '90 ngày qua' },
              ]}
              style={{ width: 140 }}
            />
            <Button icon={<SyncOutlined />} onClick={loadTopSelling} loading={topLoading}>Làm mới</Button>
          </div>

          <Table<TopSellingItem>
            loading={topLoading}
            dataSource={topItems}
            rowKey="skuId"
            columns={[
              { title: 'Hạng', key: 'rank', render: (_, __, i) => `#${i + 1}`, width: 70 },
              { title: 'Sản phẩm', dataIndex: 'productName', key: 'productName' },
              { title: 'Mã SKU', dataIndex: 'skuCode', key: 'skuCode', render: (c) => <Tag color="blue">{c}</Tag> },
              { title: 'Số lượng đã bán', dataIndex: 'quantitySold', key: 'quantitySold', align: 'right' },
              { title: 'Tổng doanh thu', dataIndex: 'totalRevenue', key: 'totalRevenue', render: money, align: 'right' },
            ]}
          />
        </div>
      ),
    },
    {
      key: 'slow-moving',
      label: 'Hàng tồn chậm',
      children: (
        <div>
          <div style={{ marginBottom: 16, display: 'flex', gap: 12, alignItems: 'center' }}>
            <span>Số ngày không bán được:</span>
            <Select
              value={slowDays}
              onChange={setSlowDays}
              options={[
                { value: 15, label: 'Trên 15 ngày' },
                { value: 30, label: 'Trên 30 ngày' },
                { value: 60, label: 'Trên 60 ngày' },
              ]}
              style={{ width: 160 }}
            />
            <Button icon={<SyncOutlined />} onClick={loadSlowMoving} loading={slowLoading}>Làm mới</Button>
          </div>

          <Table<SlowMovingItem>
            loading={slowLoading}
            dataSource={slowItems}
            rowKey={(r) => `${r.branchId}-${r.skuId}`}
            columns={[
              { title: 'Chi nhánh', dataIndex: 'branchName', key: 'branchName' },
              { title: 'Sản phẩm', dataIndex: 'productName', key: 'productName' },
              { title: 'Mã SKU', dataIndex: 'skuCode', key: 'skuCode', render: (c) => <Tag color="orange">{c}</Tag> },
              { title: 'Tồn đọng', dataIndex: 'onHand', key: 'onHand', align: 'right' },
              { title: 'Giá vốn', dataIndex: 'avgCost', key: 'avgCost', render: money, align: 'right' },
              { title: 'Giá trị ứ đọng', dataIndex: 'totalValue', key: 'totalValue', render: money, align: 'right' },
            ]}
          />
        </div>
      ),
    },

    ...(isOwner ? [
    {
      key: 'daily',
      label: 'Chốt sổ ngày (Daily Report)',
      children: (
        <div>
          <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <Typography.Text type="secondary">
              Tác vụ nền chốt doanh thu, giá vốn, lợi nhuận gộp (đơn Confirmed/Completed) theo chi nhánh × kênh mỗi đêm lúc 02:00 UTC. Bạn cũng có thể chủ động chốt ngay.
            </Typography.Text>
            <Button type="primary" onClick={triggerDaily}>Chốt sổ hôm qua ngay</Button>
          </div>

          <Table<DailyReport>
            loading={dailyLoading}
            dataSource={dailyReports}
            rowKey="id"
            columns={[
              { title: 'Ngày chốt', dataIndex: 'reportDate', key: 'reportDate' },
              { title: 'Chi nhánh', dataIndex: 'branchName', key: 'branchName' },
              { title: 'Kênh', dataIndex: 'channel', key: 'channel', render: (c) => <Tag>{c}</Tag> },
              { title: 'Số đơn', dataIndex: 'orderCount', key: 'orderCount', align: 'right' },
              { title: 'Doanh thu', dataIndex: 'revenue', key: 'revenue', render: money, align: 'right' },
              { title: 'Giá vốn (COGS)', dataIndex: 'cogs', key: 'cogs', render: money, align: 'right' },
              {
                title: 'Lợi nhuận gộp',
                dataIndex: 'grossProfit',
                key: 'grossProfit',
                render: (v) => <strong style={{ color: v >= 0 ? '#3f8600' : '#cf1322' }}>{money(v)}</strong>,
                align: 'right',
              },
            ]}
          />
        </div>
      ),
    },
    ] : []),
  ]

  return (
    <div>
      <Typography.Title level={3}>Báo cáo tồn kho & hàng bán</Typography.Title>
      <Tabs defaultActiveKey="valuation" items={tabItems} />
    </div>
  )
}
