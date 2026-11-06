import { useEffect, useState } from 'react'
import { Badge, Button, Layout, Menu, Result, Space, Tag, Typography, notification } from 'antd'
import {
  ApartmentOutlined, AppstoreOutlined, AuditOutlined, BarChartOutlined, BellOutlined,
  ContainerOutlined, DashboardOutlined, DatabaseOutlined, LineChartOutlined, LogoutOutlined,
  SettingOutlined, ShopOutlined, ShoppingCartOutlined, SwapOutlined, TagsOutlined, TeamOutlined,
  ThunderboltOutlined, UsergroupAddOutlined,
} from '@ant-design/icons'
import { Outlet, useLocation, useNavigate } from 'react-router-dom'
import { Client } from '@stomp/stompjs'
import { useAuth } from '../auth/AuthContext'
import { alertApi } from '../api/services'
import { tokenStore } from '../api/client'
import type { Role } from '../api/types'

const { Header, Sider, Content } = Layout

interface Entry {
  key: string
  label: string
  icon: React.ReactNode
  roles: Role[]
}

const ENTRIES: Entry[] = [
  { key: '/dashboard', label: 'Tổng quan', icon: <DashboardOutlined />, roles: ['OWNER', 'STAFF'] },
  { key: '/orders', label: 'Đơn hàng', icon: <ShoppingCartOutlined />, roles: ['OWNER', 'STAFF'] },
  { key: '/products', label: 'Sản phẩm', icon: <AppstoreOutlined />, roles: ['OWNER', 'STAFF'] },
  { key: '/categories', label: 'Danh mục', icon: <ApartmentOutlined />, roles: ['OWNER'] },
  { key: '/brands', label: 'Thương hiệu', icon: <TagsOutlined />, roles: ['OWNER'] },
  { key: '/suppliers', label: 'Nhà cung cấp', icon: <TeamOutlined />, roles: ['OWNER', 'STAFF'] },
  { key: '/receipts', label: 'Phiếu nhập kho', icon: <ContainerOutlined />, roles: ['OWNER', 'STAFF'] },
  { key: '/transfers', label: 'Chuyển kho', icon: <SwapOutlined />, roles: ['OWNER', 'STAFF'] },
  { key: '/stocktakes', label: 'Kiểm kê kho', icon: <AuditOutlined />, roles: ['OWNER', 'STAFF'] },
  { key: '/stock', label: 'Tồn kho & sổ cái', icon: <DatabaseOutlined />, roles: ['OWNER', 'STAFF'] },
  { key: '/stock-reports', label: 'Báo cáo tồn kho', icon: <BarChartOutlined />, roles: ['OWNER'] },
  { key: '/profit-report', label: 'Báo cáo lợi nhuận', icon: <LineChartOutlined />, roles: ['OWNER'] },
  { key: '/branches', label: 'Chi nhánh / Kho', icon: <ShopOutlined />, roles: ['OWNER'] },
  { key: '/users', label: 'Nhân viên', icon: <UsergroupAddOutlined />, roles: ['OWNER'] },
  { key: '/settings', label: 'Cài đặt hệ thống', icon: <SettingOutlined />, roles: ['OWNER'] },
  { key: '/simulator', label: 'Giả lập & Webhook', icon: <ThunderboltOutlined />, roles: ['OWNER'] },
]

const ROLE_LABEL: Record<Role, string> = { OWNER: 'Chủ cửa hàng', STAFF: 'Nhân viên', CASHIER: 'Thu ngân' }

// Âm thanh thông báo (Web Audio API)
function playNotificationSound() {
  try {
    const AudioCtx = window.AudioContext || (window as unknown as { webkitAudioContext: typeof AudioContext }).webkitAudioContext
    const ctx = new AudioCtx()
    const osc = ctx.createOscillator()
    const gain = ctx.createGain()
    osc.connect(gain)
    gain.connect(ctx.destination)
    osc.type = 'sine'
    osc.frequency.setValueAtTime(587.33, ctx.currentTime) // D5
    osc.frequency.setValueAtTime(880, ctx.currentTime + 0.1) // A5
    gain.gain.setValueAtTime(0.2, ctx.currentTime)
    gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 0.5)
    osc.start()
    osc.stop(ctx.currentTime + 0.5)
  } catch { /* ignore */ }
}

export default function AppLayout() {
  const { user, logout, hasRole } = useAuth()
  const navigate = useNavigate()
  const { pathname } = useLocation()
  const [openAlertsCount, setOpenAlertsCount] = useState(0)

  useEffect(() => {
    if (!user) return

    alertApi.count().then((r) => setOpenAlertsCount(r.openAlerts)).catch(() => undefined)

    const tokens = tokenStore.get()
    const token = tokens ? tokens.accessToken : ''
    const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws'
    const brokerURL = `${protocol}://${window.location.host}/ws`

    const client = new Client({
      brokerURL,
      connectHeaders: { Authorization: `Bearer ${token}` },
      debug: () => {},
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
    })

    client.onConnect = () => {
      // 1. Đơn hàng mới
      client.subscribe(`/topic/tenants/${user.tenantId}/orders/new`, (message) => {
        if (message.body) {
          try {
            const data = JSON.parse(message.body)
            playNotificationSound()
            notification.info({
              message: '🔔 Có đơn hàng mới!',
              description: `Mã đơn: ${data.orderCode} - Kênh: ${data.channel || 'POS'} - Tổng: ${Number(data.totalAmount || 0).toLocaleString('vi-VN')} ₫`,
              placement: 'topRight',
              duration: 6,
            })
          } catch { /* ignore */ }
        }
      })

      // 2. Cảnh báo tồn kho
      client.subscribe(`/topic/tenants/${user.tenantId}/alerts`, (message) => {
        if (message.body) {
          try {
            const data = JSON.parse(message.body)
            playNotificationSound()
            notification.warning({
              message: '⚠️ Cảnh báo tồn kho thấp!',
              description: `SKU ${data.skuCode} tại chi nhánh đã xuống dưới ngưỡng đặt lại!`,
              placement: 'topRight',
              duration: 6,
            })
            alertApi.count().then((r) => setOpenAlertsCount(r.openAlerts)).catch(() => undefined)
          } catch { /* ignore */ }
        }
      })
    }

    client.activate()

    const interval = setInterval(() => {
      alertApi.count().then((r) => setOpenAlertsCount(r.openAlerts)).catch(() => undefined)
    }, 30000)

    return () => {
      clearInterval(interval)
      client.deactivate()
    }
  }, [user])

  if (user && user.role === 'CASHIER') {
    return (
      <Result
        status="info"
        title="Tài khoản Thu ngân dùng ứng dụng POS"
        subTitle="Trang quản trị chỉ dành cho Chủ cửa hàng và Nhân viên"
        extra={<Button onClick={() => logout().then(() => navigate('/login'))}>Đăng xuất</Button>}
      />
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
        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={selected ? [selected] : []}
          items={items}
          onClick={({ key }) => navigate(key)}
        />
      </Sider>
      <Layout>
        <Header style={{ background: '#fff', display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '0 16px' }}>
          <Space>
            <Typography.Text strong>{user?.tenantName}</Typography.Text>
            {openAlertsCount > 0 && (
              <Badge count={openAlertsCount} overflowCount={99} title={`${openAlertsCount} cảnh báo tồn thấp`}>
                <Button size="small" type="text" icon={<BellOutlined style={{ color: '#cf1322' }} />} onClick={() => navigate('/dashboard')}>
                  Cảnh báo
                </Button>
              </Badge>
            )}
          </Space>
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