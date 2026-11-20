import { useState } from 'react'
import { Alert, Button, Col, Form, Input, Row } from 'antd'
import AuthShell from '../../components/AuthShell'
import { Link, useNavigate } from 'react-router-dom'
import { authApi } from '../../api/services'
import { errorText } from '../../api/client'
import { useAuth } from '../../auth/AuthContext'

interface Values {
  storeName: string
  fullName: string
  email: string
  phone?: string
  password: string
}

export default function RegisterPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  const onFinish = async (values: Values) => {
    setSubmitting(true)
    setError(null)
    try {
      await authApi.register(values)
      await login(values.email, values.password)
      navigate('/', { replace: true })
    } catch (e) {
      setError(errorText(e))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <AuthShell title="Đăng ký cửa hàng" subtitle="Tạo cửa hàng mới, tài khoản Chủ cửa hàng và chi nhánh mặc định.">
      {error && <Alert type="error" message={error} showIcon style={{ marginBottom: 16 }} />}
      <Form layout="vertical" onFinish={onFinish} requiredMark={false}>
        <Form.Item name="storeName" label="Tên cửa hàng" rules={[{ required: true, max: 150 }]}><Input /></Form.Item>
        <Form.Item name="fullName" label="Họ tên chủ cửa hàng" rules={[{ required: true, max: 150 }]}><Input /></Form.Item>
        <Row gutter={12}>
          <Col xs={24} sm={14}>
            <Form.Item name="email" label="Email" rules={[{ required: true, type: 'email' }]}><Input autoComplete="email" /></Form.Item>
          </Col>
          <Col xs={24} sm={10}>
            <Form.Item name="phone" label="Số điện thoại" rules={[{ pattern: /^$|^[0-9+]{8,15}$/, message: 'Số điện thoại không hợp lệ' }]}>
              <Input />
            </Form.Item>
          </Col>
        </Row>
        <Form.Item name="password" label="Mật khẩu (8–72 ký tự)" rules={[{ required: true, min: 8, max: 72 }]}>
          <Input.Password autoComplete="new-password" />
        </Form.Item>
        <Button type="primary" htmlType="submit" size="large" block loading={submitting}>Tạo cửa hàng</Button>
      </Form>
      <div className="switch">
        Đã có tài khoản? <Link to="/login">Đăng nhập</Link>
      </div>
    </AuthShell>
  )
}
