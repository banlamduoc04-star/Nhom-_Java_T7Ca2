import { useState } from 'react'
import { Alert, Button, Form, Input } from 'antd'
import AuthShell from '../../components/AuthShell'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { useAuth } from '../../auth/AuthContext'
import { errorText } from '../../api/client'

export default function LoginPage() {
  const { user, login } = useAuth()
  const navigate = useNavigate()
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  if (user) {
    return <Navigate to="/" replace />
  }

  const onFinish = async (values: { identifier: string; password: string }) => {
    setSubmitting(true)
    setError(null)
    try {
      await login(values.identifier, values.password)
      navigate('/', { replace: true })
    } catch (e) {
      setError(errorText(e))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <AuthShell title="Đăng nhập" subtitle="Chào mừng trở lại! Đăng nhập để quản lý cửa hàng của bạn.">
      {error && <Alert type="error" message={error} showIcon style={{ marginBottom: 16 }} />}
      <Form layout="vertical" size="large" onFinish={onFinish} requiredMark={false}>
        <Form.Item name="identifier" label="Email hoặc số điện thoại"
          rules={[{ required: true, message: 'Nhập email hoặc số điện thoại' }]}>
          <Input autoFocus autoComplete="username" placeholder="ban@cuahang.vn" />
        </Form.Item>
        <Form.Item name="password" label="Mật khẩu" rules={[{ required: true, message: 'Nhập mật khẩu' }]}>
          <Input.Password autoComplete="current-password" placeholder="••••••••" />
        </Form.Item>
        <Button type="primary" htmlType="submit" block loading={submitting}>Đăng nhập</Button>
      </Form>
      <div className="switch">
        Chưa có cửa hàng? <Link to="/register">Đăng ký cửa hàng</Link>
      </div>
    </AuthShell>
  )
}
