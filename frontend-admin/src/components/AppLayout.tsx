import { Button, Layout, Menu, Result, Space, Tag, Typography } from 'antd'
import {
  ApartmentOutlined, AppstoreOutlined, AuditOutlined, ContainerOutlined, DatabaseOutlined,
  LineChartOutlined, LogoutOutlined, ShopOutlined, ShoppingCartOutlined, SwapOutlined,
  TagsOutlined, TeamOutlined, ThunderboltOutlined,
} from '@ant-design/icons'
import { Outlet, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import type { Role } from '../api/types'

const { Header, Sider, Content } = Layout

interface Entry {
  key: string
  label: string
  icon: React.ReactNode
  roles: Role[]
}

/** Menu theo vai tro (FR-AUTH-03): moi muc chi hien voi vai tro duoc phep. */
const ENTRIES: Entry[] = [
  { key: '/orders', label: 'Đơn hàng', icon: <ShoppingCartOutlined />, roles: ['OWNER', 'STAFF'] },
  { key: '/products', label: 'Sản phẩm', icon: <AppstoreOutlined />, roles: ['OWNER', 'STAFF'] },
  { key: '/categories', label: 'Danh mục', icon: <ApartmentOutlined />, roles: ['OWNER'] },
  { key: '/brands', label: 'Thương hiệu', icon: <TagsOutlined />, roles: ['OWNER'] },
  { key: '/suppliers', label: 'Nhà cung cấp', icon: <TeamOutlined />, roles: ['OWNER', 'STAFF'] },
  { key: '/receipts', label: 'Phiếu nhập kho', icon: <ContainerOutlined />, roles: ['OWNER', 'STAFF'] },
  { key: '/transfers', label: 'Chuyển kho', icon: <SwapOutlined />, roles: ['OWNER', 'STAFF'] },
  { key: '/stocktakes', label: 'Kiểm kê kho', icon: <AuditOutlined />, roles: ['OWNER', 'STAFF'] },
  { key: '/stock', label: 'Tồn kho & sổ cái', icon: <DatabaseOutlined />, roles: ['OWNER', 'STAFF'] },
  { key: '/profit-report', label: 'Báo cáo lợi nhuận', icon: <LineChartOutlined />, roles: ['OWNER'] },
  { key: '/simulator', label: 'Giả lập & Webhook', icon: <ThunderboltOutlined />, roles: ['OWNER'] },
]

const ROLE_LABEL: Record<Role, string> = { OWNER: 'Chủ cửa hàng', STAFF: 'Nhân viên', CASHIER: 'Thu ngân' }

export default function AppLayout() {
  const { user, logout, hasRole } = useAuth()
  const navigate = useNavigate()
  const { pathname } = useLocation()

  if (user && user.role === 'CASHIER') {
    return (
      <Result status="info" title="Tài khoản Thu ngân dùng ứng dụng POS"
        subTitle="Trang quản trị chỉ dành cho Chủ cửa hàng và Nhân viên"
        extra={<Button onClick={() => logout().then(() => navigate('/login'))}>Đăng xuất</Button>} />
    )
  }

  const items = ENTRIES.filter((e) => hasRole(...e.roles)).map(({ key, label, icon }) => ({ key, label, icon }))
  const selected = ENTRIES.find((e) => pathname.startsWith(e.key))?.key

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider breakpoint="lg" collapsedWidth={0}>
        <div style={{ color: '#fff', fontWeight: 700, fontSize: 18, padding: 16 }}>
          <ShopOutlined /> OISM Admin
        </div>
        <Menu theme="dark" mode="inline" selectedKeys={selected ? [selected] : []} items={items}
          onClick={({ key }) => navigate(key)} />
      </Sider>
      <Layout>
        <Header style={{ background: '#fff', display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '0 16px' }}>
          <Typography.Text strong>{user?.tenantName}</Typography.Text>
          <Space>
            <span>{user?.fullName}</span>
            {user && <Tag color="blue">{ROLE_LABEL[user.role]}</Tag>}
            <Button icon={<LogoutOutlined />} onClick={() => logout().then(() => navigate('/login'))}>Đăng xuất</Button>
          </Space>
        </Header>
        <Content style={{ margin: 16 }}>
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  )
}
