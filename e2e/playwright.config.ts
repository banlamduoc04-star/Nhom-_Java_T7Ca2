import { defineConfig, devices } from '@playwright/test'

/**
 * E2E chay tren ban Docker (docker compose --profile frontend up -d --build, OISM_SEED khong bat buoc).
 * Dia chi doi bang bien moi truong: E2E_API_URL, E2E_ADMIN_URL, E2E_POS_URL.
 * NFR-USA-02: luong POS chay o 3 kich thuoc man hinh (may tinh, tablet, dien thoai).
 */
export default defineConfig({
  testDir: './tests',
  timeout: 60_000,
  expect: { timeout: 10_000 },
  fullyParallel: false,
  workers: 1,
  reporter: [['list'], ['html', { open: 'never' }]],
  use: {
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    locale: 'vi-VN',
  },
  projects: [
    { name: 'admin-desktop', testMatch: /admin\.spec\.ts/, use: { ...devices['Desktop Chrome'], viewport: { width: 1366, height: 768 } } },
    { name: 'pos-desktop', testMatch: /pos\.spec\.ts/, use: { ...devices['Desktop Chrome'], viewport: { width: 1366, height: 768 } } },
    { name: 'pos-tablet', testMatch: /pos\.spec\.ts/, use: { ...devices['Desktop Chrome'], viewport: { width: 768, height: 1024 }, hasTouch: true } },
    { name: 'pos-mobile', testMatch: /pos\.spec\.ts/, use: { ...devices['Pixel 7'], browserName: 'chromium' } },
  ],
})
