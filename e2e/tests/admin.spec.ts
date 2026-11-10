import { expect, test, type Page } from '@playwright/test'
import { ADMIN_URL, PASSWORD, api, newShop } from './api'

/** Chon gia tri trong antd Select: mo o chon, go tu khoa, chon dong khop. */
async function pickOption(page: Page, field: ReturnType<Page['locator']>, text: string) {
  await field.click()
  await page.keyboard.type(text)
  await page.locator('.ant-select-dropdown:visible .ant-select-item-option').filter({ hasText: text }).first().click()
}

/**
 * Luong Admin dau-cuoi (Ke hoach Sprint 5 M7: dang nhap, tao san pham, nhap kho, xu ly don san)
 * + FR-SIM-02: don webhook san hien popup realtime khong can tai lai trang.
 */
test('owner đăng nhập, tạo sản phẩm, nhập kho, nhận đơn sàn realtime và duyệt', async ({ page }) => {
  const shop = await newShop('adm')
  const skuCode = `ADM-${shop.runId}`.toUpperCase()

  // 1. Dang nhap
  await page.goto(`${ADMIN_URL}/login`)
  await page.getByLabel('Email hoặc số điện thoại').fill(shop.email)
  await page.getByLabel('Mật khẩu').fill(PASSWORD)
  await page.getByRole('button', { name: 'Đăng nhập' }).click()
  await expect(page).toHaveURL(/\/dashboard/)

  // 2. Tao san pham 1 SKU (FR-PROD-02)
  await page.goto(`${ADMIN_URL}/products`)
  await page.getByRole('button', { name: 'Thêm sản phẩm' }).click()
  const productDialog = page.getByRole('dialog', { name: 'Thêm sản phẩm' })
  await productDialog.getByLabel('Tên sản phẩm').fill('Ao Playwright')
  await productDialog.getByPlaceholder('Mã SKU').fill(skuCode)
  await productDialog.getByPlaceholder('Giá lẻ').fill('150000')
  await productDialog.getByRole('button', { name: 'Tạo sản phẩm' }).click()
  await expect(productDialog).toBeHidden()
  await expect(page.getByText('Ao Playwright')).toBeVisible()

  // 3. Nhap kho 10 cai va xac nhan (FR-INV-02, FR-COST-01)
  await page.goto(`${ADMIN_URL}/receipts`)
  await page.getByRole('button', { name: 'Tạo phiếu nhập' }).click()
  const drawer = page.locator('.ant-drawer').filter({ hasText: 'Tạo phiếu nhập' })
  await pickOption(page, drawer.locator('.ant-select').filter({ hasText: 'Gõ mã SKU' }), skuCode)
  await drawer.getByPlaceholder('Số lượng').fill('10')
  await drawer.getByPlaceholder('Đơn giá nhập').fill('100000')
  await drawer.getByRole('button', { name: 'Lưu nháp' }).click()
  await expect(drawer).toBeHidden()
  await page.getByRole('button', { name: 'Xác nhận' }).first().click()
  await page.locator('.ant-popconfirm').getByRole('button', { name: 'Xác nhận' }).click()
  await expect(page.getByText('Đã xác nhận', { exact: true }).first()).toBeVisible()

  const sku = await api<{ content: { id: string; available: number }[] }>('GET', `/skus?keyword=${skuCode}`, shop.token)
  const skuId = sku.content[0].id
  const stock = await api<{ content: { onHand: number }[] }>('GET', `/stock?skuId=${skuId}`, shop.token)
  expect(stock.content[0].onHand).toBe(10)

  // 4. Don san qua webhook -> popup realtime tren trang Don hang (FR-SIM-02)
  await page.goto(`${ADMIN_URL}/orders`)
  await expect(page.getByRole('button', { name: /Tạo đơn/ }).first()).toBeVisible()
  const { apiKey } = await api<{ apiKey: string }>('POST', '/settings/channel-keys/SHOPEE', shop.token)
  // WebSocket ket noi bat dong bo sau khi trang tai: gui lai don den khi popup hien (khong sleep co dinh)
  let orderCode = ''
  await expect(async () => {
    const order = await api<{ code: string }>('POST', '/webhooks/shopee', undefined,
      { branchId: shop.branchId, externalOrderId: `SP-E2E-${Date.now()}`, customerName: 'Khach San E2E',
        items: [{ skuId, quantity: 1 }] }, { 'X-Api-Key': apiKey })
    orderCode = order.code
    await expect(page.locator('.ant-notification-notice').filter({ hasText: order.code })).toBeVisible({ timeout: 3_000 })
  }).toPass({ timeout: 30_000 })
  await expect(page.locator('.ant-notification-notice').filter({ hasText: orderCode })).toContainText('SHOPEE')

  // 5. Duyet don san (Reserved -> Confirmed, tru kho)
  await page.reload()
  const row = page.locator('tr').filter({ hasText: orderCode })
  await row.getByRole('button', { name: 'Duyệt (Trừ kho)' }).click()
  await expect(row.getByText(/Đã duyệt|CONFIRMED|Đã xác nhận/)).toBeVisible()
})
