import type { ReactNode } from 'react'
import { DatabaseOutlined, SafetyCertificateOutlined, ThunderboltOutlined } from '@ant-design/icons'
import BrandLogo from './BrandLogo'

const FEATURES = [
  { icon: <ThunderboltOutlined />, title: 'Gom đơn đa kênh', text: 'POS, đơn tạo tay, Shopee và TikTok Shop về một màn hình, báo đơn mới tức thì.' },
  { icon: <DatabaseOutlined />, title: 'Tồn kho chính xác', text: 'Sổ cái chỉ ghi thêm, giá vốn bình quân, chống bán vượt khi nhiều đơn cùng lúc.' },
  { icon: <SafetyCertificateOutlined />, title: 'Tách biệt từng cửa hàng', text: 'Dữ liệu mỗi cửa hàng riêng biệt, phân quyền Chủ, Nhân viên, Thu ngân.' },
]

/** Bo cuc chung cho man dang nhap / dang ky: ben trai gioi thieu, ben phai la form. */
export default function AuthShell({ title, subtitle, children }: { title: string; subtitle: string; children: ReactNode }) {
  return (
    <div className="auth-page">
      <aside className="auth-hero">
        <div className="auth-hero-brand">
          <BrandLogo size={40} /> OISM
        </div>
        <div>
          <h1>Quản lý bán hàng và tồn kho đa kênh trong một nơi</h1>
          <p>Theo dõi đơn hàng, nhập xuất kho và lợi nhuận của mọi chi nhánh theo thời gian thực.</p>
          <div className="auth-features">
            {FEATURES.map((f) => (
              <div className="auth-feature" key={f.title}>
                <span className="auth-feature-icon">{f.icon}</span>
                <div>
                  <b>{f.title}</b>
                  <span>{f.text}</span>
                </div>
              </div>
            ))}
          </div>
        </div>
        <div className="auth-channels">
          {['POS tại quầy', 'Shopee', 'TikTok Shop', 'Đơn tạo tay'].map((c) => (
            <span className="auth-channel" key={c}>{c}</span>
          ))}
        </div>
      </aside>
      <main className="auth-panel">
        <div className="auth-form">
          <div className="auth-mobile-brand"><BrandLogo size={32} /> OISM</div>
          <h2>{title}</h2>
          <p className="sub">{subtitle}</p>
          {children}
        </div>
      </main>
    </div>
  )
}
