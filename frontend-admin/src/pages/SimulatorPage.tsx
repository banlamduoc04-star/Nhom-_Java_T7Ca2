import { useCallback, useEffect, useState } from 'react'
import {
  Alert, Button, Card, Col, Form, Input, InputNumber, Progress, Row, Select, Space,
  Statistic, Tag, Typography, message,
} from 'antd'
import {
  CheckCircleOutlined, CloseCircleOutlined, FireOutlined, PlayCircleOutlined, SendOutlined,
} from '@ant-design/icons'
import { branchApi, simulatorApi, skuApi } from '../api/services'
import type { Branch, SimulationResult, SkuSearchItem } from '../api/types'

export default function SimulatorPage() {
  const [branches, setBranches] = useState<Branch[]>([])
  const [skus, setSkus] = useState<SkuSearchItem[]>([])
  const [simLoading, setSimLoading] = useState(false)
  const [simResult, setSimResult] = useState<SimulationResult | null>(null)

  const [webhookLoading, setWebhookLoading] = useState(false)
  const [webhookResult, setWebhookResult] = useState<any>(null)

  const [simForm] = Form.useForm()
  const [webhookForm] = Form.useForm()

  const loadBranches = useCallback(async () => {
    try {
      const bs = await branchApi.list()
      setBranches(bs)
    } catch {
      // ignore
    }
  }, [])

  useEffect(() => {
    loadBranches()
  }, [loadBranches])

  const handleSearchSku = async (keyword: string) => {
    if (!keyword) return
    try {
      const data = await skuApi.search({ keyword, status: 'ACTIVE', size: 10 })
      setSkus(data.content)
    } catch {
      // ignore
    }
  }

  const handleRunSimulation = async (values: any) => {
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

  const handleSendWebhook = async (values: any) => {
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
        {/* FLASH SALE CONCURRENCY SIMULATOR */}
        <Col span={12}>
          <Card title="1. Mô phỏng Đua đơn Flash Sale (Đa luồng)">
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

            {simResult && (
              <div style={{ marginTop: 24, padding: 16, background: '#fafafa', borderRadius: 8 }}>
                <Typography.Title level={5}>Kết quả kiểm thử đồng thời:</Typography.Title>
                <Row gutter={16} style={{ marginBottom: 16 }}>
                  <Col span={8}>
                    <Statistic title="Tổng số yêu cầu" value={simResult.requested} />
                  </Col>
                  <Col span={8}>
                    <Statistic
                      title="Thành công"
                      value={simResult.successful}
                      valueStyle={{ color: '#3f8600' }}
                      prefix={<CheckCircleOutlined />}
                    />
                  </Col>
                  <Col span={8}>
                    <Statistic
                      title="Từ chối (Hết tồn)"
                      value={simResult.failed}
                      valueStyle={{ color: '#cf1322' }}
                      prefix={<CloseCircleOutlined />}
                    />
                  </Col>
                </Row>
                <p><strong>Thời gian xử lý:</strong> {simResult.durationMs} ms</p>
                <Progress
                  percent={Math.round((simResult.successful / simResult.requested) * 100)}
                  status={simResult.failed > 0 ? 'normal' : 'success'}
                />

                {simResult.successfulOrderCodes.length > 0 && (
                  <div style={{ marginTop: 12 }}>
                    <strong>Mã các đơn giữ hàng thành công:</strong>
                    <div style={{ maxHeight: 100, overflowY: 'auto', marginTop: 4 }}>
                      <Space wrap size={[4, 4]}>
                        {simResult.successfulOrderCodes.map((c) => (
                          <Tag color="green" key={c}>{c}</Tag>
                        ))}
                      </Space>
                    </div>
                  </div>
                )}

                {simResult.errors.length > 0 && (
                  <div style={{ marginTop: 12 }}>
                    <strong style={{ color: '#cf1322' }}>Thông điệp chặn (Oversell Prevention):</strong>
                    <p style={{ color: '#888', fontSize: 13 }}>{simResult.errors[0]}</p>
                  </div>
                )}
              </div>
            )}
          </Card>
        </Col>

        {/* WEBHOOK SIMULATOR */}
        <Col span={12}>
          <Card title="2. Thử nghiệm Webhook Đơn hàng từ Sàn TMĐT">
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

            {webhookResult && (
              <div style={{ marginTop: 24, padding: 16, background: '#fafafa', borderRadius: 8 }}>
                <Typography.Title level={5}>Phản hồi từ Webhook API:</Typography.Title>
                <pre style={{ background: '#f0f0f0', padding: 12, borderRadius: 4, overflowX: 'auto', fontSize: 12 }}>
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
