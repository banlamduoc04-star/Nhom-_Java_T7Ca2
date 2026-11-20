import { channelStyle } from '../theme'

/** Nhan kenh ban co mau rieng (POS, tao tay, Shopee, TikTok) de nhin la nhan ra nguon don. */
export default function ChannelTag({ channel }: { channel: string }) {
  const style = channelStyle[channel] ?? { label: channel, color: '#475569', bg: '#f1f5f9' }
  return (
    <span className="oism-pill" style={{ color: style.color, background: style.bg }} title={channel}>
      {style.label}
    </span>
  )
}
