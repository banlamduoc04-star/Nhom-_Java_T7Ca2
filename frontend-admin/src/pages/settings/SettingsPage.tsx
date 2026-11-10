import { useCallback, useEffect, useState } from 'react'
import { Alert, Button, Card, Form, InputNumber, Popconfirm, Space, Table, Tag, Typography, message } from 'antd'
import { ApiOutlined, SettingOutlined } from '@ant-design/icons'
import { settingsApi } from '../../api/services'
import { errorText } from '../../api/client'
import type { ChannelKeyView, IssuedChannelKey, WebhookChannel } from '../../api/types'

const CHANNEL_LABEL: Record<WebhookChannel, string> = { SHOPEE: 'Shopee', TIKTOK: 'TikTok Shop' }

export default function SettingsPage() {
  const [loading, setLoading] = useState(false)
  const [saving, setSaving] = useState(false)
  const [form] = Form.useForm<{ reservationMinutes: number }>()

  const [keys, setKeys] = useState<ChannelKeyView[]>([])
  const [keysLoading, setKeysLoading] = useState(false)
  const [issuing, setIssuing] = useState<WebhookChannel | null>(null)
  const [issued, setIssued] = useState<IssuedChannelKey | null>(null)

  const loadSettings = useCallback(async () => {
    setLoading(true)
    try {
      const data = await settingsApi.get()
      form.setFieldsValue({ reservationMinutes: data.reservationMinutes })
    } catch (err) {
      message.error(errorText(err))
    } finally {
      setLoading(false)
    }
  }, [form])

  const loadKeys = useCallback(async () => {
    setKeysLoading(true)
    try {
      setKeys(await settingsApi.channelKeys())
    } catch (err) {
      message.error(errorText(err))
    } finally {
      setKeysLoading(false)
    }
  }, [])

  useEffect(() => {
    loadSettings()
    loadKeys()
  }, [loadSettings, loadKeys])

  const handleSave = async (values: { reservationMinutes: number }) => {
    setSaving(true)
    try {
      await settingsApi.update({ reservationMinutes: values.reservationMinutes })
      message.success('Đã lưu cấu hình thành công')
    } catch (err) {
      message.error(errorText(err))
    } finally {
      setSaving(false)
    }
  }

  const handleIssue = async (channel: WebhookChannel) => {
    setIssuing(channel)
    try {
      setIssued(await settingsApi.issueChannelKey(channel))
      await loadKeys()
    } catch (err) {
      message.error(errorText(err))
    } finally {
      setIssuing(null)
    }
  }

  return (
    <div style={{ maxWidth: 760 }}>
      <Typography.Title level={3}>Cài đặt hệ thống cửa hàng</Typography.Title>

      <Card title={<Space><SettingOutlined /><span>Cấu hình giữ hàng & tồn kho</span></Space>} loading={loading}>
        <Form form={form} layout="vertical" onFinish={handleSave}>
          <Form.Item
            name="reservationMinutes"
            label="Thời gian giữ hàng tự động cho đơn hàng online / Webhook (Phút)"
            extra="Sau thời gian này, nếu đơn hàng ở trạng thái Giữ hàng (Reserved) mà chưa được duyệt (Confirmed), tác vụ nền sẽ tự động hủy đơn và hoàn trả hàng khả dụng (Available) cho kho."
            rules={[
              { required: true, message: 'Vui lòng nhập số phút' },
              { type: 'number', min: 1, max: 1440, message: 'Thời gian từ 1 đến 1440 phút (24 giờ)' },
            ]}
          >
            <InputNumber style={{ width: '100%' }} min={1} max={1440} placeholder="30" addonAfter="phút" />
          </Form.Item>

          <Button type="primary" htmlType="submit" loading={saving}>
            Lưu cài đặt
          </Button>
        </Form>
      </Card>

      <Card
        style={{ marginTop: 24 }}
        title={<Space><ApiOutlined /><span>API key webhook sàn (FR-SIM-01)</span></Space>}
      >
        <Typography.Paragraph type="secondary">
          Sàn gọi <code>POST /api/v1/webhooks/{'{shopee|tiktok}'}</code> kèm header <code>X-Api-Key</code>.
          Hệ thống chỉ lưu bản băm của key; key chỉ hiện <strong>một lần</strong> ngay sau khi sinh.
          Sinh key mới thì key cũ của sàn đó hết hiệu lực.
        </Typography.Paragraph>

        {issued && (
          <Alert
            type="success"
            showIcon
            closable
            onClose={() => setIssued(null)}
            style={{ marginBottom: 16 }}
            message={`Key mới cho ${CHANNEL_LABEL[issued.channel]} - hãy sao chép ngay, sẽ không hiển thị lại`}
            description={<Typography.Text code copyable>{issued.apiKey}</Typography.Text>}
          />
        )}

        <Table<ChannelKeyView>
          rowKey="channel"
          loading={keysLoading}
          pagination={false}
          dataSource={keys}
          locale={{ emptyText: 'Chưa có dữ liệu' }}
          columns={[
            { title: 'Sàn', dataIndex: 'channel', render: (c: WebhookChannel) => CHANNEL_LABEL[c] },
            {
              title: 'Trạng thái',
              dataIndex: 'configured',
              render: (configured: boolean) => configured
                ? <Tag color="green">Đã có key</Tag>
                : <Tag>Chưa có key</Tag>,
            },
            {
              title: 'Ngày sinh key',
              dataIndex: 'createdAt',
              render: (v: string | null) => (v ? new Date(v).toLocaleString('vi-VN') : '—'),
            },
            {
              title: '',
              key: 'action',
              render: (_: unknown, row: ChannelKeyView) => row.configured ? (
                <Popconfirm
                  title="Sinh key mới?"
                  description="Key hiện tại của sàn này sẽ hết hiệu lực ngay."
                  okText="Sinh key mới"
                  cancelText="Hủy"
                  onConfirm={() => handleIssue(row.channel)}
                >
                  <Button loading={issuing === row.channel}>Sinh lại key</Button>
                </Popconfirm>
              ) : (
                <Button type="primary" loading={issuing === row.channel} onClick={() => handleIssue(row.channel)}>
                  Sinh key
                </Button>
              ),
            },
          ]}
        />
      </Card>
    </div>
  )
}
