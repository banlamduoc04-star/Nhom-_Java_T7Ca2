import { useCallback, useEffect, useState } from 'react'
import {
  Alert, Button, Card, Col, Form, Input, InputNumber, Progress, Row, Select,
  Statistic, Tag, Typography, message,
} from 'antd'
import {
  CheckCircleOutlined, CloseCircleOutlined, FireOutlined, PlayCircleOutlined, SendOutlined,
} from '@ant-design/icons'
import { branchApi, simulatorApi, skuApi } from '../../api/services'
import type { Branch, SimulationResult, SkuSearchItem } from '../../api/types'

interface SimulateFormValues {
  branchId: string
  channel: 'SHOPEE' | 'LAZADA' | 'TIKTOK'
  skuId: string
  orderCount: number
  quantityPerOrder: number
}

interface WebhookFormValues {
  channel: 'SHOPEE' | 'LAZADA' | 'TIKTOK'
  externalOrderId: string
  branchId: string
  customerName?: string
  customerPhone?: string
  shippingAddress?: string
  skuId: string
  quantity: number
}

export default function SimulatorPage() {
  const [branches, setBranches] = useState<Branch[]>([])
  const [skus, setSkus] = useState<SkuSearchItem[]>([])

  // State cho Flash Sale
  const [simLoading, setSimLoading] = useState(false)
  const [simResult, setSimResult] = useState<SimulationResult | null>(null)

  // State cho Webhook
  const [webhookLoading, setWebhookLoading] = useState(false)
  const [webhookResult, setWebhookResult] = useState<unknown>(null)

  const [simForm] = Form.useForm<SimulateFormValues>()
  const [webhookForm] = Form.useForm<WebhookFormValues>()

  const loadBranches = useCallback(async () => {
    try {
      const bs = await branchApi.list()
      setBranches(bs)
    } catch { /* ignore */ }
  }, [])

  useEffect(() => { loadBranches() }, [loadBranches])

  const handleSearchSku = async (keyword: string) => {
    if (!keyword) return
    try {
      const data = await skuApi.search({ keyword, status: 'ACTIVE', size: 10 })
      setSkus(data.content)
    } catch { /* ignore */ }
  }

  // 1. XỬ LÝ CHẠY FLASH SALE (ĐA LUỒNG)
  const handleRunSimulation = async (values: SimulateFormValues) => {
    setSimLoading(true)
    setSimResult(null)
    try {
      const result = await simulatorApi.simulate({
        branchId: values.branchId,
        channel: values.channel,
        orderCount: values.orderCount,
        items: [{ skuId: values.skuId, quantity: values.quantityPerOrder }],
      })
      setSimResult(result)
      message.success(`Hoàn tất mô phỏng: ${result.successful}/${result.requested} đơn thành công`)
    } catch (err: any) {
      message.error(err.response?.data?.message || 'Mô phỏng thất bại')
    } finally {
      setSimLoading(false)
    }
  }

  // 2. XỬ LÝ BẮN WEBHOOK SÀN thuong mai dien tu
  const handleSendWebhook = async (values: WebhookFormValues) => {
    setWebhookLoading(true)
    setWebhookResult(null)
    try {
      const result = await simulatorApi.sendWebhook(values.channel.toLowerCase(), {
        branchId: values.branchId,
        externalOrderId: values.externalOrderId,
        customerName: values.customerName,
        customerPhone: values.customerPhone,
        shippingAddress: values.shippingAddress,
        items: [{ skuId: values.skuId, quantity: values.quantity }],
      })
      setWebhookResult(result)
      message.success(`Đã nhận webhook và tạo đơn hàng ${result.code}`)
    } catch (err: any) {
      setWebhookResult({ error: err.response?.data || err.message })
      message.error(err.response?.data?.message || 'Gửi webhook thất bại')
    } finally {
      setWebhookLoading(false)
    }
  }

  return (
    <div>
      <Typography.Title level={3} style={{ marginBottom: 16 }}>
        <FireOutlined style={{ color: '#ff4d4f' }} /> Bộ Giả Lập Đơn Hàng & Webhook Sàn TMĐT
      </Typography.Title>

      <Row gutter={24}>
        {/* cột trái FLASH SALE CONCURRENCY SIMULATOR */}
        <Col xs={24} lg={12}>
          <Card title="1. Mô phỏng Đua đơn Flash Sale (Đa luồng)" style={{ height: '100%' }}>
            <Alert
              type="info"
              showIcon
              message="Kiểm thử Chống Bán Quá Tồn (Oversell Prevention)"
              description="Hệ thống sẽ kích hoạt đồng thời N luồng giả lập cùng tranh chấp mua 1 mặt hàng SKU qua cơ chế SELECT FOR UPDATE sắp xếp theo sku_id để đảm bảo không deadlock và không bao giờ bán âm kho."
              style={{ marginBottom: 16 }}
            />

            <Form
              form={simForm}
              layout="vertical"
              onFinish={handleRunSimulation}
              initialValues={{ channel: 'SHOPEE', orderCount: 20, quantityPerOrder: 1 }}
            >
              <Form.Item name="branchId" label="Chi nhánh kho bán" rules={[{ required: true, message: 'Chọn chi nhánh' }]}>
                <Select placeholder="Chọn chi nhánh" options={branches.map((b) => ({ value: b.id, label: b.name }))} />
              </Form.Item>

              <Form.Item name="channel" label="Kênh sàn TMĐT" rules={[{ required: true }]}>
                <Select
                  options={[
                    { value: 'SHOPEE', label: 'Shopee' },
                    { value: 'LAZADA', label: 'Lazada' },
                    { value: 'TIKTOK', label: 'TikTok Shop' },
                  ]}
                />
              </Form.Item>

              <Form.Item name="skuId" label="Sản phẩm SKU thử thách" rules={[{ required: true, message: 'Chọn SKU' }]}>
                <Select
                  showSearch
                  placeholder="Gõ mã hoặc tên SKU để tìm"
                  filterOption={false}
                  onSearch={handleSearchSku}
                  options={skus.map((s) => ({
                    value: s.id,
                    label: `${s.code} - ${s.productName} (Còn khả dụng: ${s.available})`,
                  }))}
                />
              </Form.Item>

              <Row gutter={16}>
                <Col span={12}>
                  <Form.Item name="orderCount" label="Số đơn đồng thời" rules={[{ required: true }]}>
                    <InputNumber min={1} max={100} style={{ width: '100%' }} />
                  </Form.Item>
                </Col>
                <Col span={12}>
                  <Form.Item name="quantityPerOrder" label="Số lượng mỗi đơn" rules={[{ required: true }]}>
                    <InputNumber min={1} max={50} style={{ width: '100%' }} />
                  </Form.Item>
                </Col>
              </Row>

              <Button
                type="primary"
                danger
                icon={<PlayCircleOutlined />}
                htmlType="submit"
                loading={simLoading}
                block
                size="large"
              >
                Chạy kiểm thử Flash Sale
              </Button>
            </Form>

            {/* KẾT QUẢ FLASH SALE  */}
            {simResult && (
              <div style={{
                marginTop: 24,
                padding: 20,
                background: simResult.failed > 0 ? '#fff1f0' : '#f6ffed',
                borderRadius: 8,
                border: simResult.failed > 0 ? '1px solid #ffa39e' : '1px solid #b7eb8f'
              }}>
                <Typography.Title level={4} style={{ color: simResult.failed > 0 ? '#cf1322' : '#389e0d', marginTop: 0 }}>
                  {simResult.failed > 0 ? '🔥 KẾT QUẢ FLASH SALE (ĐÃ CHẶN BÁN VƯỢT)' : '✅ KẾT QUẢ FLASH SALE'}
                </Typography.Title>

                <Row gutter={16} style={{ marginBottom: 16 }}>
                  <Col span={8}>
                    <Statistic title="Tổng yêu cầu" value={simResult.requested} />
                  </Col>
                  <Col span={8}>
                    <Statistic title="Đơn giật thành công" value={simResult.successful} valueStyle={{ color: '#3f8600' }} prefix={<CheckCircleOutlined />} />
                  </Col>
                  <Col span={8}>
                    <Statistic title="Đơn bị chặn (Hết hàng)" value={simResult.failed} valueStyle={{ color: '#cf1322' }} prefix={<CloseCircleOutlined />} />
                  </Col>
                </Row>

                <div style={{ marginBottom: 8, display: 'flex', justifyContent: 'space-between' }}>
                  <span>Thời gian xử lý (Pessimistic Lock):</span>
                  <strong>{simResult.durationMs} ms</strong>
                </div>
                <Progress
                  percent={Math.round((simResult.successful / simResult.requested) * 100)}
                  status={simResult.failed > 0 ? 'exception' : 'success'}
                  strokeColor={{ '0%': '#108ee9', '100%': '#87d068' }}
                />

                {simResult.successfulOrderCodes.length > 0 && (
                  <div style={{ marginTop: 16 }}>
                    <Typography.Text strong>Mã đơn được tạo ({simResult.successfulOrderCodes.length}):</Typography.Text>
                    <div style={{ marginTop: 8, display: 'flex', flexWrap: 'wrap', gap: 6 }}>
                      {simResult.successfulOrderCodes.map((code) => (
                        <Tag key={code} color="success" style={{ fontSize: 14, padding: '4px 8px' }}>
                          {code}
                        </Tag>
                      ))}
                    </div>
                  </div>
                )}

                {simResult.errors.length > 0 && (
                  <div style={{ marginTop: 16, padding: 12, background: '#fff', borderLeft: '4px solid #cf1322' }}>
                    <strong>Log hệ thống (Mẫu):</strong>
                    <p style={{ color: '#cf1322', margin: '4px 0 0 0', fontFamily: 'monospace' }}>
                      {simResult.errors[0]}
                    </p>
                  </div>
                )}
              </div>
            )}
          </Card>
        </Col>

        {/* CỘT PHẢI: WEBHOOK SIMULATOR */}
        <Col xs={24} lg={12}>
          <Card title="2. Thử nghiệm Webhook Đơn hàng từ Sàn TMĐT" style={{ height: '100%' }}>
            <Alert
              type="success"
              showIcon
              message="Đồng bộ Đơn hàng Đa kênh Real-time"
              description="Bắn payload đơn hàng từ sàn TMĐT (Shopee, Lazada, TikTok Shop) vào endpoint public /api/v1/webhooks/{channel} để tự động tạo đơn và giữ hàng ngay lập tức."
              style={{ marginBottom: 16 }}
            />

            <Form
              form={webhookForm}
              layout="vertical"
              onFinish={handleSendWebhook}
              initialValues={{
                channel: 'SHOPEE',
                externalOrderId: `EXT-${Date.now().toString().slice(-6)}`,
                customerName: 'Nguyễn Thị Bích',
                customerPhone: '0988776655',
                shippingAddress: '123 Đường Hai Bà Trưng, Quận 1, TP.HCM',
                quantity: 1,
              }}
            >
              <Row gutter={16}>
                <Col span={12}>
                  <Form.Item name="channel" label="Kênh Webhook" rules={[{ required: true }]}>
                    <Select
                      options={[
                        { value: 'SHOPEE', label: 'Shopee Webhook' },
                        { value: 'LAZADA', label: 'Lazada Webhook' },
                        { value: 'TIKTOK', label: 'TikTok Shop Webhook' },
                      ]}
                    />
                  </Form.Item>
                </Col>
                <Col span={12}>
                  <Form.Item name="externalOrderId" label="Mã đơn trên Sàn" rules={[{ required: true }]}>
                    <Input placeholder="Mã đơn sàn..." />
                  </Form.Item>
                </Col>
              </Row>

              <Form.Item name="branchId" label="Chi nhánh kho xuất" rules={[{ required: true, message: 'Chọn chi nhánh' }]}>
                <Select placeholder="Chọn chi nhánh" options={branches.map((b) => ({ value: b.id, label: b.name }))} />
              </Form.Item>

              <Row gutter={16}>
                <Col span={12}>
                  <Form.Item name="customerName" label="Tên khách hàng">
                    <Input />
                  </Form.Item>
                </Col>
                <Col span={12}>
                  <Form.Item name="customerPhone" label="Số điện thoại">
                    <Input />
                  </Form.Item>
                </Col>
              </Row>

              <Form.Item name="shippingAddress" label="Địa chỉ giao hàng">
                <Input />
              </Form.Item>

              <Row gutter={16}>
                <Col span={18}>
                  <Form.Item name="skuId" label="Sản phẩm SKU đặt mua" rules={[{ required: true, message: 'Chọn SKU' }]}>
                    <Select
                      showSearch
                      placeholder="Tìm SKU theo mã hoặc tên"
                      filterOption={false}
                      onSearch={handleSearchSku}
                      options={skus.map((s) => ({
                        value: s.id,
                        label: `${s.code} - ${s.productName} (Còn: ${s.available})`,
                      }))}
                    />
                  </Form.Item>
                </Col>
                <Col span={6}>
                  <Form.Item name="quantity" label="Số lượng" rules={[{ required: true }]}>
                    <InputNumber min={1} style={{ width: '100%' }} />
                  </Form.Item>
                </Col>
              </Row>

              <Button
                type="primary"
                icon={<SendOutlined />}
                htmlType="submit"
                loading={webhookLoading}
                block
                size="large"
              >
                Bắn Webhook mô phỏng
              </Button>
            </Form>

            {webhookResult !== null && webhookResult !== undefined && (
              <div style={{ marginTop: 24, padding: 16, background: '#fafafa', borderRadius: 8 }}>
                <Typography.Title level={5}>Phản hồi từ Webhook API:</Typography.Title>
                <pre style={{ background: '#f0f0f0', padding: 12, borderRadius: 4, overflowX: 'auto', fontSize: 12, maxHeight: 200 }}>
                  {JSON.stringify(webhookResult, null, 2)}
                </pre>
              </div>
            )}
          </Card>
        </Col>
      </Row>
    </div>
  )
}
