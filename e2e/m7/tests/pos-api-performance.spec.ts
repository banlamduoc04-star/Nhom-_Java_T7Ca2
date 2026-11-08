import { test, expect } from '@playwright/test'

const API_URL = (
  process.env.M7_API_URL || 'http://localhost:8081/api/v1'
).replace(/\/+$/, '')

const WARMUP = 5
const SAMPLES = 100

function percentile(values: number[], p: number): number {
  const sorted = [...values].sort((a, b) => a - b)
  return sorted[Math.ceil(p * sorted.length) - 1]
}

async function measure(
  request: import('@playwright/test').APIRequestContext,
  url: URL,
  token: string,
) {
  const timings: number[] = []
  let errors = 0

  for (let i = 0; i < WARMUP + SAMPLES; i++) {
    const start = performance.now()

    try {
      const response = await request.get(url.toString(), {
        headers: { Authorization: `Bearer ${token}` },
        timeout: 15000,
      })

      await response.body()
      const elapsed = performance.now() - start

      if (!response.ok()) {
        errors++
        console.log(`HTTP ${response.status()} - ${url.pathname}`)
      } else if (i >= WARMUP) {
        timings.push(elapsed)
      }
    } catch (error) {
      errors++
      console.log(`Request error: ${String(error)}`)
    }
  }

  return {
    endpoint: url.pathname,
    warmup: WARMUP,
    requested: SAMPLES,
    successful: timings.length,
    errors,
    p50_ms: Number(percentile(timings, 0.50)?.toFixed(2)),
    p95_ms: Number(percentile(timings, 0.95)?.toFixed(2)),
    p99_ms: Number(percentile(timings, 0.99)?.toFixed(2)),
    average_ms: Number(
      (timings.reduce((sum, value) => sum + value, 0) /
        Math.max(timings.length, 1)).toFixed(2),
    ),
  }
}

test('M7 POS - measure SKU search and barcode API p95', async ({ page }) => {
  const email = process.env.M7_POS_EMAIL
  const password = process.env.M7_POS_PASSWORD

  test.skip(!email || !password, 'Set M7_POS_EMAIL and M7_POS_PASSWORD locally')

  await page.goto('/')

  await page.locator('input[autocomplete="username"]').fill(email!)
  await page.locator('input[autocomplete="current-password"]').fill(password!)
  await page.locator('form.login button[type="submit"]').click()

  await expect(
    page.locator('input[aria-label="Tìm sản phẩm"]'),
  ).toBeVisible()

  const stored = await page.evaluate(() =>
    localStorage.getItem('oism.pos.tokens'),
  )

  expect(stored, 'POS access token must exist').toBeTruthy()

  const token = (JSON.parse(stored!) as { accessToken: string }).accessToken
  expect(token).toBeTruthy()

  const meResponse = await page.request.get(`${API_URL}/me`, {
    headers: { Authorization: `Bearer ${token}` },
  })

  expect(meResponse.ok(), 'GET /me must succeed').toBeTruthy()

  const me = await meResponse.json() as {
    branchId: string | null
  }

  expect(me.branchId, 'Cashier must have a branch assigned').toBeTruthy()

  const searchUrl = new URL(`${API_URL}/skus`)
  searchUrl.searchParams.set('keyword', 'JEAN-L')
  searchUrl.searchParams.set('status', 'ACTIVE')
  searchUrl.searchParams.set('branchId', me.branchId!)
  searchUrl.searchParams.set('size', '12')

  const barcodeUrl = new URL(`${API_URL}/skus/lookup`)
  barcodeUrl.searchParams.set('barcode', '8930000000132')
  barcodeUrl.searchParams.set('branchId', me.branchId!)

  const searchResult = await measure(page.request, searchUrl, token)
  const barcodeResult = await measure(page.request, barcodeUrl, token)

  console.log('\n===== M7 API PERFORMANCE REPORT =====')
  console.log(JSON.stringify({
    apiBase: API_URL,
    samplesPerEndpoint: SAMPLES,
    results: [searchResult, barcodeResult],
    target_p95_ms: 200,
  }, null, 2))

  for (const result of [searchResult, barcodeResult]) {
    expect(
      result.errors,
      `${result.endpoint}: API requests must not fail`,
    ).toBe(0)

    expect(
      result.successful,
      `${result.endpoint}: all measured requests must succeed`,
    ).toBe(SAMPLES)

    expect(
      result.p95_ms,
      `${result.endpoint}: p95 must be below 200 ms`,
    ).toBeLessThan(200)
  }
})
