import { Navigate, Outlet } from 'react-router-dom'
import { Result, Button, Spin } from 'antd'
import { useAuth } from './AuthContext'
import type { Role } from '../api/types'

/** Chua dang nhap -> ve /login. */
export function RequireAuth() {
  const { user, loading } = useAuth()
  if (loading) {
    return <Spin fullscreen />
  }
  return user ? <Outlet /> : <Navigate to="/login" replace />
}

/** Chan trang theo vai tro (FR-AUTH-03); API van kiem tra lai bang @PreAuthorize. */
export function RequireRole({ roles }: { roles: Role[] }) {
  const { hasRole } = useAuth()
  if (!hasRole(...roles)) {
    return (
      <Result status="403" title="403" subTitle="Bạn không có quyền truy cập chức năng này"
        extra={<Button href="/">Về trang chủ</Button>} />
    )
  }
  return <Outlet />
}
