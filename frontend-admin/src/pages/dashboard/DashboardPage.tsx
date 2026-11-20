import { useEffect, useState } from 'react'
import { Card, Col, Row, Table, Tag, Button, Space, message, Badge, Popconfirm, Empty, Progress } from 'antd'
import { ShoppingCartOutlined, DollarOutlined, AlertOutlined, TrophyOutlined, SyncOutlined, QuestionCircleOutlined, RiseOutlined } from '@ant-design/icons'
import type { ReactNode } from 'react'
import { useNavigate } from 'react-router-dom'
import { alertApi, orderApi, reportApi, stockReportApi } from '../../api/services'
import type { StockAlert, TopSellingItem } from '../../api/types'
import { handleApiError } from '../../api/errorHandler'
import { palette } from '../../theme'
const money = (v: number) => `${v.toLocaleString('vi-VN')} ₫`

function Kpi({ icon, color, bg, label, value, sub }: { icon: ReactNode; color: string; bg: string; label: string; value: ReactNode; sub?: ReactNode }) {
  return (
    <Card className="kpi-card" style={{ height: '100%' }}>
      <span className="kpi-icon" style={{ color, background: bg }}>{icon}</span>
      <div style={{ minWidth: 0 }}>
        <div className="kpi-label">{label}</div>
        <div className="kpi-value">{value}</div>
        {sub && <div className="kpi-sub">{sub}</div>}
      </div>
    </Card>
  )
}

export default function DashboardPage() {
  const navigate = useNavigate()
  const [loading, setLoading] = useState(false)
  const [alerts, setAlerts] = useState<StockAlert[]>([])
  const [topSelling, setTopSelling] = useState<TopSellingItem[]>([])
  const [orderCounts, setOrderCounts] = useState({ total: 0, reserved: 0, confirmed: 0, completed: 0, cancelled: 0 })
  const [revenue, setRevenue] = useState({ totalRevenue: 0, grossProfit: 0 })

  const loadData = async () => {
    setLoading(true)
    try {
      const [alertList, topList, ordersPage, profitData] = await Promise.all([
        alertApi.list({ status: 'OPEN' }).catch(() => []),
        stockReportApi.topSelling(30, 5).catch(() => []),
        orderApi.list({ size: 100 }).catch(() => ({ content: [] })),
        reportApi.profit({}).catch(() => null),
      ])

      setAlerts(alertList)
      setTopSelling(topList)
      setRevenue({
        totalRevenue: profitData?.summary?.totalRevenue || 0,
        grossProfit: profitData?.summary?.grossProfit || 0,
      })

      const counts = { total: 0, reserved: 0, confirmed: 0, completed: 0, cancelled: 0 }
      ordersPage.content.forEach((o) => {
        counts.total++
        if (o.status === 'RESERVED') counts.reserved++
        if (o.status === 'CONFIRMED') counts.confirmed++
        if (o.status === 'COMPLETED') counts.completed++
        if (o.status === 'CANCELLED') counts.cancelled++
      })
      setOrderCounts(counts)
    } catch (err) {
      handleApiError(err)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadData()
  }, [])

  const resolveAlert = async (id: string) => {
    try {
      await alertApi.markRead(id)
      message.success('Đã đóng cảnh báo')
      loadData()
    } catch (err) {
      handleApiError(err)
    }
  }

  const margin = revenue.totalRevenue > 0 ? (revenue.grossProfit / revenue.totalRevenue) * 100 : 0
  const maxSold = Math.max(1, ...topSelling.map((t) => t.quantitySold))
  const statuses = [
    { label: 'Đang giữ hàng', n: orderCounts.reserved, color: palette.warningText },
    { label: 'Đã duyệt', n: orderCounts.confirmed, color: palette.infoText },
    { label: 'Hoàn tất', n: orderCounts.completed, color: palette.successText },
    { label: 'Đã hủy', n: orderCounts.cancelled, color: palette.muted },
  ]

  return (
    <div>
      <div className="page-head">
        <div>
          <h2>Tổng quan Dashboard</h2>
          <p>Doanh thu, đơn hàng và tình trạng tồn kho của cửa hàng</p>
        </div>
        <Button icon={<SyncOutlined />} onClick={loadData} loading={loading}>Làm mới</Button>
      </div>

      <Row gutter={[16, 16]}>
        <Col xs={24} sm={12} xl={6}>
          <Kpi icon={<DollarOutlined />} color="#4f46e5" bg="#eef2ff" label="Tổng doanh thu"
            value={money(revenue.totalRevenue)} sub="Đơn đã duyệt và hoàn tất" />
        </Col>
        <Col xs={24} sm={12} xl={6}>
          <Kpi icon={<RiseOutlined />} color="#16a34a" bg="#f0fdf4" label="Lợi nhuận gộp"
            value={money(revenue.grossProfit)} sub={`Biên lợi nhuận ${margin.toFixed(1)}%`} />
        </Col>
        <Col xs={24} sm={12} xl={6}>
          <Kpi icon={<ShoppingCartOutlined />} color="#d97706" bg="#fffbeb" label="Đơn đang xử lý (Giữ / Đã duyệt)"
            value={`${orderCounts.reserved} / ${orderCounts.confirmed}`} sub={`Trong ${orderCounts.total} đơn gần nhất`} />
        </Col>
        <Col xs={24} sm={12} xl={6}>
          <Kpi icon={<AlertOutlined />} color={alerts.length > 0 ? palette.dangerText : palette.successText} bg={alerts.length > 0 ? '#fef2f2' : '#f0fdf4'}
            label="Cảnh báo tồn kho thấp" value={alerts.length}
            sub={alerts.length > 0 ? 'Cần nhập thêm hàng' : 'Tồn kho ổn định'} />
        </Col>
      </Row>

      <Card title="Trạng thái đơn hàng" style={{ marginTop: 16 }}
        extra={<Button type="link" onClick={() => navigate('/orders')}>Xem đơn hàng</Button>}>
        <div className="status-strip">
          {statuses.map((st) => (
            <div className="status-cell" key={st.label}>
              <div className="n" style={{ color: st.color }}>{st.n}</div>
              <div className="l">{st.label}</div>
              <Progress
                percent={orderCounts.total ? Math.round((st.n / orderCounts.total) * 100) : 0}
                showInfo={false} size="small" strokeColor={st.color} style={{ margin: '6px 0 0' }}
              />
            </div>
          ))}
        </div>
      </Card>

      <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
        <Col xs={24} lg={14}>
          <Card
            title={
              <Space>
                <AlertOutlined style={{ color: palette.dangerText }} />
                <span>Cảnh báo tồn kho thấp</span>
                {alerts.length > 0 && <Badge count={alerts.length} />}
              </Space>
            }
            extra={<Button type="link" onClick={() => navigate('/stock')}>Xem tồn kho</Button>}
            style={{ height: '100%' }}
          >
            <Table
              dataSource={alerts}
              rowKey="id"
              size="small"
              loading={loading}
              pagination={{ pageSize: 5, hideOnSinglePage: true }}
              scroll={{ x: 520 }}
              locale={{
                emptyText: (
                  <Empty
                    image={Empty.PRESENTED_IMAGE_SIMPLE}
                    description="Hiện tại không có cảnh báo tồn kho nào"
                  />
                ),
              }}
              columns={[
                { title: 'Sản phẩm', dataIndex: 'productName', key: 'productName' },
                { title: 'Mã SKU', dataIndex: 'skuCode', key: 'skuCode', render: (c) => <Tag>{c}</Tag> },
                { title: 'Chi nhánh', dataIndex: 'branchName', key: 'branchName' },
                {
                  title: 'Khả dụng / Ngưỡng',
                  key: 'qty',
                  render: (_, r) => (
                    <span style={{ color: palette.dangerText, fontWeight: 600 }}>
                      {r.available} / {r.threshold}
                    </span>
                  ),
                },
                {
                  title: 'Thao tác',
                  key: 'action',
                  render: (_, r) => (
                    <Popconfirm
                      title="Xác nhận đóng cảnh báo?"
                      description="Hành động này sẽ xác nhận xử lý cảnh báo tồn kho."
                      icon={<QuestionCircleOutlined style={{ color: 'red' }} />}
                      onConfirm={() => resolveAlert(r.id)}
                      okText="Đóng"
                      cancelText="Hủy"
                      okButtonProps={{ danger: true }}
                    >
                      <Button size="small" danger type="text">Đóng</Button>
                    </Popconfirm>
                  ),
                },
              ]}
            />
          </Card>
        </Col>

        <Col xs={24} lg={10}>
          <Card
            title={
              <Space>
                <TrophyOutlined style={{ color: palette.warningText }} />
                <span>Top 5 bán chạy (30 ngày)</span>
              </Space>
            }
            extra={<Button type="link" onClick={() => navigate('/stock-reports')}>Xem chi tiết</Button>}
            style={{ height: '100%' }}
            loading={loading && topSelling.length === 0}
          >
            {topSelling.length === 0 ? (
              <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="Chưa có dữ liệu bán chạy trong 30 ngày qua" />
            ) : (
              <div style={{ display: 'grid', gap: 14 }}>
                {topSelling.map((t, i) => (
                  <div key={t.skuId}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', gap: 8 }}>
                      <span style={{ minWidth: 0, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                        <b style={{ color: palette.muted, marginRight: 8 }}>#{i + 1}</b>
                        <b>{t.productName}</b> <span style={{ color: palette.muted, fontSize: 12 }}>{t.skuCode}</span>
                      </span>
                      <span style={{ whiteSpace: 'nowrap', fontWeight: 600 }}>{money(t.totalRevenue)}</span>
                    </div>
                    <Progress
                      percent={Math.round((t.quantitySold / maxSold) * 100)}
                      format={() => `${t.quantitySold} đã bán`}
                      strokeColor="#4f46e5"
                      size="small"
                    />
                  </div>
                ))}
              </div>
            )}
          </Card>
        </Col>
      </Row>
    </div>
  )
}
