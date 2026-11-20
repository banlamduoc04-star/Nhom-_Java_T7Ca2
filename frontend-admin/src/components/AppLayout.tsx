import { useEffect, useState } from 'react'
import { Avatar, Badge, Breadcrumb, Button, Drawer, Dropdown, Grid, Layout, Menu, Result, Tooltip, notification } from 'antd'
import type { MenuProps } from 'antd'
import {
  ApartmentOutlined, AppstoreOutlined, AuditOutlined, BarChartOutlined, BellOutlined,
  ContainerOutlined, DashboardOutlined, DatabaseOutlined, LineChartOutlined, LogoutOutlined,
  MenuFoldOutlined, MenuUnfoldOutlined, SettingOutlined, ShopOutlined, ShoppingCartOutlined, SwapOutlined,
  TagsOutlined, TeamOutlined, ThunderboltOutlined, UsergroupAddOutlined,
} from '@ant-design/icons'
import { Link, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { Client } from '@stomp/stompjs'
import { useAuth } from '../auth/AuthContext'
import { alertApi } from '../api/services'
import { tokenStore, websocketUrl } from '../api/client'
import type { OrderCreatedEvent, Role, StockAlertEvent } from '../api/types'
import BrandLogo from './BrandLogo'

const { Header, Sider, Content } = Layout

interface Entry {
  key: string
  label: string
  icon: React.ReactNode
  roles: Role[]
}

interface Group {
  title: string
  entries: Entry[]
}

/** Menu chia nhom theo nghiep vu, giong cac he thong quan ly ban hang da kenh. */
const GROUPS: Group[] = [
  {
    title: 'Bán hàng',
    entries: [
      { key: '/dashboard', label: 'Tổng quan', icon: <DashboardOutlined />, roles: ['OWNER', 'STAFF'] },
      { key: '/orders', label: 'Đơn hàng', icon: <ShoppingCartOutlined />, roles: ['OWNER', 'STAFF'] },
      { key: '/simulator', label: 'Giả lập & Webhook', icon: <ThunderboltOutlined />, roles: ['OWNER'] },
    ],
  },
  {
    title: 'Sản phẩm',
    entries: [
      { key: '/products', label: 'Sản phẩm', icon: <AppstoreOutlined />, roles: ['OWNER', 'STAFF'] },
      { key: '/categories', label: 'Danh mục', icon: <ApartmentOutlined />, roles: ['OWNER'] },
      { key: '/brands', label: 'Thương hiệu', icon: <TagsOutlined />, roles: ['OWNER'] },
    ],
  },
  {
    title: 'Kho hàng',
    entries: [
      { key: '/stock', label: 'Tồn kho & sổ cái', icon: <DatabaseOutlined />, roles: ['OWNER', 'STAFF'] },
      { key: '/receipts', label: 'Phiếu nhập kho', icon: <ContainerOutlined />, roles: ['OWNER', 'STAFF'] },
      { key: '/transfers', label: 'Chuyển kho', icon: <SwapOutlined />, roles: ['OWNER', 'STAFF'] },
      { key: '/stocktakes', label: 'Kiểm kê kho', icon: <AuditOutlined />, roles: ['OWNER', 'STAFF'] },
      { key: '/suppliers', label: 'Nhà cung cấp', icon: <TeamOutlined />, roles: ['OWNER', 'STAFF'] },
    ],
  },
  {
    title: 'Báo cáo',
    entries: [
      { key: '/stock-reports', label: 'Báo cáo tồn kho', icon: <BarChartOutlined />, roles: ['OWNER', 'STAFF'] },
      { key: '/profit-report', label: 'Báo cáo lợi nhuận', icon: <LineChartOutlined />, roles: ['OWNER'] },
    ],
  },
  {
    title: 'Hệ thống',
    entries: [
      { key: '/branches', label: 'Chi nhánh / Kho', icon: <ShopOutlined />, roles: ['OWNER'] },
      { key: '/users', label: 'Nhân viên', icon: <UsergroupAddOutlined />, roles: ['OWNER'] },
      { key: '/settings', label: 'Cài đặt hệ thống', icon: <SettingOutlined />, roles: ['OWNER'] },
    ],
  },
]

const ENTRIES: Entry[] = GROUPS.flatMap((g) => g.entries)

const ROLE_LABEL: Record<Role, string> = { OWNER: 'Chủ cửa hàng', STAFF: 'Nhân viên', CASHIER: 'Thu ngân' }

const initials = (name?: string) =>
  (name ?? '?').trim().split(/\s+/).slice(-2).map((w) => w[0]?.toUpperCase() ?? '').join('')

// Âm thanh thông báo 
function playNotificationSound() {
  try {
    const AudioCtx = window.AudioContext || (window as unknown as { webkitAudioContext: typeof AudioContext }).webkitAudioContext
    const ctx = new AudioCtx()
    const osc = ctx.createOscillator()
    const gain = ctx.createGain()
    osc.connect(gain)
    gain.connect(ctx.destination)
    osc.type = 'sine'
    osc.frequency.setValueAtTime(587.33, ctx.currentTime)
    osc.frequency.setValueAtTime(880, ctx.currentTime + 0.1) 
    gain.gain.setValueAtTime(0.2, ctx.currentTime)
    gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 0.5)
    osc.start()
    osc.stop(ctx.currentTime + 0.5)
  } catch { }
}

export default function AppLayout() {
  const { user, logout, hasRole } = useAuth()
  const navigate = useNavigate()
  const { pathname } = useLocation()
  const [openAlertsCount, setOpenAlertsCount] = useState(0)
  const [collapsed, setCollapsed] = useState(false)
  // Duoi 768px: menu la ngan keo (Drawer) phu len trang, khong chiem cot ben trai
  const screens = Grid.useBreakpoint()
  const isPhone = screens.md === false
  const [drawerOpen, setDrawerOpen] = useState(false)

  useEffect(() => {
    if (!user) return

    alertApi.count().then((r) => setOpenAlertsCount(r.openAlerts)).catch(() => undefined)

    // FR-SIM-02: /ws nam o backend (suy ra tu VITE_API_URL), khong phai host cua trang Admin
    const client = new Client({
      brokerURL: websocketUrl(),
      // doc token moi nhat truoc moi lan (re)connect: access token co the vua duoc lam moi
      beforeConnect: (c) => {
        c.connectHeaders = { Authorization: `Bearer ${tokenStore.get()?.accessToken ?? ''}` }
      },
      debug: () => {},
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
    })

    client.onConnect = () => {
      //Đơn hàng mới
      client.subscribe(`/topic/tenants/${user.tenantId}/orders`, (message) => {
        if (message.body) {
          try {
            const data = JSON.parse(message.body) as OrderCreatedEvent
            playNotificationSound()
            notification.info({
              message: '🔔 Có đơn hàng mới!',
              description: `Mã đơn: ${data.orderCode} - Kênh: ${data.channel || 'POS'} - Tổng: ${Number(data.totalAmount || 0).toLocaleString('vi-VN')} ₫`,
              placement: 'topRight',
              duration: 6,
            })
          } catch {  }
        }
      })

      //Cảnh báo tồn kho
      client.subscribe(`/topic/tenants/${user.tenantId}/alerts`, (message) => {
        if (message.body) {
          try {
            const data = JSON.parse(message.body) as StockAlertEvent
            alertApi.count().then((r) => setOpenAlertsCount(r.openAlerts)).catch(() => undefined)
            if (data.status !== 'OPEN') return // CLOSED: chi cap nhat so dem, khong bao dong
            playNotificationSound()
            notification.warning({
              message: '⚠️ Cảnh báo tồn kho thấp!',
              description: `SKU ${data.skuCode} còn ${data.available} (ngưỡng ${data.threshold})`,
              placement: 'topRight',
              duration: 6,
            })
          } catch {  }
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

  const menuItems: MenuProps['items'] = GROUPS
    .map((g) => ({
      type: 'group' as const,
      key: g.title,
      label: collapsed && !isPhone ? null : g.title.toUpperCase(),
      children: g.entries.filter((e) => hasRole(...e.roles)).map(({ key, label, icon }) => ({ key, label, icon })),
    }))
    .filter((g) => g.children.length > 0)
  // Khop tron doan duong dan: '/stock' khong duoc an '/stock-reports', nhung '/products/:id' van thuoc '/products'
  const current = ENTRIES.find((e) => pathname === e.key || pathname.startsWith(e.key + '/'))
  const group = current ? GROUPS.find((g) => g.entries.includes(current)) : undefined

  const userMenu: MenuProps['items'] = [
    {
      key: 'info',
      disabled: true,
      label: (
        <div className="app-user-menu-info">
          <div className="app-user-menu-name">{user?.fullName}</div>
          <div className="app-user-menu-email">{user?.email}</div>
        </div>
      ),
    },
    { type: 'divider' },
    { key: 'logout', icon: <LogoutOutlined />, label: 'Đăng xuất', danger: true },
  ]

  // Thanh ben day du (desktop) hoac thu gon chi con icon; tren dien thoai luon day du trong ngan keo
  const narrow = collapsed && !isPhone
  const navigation = (
    <>
      <Link to="/dashboard" className="app-brand" aria-label="OISM, về trang Tổng quan" onClick={() => setDrawerOpen(false)}>
        <BrandLogo size={34} />
        {!narrow && (
          <span className="app-brand-text">
            <span className="app-brand-name">OISM</span>
            <span className="app-brand-sub">Bán hàng đa kênh</span>
          </span>
        )}
      </Link>
      <nav className="app-menu-scroll" aria-label="Menu chính">
        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={current ? [current.key] : []}
          items={menuItems}
          onClick={({ key }) => {
            setDrawerOpen(false)
            navigate(key)
          }}
        />
      </nav>
      {!narrow && user?.tenantName && (
        <div className="app-store-card">
          <span className="app-store-icon" aria-hidden="true"><ShopOutlined /></span>
          <div style={{ minWidth: 0 }}>
            <div className="app-store-label">Cửa hàng</div>
            <div className="app-store-name">{user.tenantName}</div>
          </div>
        </div>
      )}
    </>
  )

  return (
    <Layout className="app-shell" hasSider={!isPhone}>
      {isPhone ? (
        <Drawer
          open={drawerOpen}
          onClose={() => setDrawerOpen(false)}
          placement="left"
          size={272}
          closable={false}
          rootClassName="app-drawer"
        >
          {navigation}
        </Drawer>
      ) : (
        <Sider
          width={248}
          breakpoint="lg"
          collapsible
          collapsed={collapsed}
          onCollapse={setCollapsed}
          trigger={null}
          className="app-sider"
        >
          {navigation}
        </Sider>
      )}
      <Layout className="app-main">
        <Header className="app-header">
          <div className="app-header-left">
            {isPhone ? (
              <Button
                type="text"
                aria-label="Mở menu"
                aria-expanded={drawerOpen}
                icon={<MenuUnfoldOutlined />}
                onClick={() => setDrawerOpen(true)}
              />
            ) : (
              <Button
                type="text"
                aria-label={collapsed ? 'Mở rộng menu' : 'Thu gọn menu'}
                aria-expanded={!collapsed}
                icon={collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
                onClick={() => setCollapsed(!collapsed)}
              />
            )}
            <div className="app-header-titles">
              <Breadcrumb
                className="app-breadcrumb"
                items={[
                  { title: 'OISM' },
                  ...(group ? [{ title: group.title }] : []),
                  ...(current ? [{ title: current.label }] : []),
                ]}
              />
              <h1 className="app-page-title">{current?.label ?? 'OISM'}</h1>
            </div>
          </div>
          <div className="app-header-right">
            <Tooltip title={openAlertsCount > 0 ? `${openAlertsCount} cảnh báo tồn thấp` : 'Không có cảnh báo tồn thấp'}>
              <Badge count={openAlertsCount} overflowCount={99} size="small">
                <Button
                  shape="circle"
                  aria-label={openAlertsCount > 0 ? `Cảnh báo tồn kho: ${openAlertsCount} cảnh báo` : 'Cảnh báo tồn kho: không có'}
                  icon={<BellOutlined />}
                  onClick={() => navigate('/dashboard')}
                />
              </Badge>
            </Tooltip>
            <Dropdown
              trigger={['click']}
              placement="bottomRight"
              menu={{
                items: userMenu,
                onClick: ({ key }) => {
                  if (key === 'logout') logout().then(() => navigate('/login'))
                },
              }}
            >
              <button type="button" className="app-user">
                <Avatar size={36} className="app-avatar">{initials(user?.fullName)}</Avatar>
                <span className="app-user-text">
                  <span className="app-user-name">{user?.fullName}</span>
                  {user && <span className="app-user-role">{ROLE_LABEL[user.role]}</span>}
                </span>
              </button>
            </Dropdown>
          </div>
        </Header>
        <Content className="app-content">
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  )
}
