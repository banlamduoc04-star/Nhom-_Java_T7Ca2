import { useEffect, useState } from 'react'
import { Button, Card, Form, InputNumber, Space, Typography, message } from 'antd'
import { SettingOutlined } from '@ant-design/icons'
import { settingsApi } from '../../api/services'

export default function SettingsPage() {
  const [loading, setLoading] = useState(false)
  const [saving, setSaving] = useState(false)
  const [form] = Form.useForm()

  const loadSettings = async () => {
    setLoading(true)
    try {
      const data = await settingsApi.get()
      form.setFieldsValue({ reservationMinutes: data.reservationMinutes })
    } catch {
      message.error('Không thể tải cài đặt')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadSettings()
  }, [])

  const handleSave = async () => {
    try {
      const values = await form.validateFields()
      setSaving(true)
      await settingsApi.update(values.reservationMinutes)
      message.success('Đã lưu cấu hình thành công')
    } catch {
      message.error('Lỗi khi lưu cài đặt')
    } finally {
      setSaving(false)
    }
  }

  return (
    <div style={{ maxWidth: 650 }}>
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
    </div>
  )
}

