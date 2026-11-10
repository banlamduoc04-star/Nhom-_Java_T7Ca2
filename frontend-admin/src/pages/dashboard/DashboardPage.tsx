import { useEffect, useState } from 'react'
import { Card, Col, Row, Statistic, Table, Tag, Typography, Button, Space, message, Badge, Popconfirm, Empty } from 'antd'
import { ShoppingCartOutlined, DollarOutlined, AlertOutlined, TrophyOutlined, SyncOutlined, QuestionCircleOutlined } from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import { alertApi, orderApi, reportApi, stockReportApi } from '../../api/services'
import type { StockAlert, TopSellingItem } from '../../api/types'
import { handleApiError } from '../../api/errorHandler'
const money = (v: number) => `${v.toLocaleString('vi-VN')} ₫`

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
      await alertApi.resolve(id)
      message.success('Đã đóng cảnh báo')
      loadData()
    } catch (err) {
      handleApiError(err)
    }
  }

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <Typography.Title level={3} style={{ margin: 0 }}>Tổng quan Dashboard</Typography.Title>
        <Button icon={<SyncOutlined />} onClick={loadData} loading={loading}>Làm mới</Button>
      </div>

      <Row gutter={[16, 16]}>
        <Col xs={24} sm={12} lg={6}>
          <Card bordered={false} style={{ background: '#f6ffed', border: '1px solid #b7eb8f' }}>
            <Statistic
              title="Tổng doanh thu"
              value={money(revenue.totalRevenue)}
              prefix={<DollarOutlined style={{ color: '#52c41a' }} />}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card bordered={false} style={{ background: '#e6f7ff', border: '1px solid #91d5ff' }}>
            <Statistic
              title="Lợi nhuận gộp"
              value={money(revenue.grossProfit)}
              prefix={<TrophyOutlined style={{ color: '#1890ff' }} />}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card bordered={false} style={{ background: '#fffbe6', border: '1px solid #ffe58f' }}>
            <Statistic
              title="Đơn đang xử lý (Giữ / Đã duyệt)"
              value={`${orderCounts.reserved} / ${orderCounts.confirmed}`}
              prefix={<ShoppingCartOutlined style={{ color: '#faad14' }} />}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card bordered={false} style={{ background: '#fff2e8', border: '1px solid #ffbb96' }}>
            <Statistic
              title="Cảnh báo tồn kho thấp"
              value={alerts.length}
              styles={{ content: { color: alerts.length > 0 ? '#cf1322' : '#3f8600' } }}
              prefix={<AlertOutlined style={{ color: '#f5222d' }} />}
            />
          </Card>
        </Col>
      </Row>

      <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
        <Col xs={24} lg={14}>
          <Card
            title={
              <Space>
                <AlertOutlined style={{ color: '#f5222d' }} />
                <span>Cảnh báo tồn kho thấp (dưới ngưỡng đặt lại)</span>
                {alerts.length > 0 && <Badge count={alerts.length} />}
              </Space>
            }
            extra={<Button type="link" onClick={() => navigate('/stock')}>Xem tồn kho</Button>}
          >
            <Table
              dataSource={alerts}
              rowKey="id"
              size="small"
              loading={loading}
              pagination={{ pageSize: 5 }}
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
                    <span style={{ color: '#cf1322', fontWeight: 600 }}>
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
                <TrophyOutlined style={{ color: '#faad14' }} />
                <span>Top 5 bán chạy (30 ngày gần nhất)</span>
              </Space>
            }
            extra={<Button type="link" onClick={() => navigate('/stock-reports')}>Xem chi tiết</Button>}
          >
            <Table
              dataSource={topSelling}
              rowKey="skuId"
              size="small"
              loading={loading}
              pagination={false}
              locale={{
                emptyText: (
                  <Empty
                    image={Empty.PRESENTED_IMAGE_SIMPLE}
                    description="Chưa có dữ liệu bán chạy trong 30 ngày qua"
                  />
                ),
              }}
              columns={[
                { title: 'Sản phẩm', dataIndex: 'productName', key: 'productName' },
                { title: 'Mã SKU', dataIndex: 'skuCode', key: 'skuCode' },
                { title: 'Đã bán', dataIndex: 'quantitySold', key: 'quantitySold', align: 'right' },
                { title: 'Doanh thu', dataIndex: 'totalRevenue', key: 'totalRevenue', render: money, align: 'right' },
              ]}
            />
          </Card>
        </Col>
      </Row>
    </div>
  )
}
