import { useCallback, useEffect, useState } from 'react'
import {
  Button, Card, Col, DatePicker, Row, Select, Space, Statistic, Table, Typography, message,
} from 'antd'
import { ArrowDownOutlined, ArrowUpOutlined, DownloadOutlined, SyncOutlined } from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import dayjs from 'dayjs'
import { branchApi, reportApi, skuApi } from '../../api/services'
import type { Branch, OrderChannel, ProfitBySku, ProfitReportView, SkuSearchItem } from '../../api/types'
import { errorText } from '../../api/client'

const money = (v: number | null | undefined) => (v != null ? `${Math.round(v).toLocaleString('vi-VN')} ₫` : '0 ₫')

export default function ProfitReportPage() {
  const [data, setData] = useState<ProfitReportView | null>(null)
  const [loading, setLoading] = useState(false)
  const [branches, setBranches] = useState<Branch[]>([])
  const [skus, setSkus] = useState<SkuSearchItem[]>([])

  // Filters
  const [dateRange, setDateRange] = useState<[dayjs.Dayjs | null, dayjs.Dayjs | null] | null>(null)
  const [branchFilter, setBranchFilter] = useState<string>()
  const [channelFilter, setChannelFilter] = useState<OrderChannel>()
  const [skuFilter, setSkuFilter] = useState<string>()

  const loadBranches = useCallback(async () => {
    try {
      const bs = await branchApi.list()
      setBranches(bs)
    } catch {
      // ignore
    }
  }, [])

  const loadReport = useCallback(async () => {
    setLoading(true)
    try {
      const from = dateRange && dateRange[0] ? dateRange[0].startOf('day').toISOString() : undefined
      const to = dateRange && dateRange[1] ? dateRange[1].endOf('day').toISOString() : undefined
      const res = await reportApi.profit({
        from,
        to,
        branchId: branchFilter,
        channel: channelFilter,
        skuId: skuFilter,
      })
      setData(res)
    } catch (err) {
      message.error(errorText(err))
    } finally {
      setLoading(false)
    }
  }, [dateRange, branchFilter, channelFilter, skuFilter])

  useEffect(() => {
    loadBranches()
  }, [loadBranches])

  useEffect(() => {
    loadReport()
  }, [loadReport])

  const handleSearchSku = async (keyword: string) => {
    if (!keyword) return
    try {
      const res = await skuApi.search({ keyword, size: 10 })
      setSkus(res.content)
    } catch {
      // ignore
    }
  }

  const handleExportCsv = async () => {
    try {
      const from = dateRange && dateRange[0] ? dateRange[0].startOf('day').toISOString() : undefined
      const to = dateRange && dateRange[1] ? dateRange[1].endOf('day').toISOString() : undefined
      const res = await reportApi.exportCsv({
        from,
        to,
        branchId: branchFilter,
        channel: channelFilter,
        skuId: skuFilter,
      })
      const blob = new Blob([res.data], { type: 'text/csv;charset=utf-8;' })
      const url = URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.setAttribute('download', `profit-report-${dayjs().format('YYYYMMDD-HHmmss')}.csv`)
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      message.success('Đã xuất file CSV thành công')
    } catch {
      message.error('Xuất CSV thất bại')
    }
  }

  const columns: ColumnsType<ProfitBySku> = [
    {
      title: 'Mã SKU',
      dataIndex: 'skuCode',
      key: 'skuCode',
      render: (code) => <strong>{code}</strong>,
    },
    {
      title: 'Số lượng đã bán',
      dataIndex: 'quantitySold',
      key: 'quantitySold',
      align: 'center',
    },
    {
      title: 'Doanh thu (Revenue)',
      dataIndex: 'revenue',
      key: 'revenue',
      align: 'right',
      render: (v) => money(v),
    },
    {
      title: 'Giá vốn hàng bán (COGS)',
      dataIndex: 'cogs',
      key: 'cogs',
      align: 'right',
      render: (v) => money(v),
    },
    {
      title: 'Lợi nhuận gộp',
      dataIndex: 'grossProfit',
      key: 'grossProfit',
      align: 'right',
      render: (v) => (
        <strong style={{ color: v >= 0 ? '#3f8600' : '#cf1322' }}>
          {money(v)}
        </strong>
      ),
    },
    {
      title: 'Tỷ suất biên (%)',
      dataIndex: 'grossMargin',
      key: 'grossMargin',
      align: 'right',
      render: (m) => (
        <span style={{ color: m >= 0 ? '#3f8600' : '#cf1322' }}>
          {(m * 100).toFixed(1)}%
        </span>
      ),
    },
  ]

  const summary = data?.summary

  return (
    <div>
      <Row justify="space-between" align="middle" style={{ marginBottom: 16 }}>
        <Col>
          <Typography.Title level={3} style={{ margin: 0 }}>Báo cáo Lợi nhuận gộp (Profit & Loss)</Typography.Title>
        </Col>
        <Col>
          <Space>
            <Button icon={<DownloadOutlined />} onClick={handleExportCsv}>Xuất file CSV</Button>
            <Button icon={<SyncOutlined />} onClick={loadReport}>Làm mới</Button>
          </Space>
        </Col>
      </Row>

      {/* Summary KPI Cards */}
      <Row gutter={16} style={{ marginBottom: 16 }}>
        <Col span={6}>
          <Card>
            <Statistic
              title="Tổng Doanh thu"
              value={summary?.totalRevenue || 0}
              precision={0}
              suffix="₫"
              valueStyle={{ color: '#1890ff' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="Tổng Giá vốn (COGS)"
              value={summary?.totalCogs || 0}
              precision={0}
              suffix="₫"
              valueStyle={{ color: '#fa8c16' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="Lợi nhuận gộp"
              value={summary?.grossProfit || 0}
              precision={0}
              suffix="₫"
              valueStyle={{ color: (summary?.grossProfit || 0) >= 0 ? '#3f8600' : '#cf1322' }}
              prefix={(summary?.grossProfit || 0) >= 0 ? <ArrowUpOutlined /> : <ArrowDownOutlined />}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="Biên lợi nhuận gộp"
              value={(summary?.grossMargin || 0) * 100}
              precision={1}
              suffix="%"
              valueStyle={{ color: (summary?.grossMargin || 0) >= 0 ? '#3f8600' : '#cf1322' }}
            />
          </Card>
        </Col>
      </Row>

      {/* Filter bar */}
      <Card style={{ marginBottom: 16 }}>
        <Space wrap>
          <DatePicker.RangePicker
            value={dateRange}
            onChange={(dates) => setDateRange(dates)}
            placeholder={['Từ ngày', 'Đến ngày']}
          />
          <Select
            placeholder="Lọc theo chi nhánh"
            allowClear
            style={{ width: 180 }}
            value={branchFilter}
            onChange={setBranchFilter}
            options={branches.map((b) => ({ value: b.id, label: b.name }))}
          />
          <Select
            placeholder="Lọc theo kênh bán"
            allowClear
            style={{ width: 160 }}
            value={channelFilter}
            onChange={setChannelFilter}
            options={[
              { value: 'MANUAL', label: 'Thủ công' },
              { value: 'POS', label: 'Tại quầy (POS)' },
              { value: 'SHOPEE', label: 'Shopee' },
              { value: 'TIKTOK', label: 'TikTok Shop' },
            ]}
          />
          <Select
            placeholder="Tìm theo SKU"
            showSearch
            allowClear
            style={{ width: 220 }}
            filterOption={false}
            onSearch={handleSearchSku}
            value={skuFilter}
            onChange={setSkuFilter}
            options={skus.map((s) => ({ value: s.id, label: `${s.code} - ${s.productName}` }))}
          />
        </Space>
      </Card>

      {/* Chi tiết theo SKU */}
      <Card title="Chi tiết Lợi nhuận theo từng mã sản phẩm (SKU)">
        <Table
          rowKey="skuId"
          columns={columns}
          dataSource={data?.bySku || []}
          loading={loading}
          pagination={{ pageSize: 10 }}
        />
      </Card>
    </div>
  )
}
