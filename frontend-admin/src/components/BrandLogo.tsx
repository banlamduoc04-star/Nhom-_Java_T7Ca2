/** Logo OISM (khoi hop ton kho), dung chung cho thanh ben va man dang nhap. */
export default function BrandLogo({ size = 32 }: { size?: number }) {
  return (
    <svg width={size} height={size} viewBox="0 0 512 512" aria-hidden="true" style={{ flexShrink: 0 }}>
      <defs>
        <linearGradient id="oism-logo" x1="0" y1="0" x2="1" y2="1">
          <stop offset="0" stopColor="#6366f1" />
          <stop offset="1" stopColor="#4338ca" />
        </linearGradient>
      </defs>
      <rect width="512" height="512" rx="112" fill="url(#oism-logo)" />
      <path d="M256 104 392 178v156L256 408 120 334V178z" fill="none" stroke="#fff" strokeWidth="30" strokeLinejoin="round" />
      <path d="M120 178 256 252l136-74M256 252v156" fill="none" stroke="#fff" strokeWidth="30" strokeLinejoin="round" />
      <circle cx="256" cy="252" r="22" fill="#a5b4fc" />
    </svg>
  )
}
