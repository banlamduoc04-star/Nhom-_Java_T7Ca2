import axios, { type AxiosError, type InternalAxiosRequestConfig } from 'axios'

export const API_URL: string = import.meta.env.VITE_API_URL ?? 'http://localhost:8080/api/v1'

export interface Tokens {
  accessToken: string
  refreshToken: string
}

const STORAGE_KEY = 'oism.admin.tokens'

/** Token luu o localStorage (SPA); khi len production nen chuyen sang cookie httpOnly. */
export const tokenStore = {
  get(): Tokens | null {
    try {
      const raw = localStorage.getItem(STORAGE_KEY)
      return raw ? (JSON.parse(raw) as Tokens) : null
    } catch {
      return null
    }
  },
  set(tokens: Tokens) {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(tokens))
  },
  clear() {
    localStorage.removeItem(STORAGE_KEY)
  },
}

/**
 * Dia chi STOMP cua backend (FR-SIM-02): cung host voi API, duong dan /ws.
 * Vi du http://localhost:8080/api/v1 -> ws://localhost:8080/ws.
 */
export function websocketUrl(apiUrl: string = API_URL): string {
  const url = new URL(apiUrl, window.location.origin)
  url.protocol = url.protocol === 'https:' ? 'wss:' : 'ws:'
  url.pathname = '/ws'
  url.search = ''
  url.hash = ''
  return url.toString()
}

export const api = axios.create({ baseURL: API_URL })

export const data = <T>(p: Promise<{ data: T }>): Promise<T> => p.then((r) => r.data)

// Tu dong gan access token vao moi request
api.interceptors.request.use((config) => {
  const tokens = tokenStore.get()
  if (tokens) {
    config.headers.Authorization = `Bearer ${tokens.accessToken}`
  }
  return config
})

type RetriableConfig = InternalAxiosRequestConfig & { _retried?: boolean }

let refreshing: Promise<string> | null = null

async function refreshAccessToken(refreshToken: string): Promise<string> {
  // Dung axios tran (khong qua interceptor) de tranh vong lap khi refresh loi
  const { data } = await axios.post<Tokens>(`${API_URL}/auth/refresh`, { refreshToken })
  tokenStore.set({ accessToken: data.accessToken, refreshToken: data.refreshToken })
  return data.accessToken
}

// 401 -> lam moi token mot lan (nhieu request cung loi chi goi refresh mot lan), roi gui lai request
api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const original = error.config as RetriableConfig | undefined
    const tokens = tokenStore.get()
    const isAuthCall = original?.url?.startsWith('/auth/') ?? false
    if (error.response?.status === 401 && tokens && original && !original._retried && !isAuthCall) {
      original._retried = true
      try {
        refreshing ??= refreshAccessToken(tokens.refreshToken).finally(() => {
          refreshing = null
        })
        const accessToken = await refreshing
        original.headers.Authorization = `Bearer ${accessToken}`
        return api(original)
      } catch {
        tokenStore.clear()
        window.location.assign('/login')
      }
    }
    return Promise.reject(error)
  },
)

interface ApiErrorBody {
  errorCode?: string
  message?: string
  details?: { field: string; message: string }[]
}

/** Doi loi tu API thanh cau thong bao tieng Viet kem ma loi {errorCode, message}. */
export function errorText(error: unknown): string {
  const data = (error as AxiosError<ApiErrorBody>)?.response?.data
  if (data?.message) {
    const details = data.details?.length ? ` (${data.details.map((d) => d.message).join('; ')})` : ''
    return `${data.message}${details}${data.errorCode ? ` [${data.errorCode}]` : ''}`
  }
  if ((error as AxiosError)?.code === 'ERR_NETWORK') {
    return 'Không kết nối được máy chủ'
  }
  return 'Có lỗi xảy ra, vui lòng thử lại'
}
