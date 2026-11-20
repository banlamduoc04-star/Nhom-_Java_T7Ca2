import type { ThemeConfig } from 'antd'

/**
 * Bo mau va token giao dien Admin. Moi trang dung Ant Design nen chi can doi o day
 * (khong ghi cung mau trong tung trang).
 */
export const palette = {
  primary: '#4f46e5', // indigo
  primarySoft: '#eef2ff',
  sidebar: '#0f172a', // slate-900
  sidebarHover: '#1e293b',
  sidebarText: '#94a3b8',
  page: '#f4f6fb',
  border: '#e5e7eb',
  text: '#0f172a',
  muted: '#5f6b7f', // chu phu: >= 4.7:1 tren nen trang, nen trang chu (#f4f6fb) va o thong ke (#eef0f4)
  placeholder: '#6b7789', // 4.5:1 tren nen trang
  success: '#16a34a',
  warning: '#d97706',
  danger: '#dc2626',
  info: '#0284c7',
  // Ban dam hon cho CHU (so lieu, nhan trang thai): dat tuong phan 4.5:1 tren nen trang va nen nhat
  successText: '#15803d',
  warningText: '#b45309',
  dangerText: '#b91c1c',
  infoText: '#0369a1',
  sidebarGroupTitle: '#8391a7',
}

/** Mau nhan dien tung kenh ban (FR-ORD-01). */
export const channelStyle: Record<string, { label: string; color: string; bg: string }> = {
  POS: { label: 'POS tại quầy', color: '#7c3aed', bg: '#f5f3ff' },
  MANUAL: { label: 'Tạo tay', color: '#475569', bg: '#f1f5f9' },
  SHOPEE: { label: 'Shopee', color: '#c2410c', bg: '#fff7ed' },
  TIKTOK: { label: 'TikTok Shop', color: '#0f172a', bg: '#e2e8f0' },
}

export const theme: ThemeConfig = {
  token: {
    colorPrimary: palette.primary,
    // Mau trang thai cua Ant Design to ca chu (nhan Tag, Alert, so lieu) nen dung ban dam de dat 4.5:1
    colorInfo: palette.infoText,
    colorLink: palette.primary,
    colorSuccess: palette.successText,
    colorWarning: palette.warningText,
    colorError: palette.dangerText,
    // Nen nhat cua trang thai: dat rieng vi Ant Design suy ra nen xam tu mau dam o tren
    colorSuccessBg: '#f0fdf4',
    colorSuccessBorder: '#bbf7d0',
    colorWarningBg: '#fffbeb',
    colorWarningBorder: '#fde68a',
    colorErrorBg: '#fef2f2',
    colorErrorBorder: '#fecaca',
    colorInfoBg: '#f0f9ff',
    colorInfoBorder: '#bae6fd',
    colorBgLayout: palette.page,
    colorText: palette.text,
    colorTextSecondary: palette.muted,
    // Mac dinh cua Ant Design (den 45% / 25%) chi dat 3.3:1 va 1.8:1: dung chung mau chu phu
    colorTextTertiary: palette.muted,
    colorTextDescription: palette.muted,
    colorTextPlaceholder: palette.placeholder,
    colorBorderSecondary: '#eef0f4',
    borderRadius: 8,
    borderRadiusLG: 12,
    fontFamily: "Inter, 'Segoe UI', system-ui, -apple-system, Roboto, 'Helvetica Neue', Arial, sans-serif",
    fontSize: 14,
    controlHeight: 36,
    boxShadowTertiary: '0 1px 2px rgba(15, 23, 42, 0.04), 0 1px 3px rgba(15, 23, 42, 0.06)',
  },
  components: {
    Layout: {
      siderBg: palette.sidebar,
      headerBg: '#ffffff',
      headerHeight: 64,
      headerPadding: '0 24px',
      bodyBg: palette.page,
      triggerBg: palette.sidebarHover,
    },
    Menu: {
      darkItemBg: palette.sidebar,
      darkSubMenuItemBg: palette.sidebar,
      darkItemColor: palette.sidebarText,
      darkItemHoverBg: palette.sidebarHover,
      darkItemHoverColor: '#ffffff',
      darkItemSelectedBg: palette.primary,
      darkItemSelectedColor: '#ffffff',
      darkGroupTitleColor: palette.sidebarGroupTitle,
      itemBorderRadius: 8,
      itemMarginInline: 12,
      itemHeight: 40,
      groupTitleFontSize: 11,
    },
    Card: {
      headerFontSize: 16,
      headerHeight: 56,
    },
    Table: {
      headerBg: '#f8fafc',
      headerColor: palette.muted,
      headerSplitColor: 'transparent',
      rowHoverBg: '#f8faff',
      cellPaddingBlock: 12,
    },
    Button: {
      fontWeight: 500,
      primaryShadow: '0 1px 2px rgba(79, 70, 229, 0.25)',
    },
    Tag: {
      defaultBg: '#f1f5f9',
    },
    Statistic: {
      contentFontSize: 26,
    },
  },
}
