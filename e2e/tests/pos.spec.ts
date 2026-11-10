import { expect, test } from '@playwright/test'
import { POS_URL, PASSWORD, api, cashier, newShop, stockedSku } from './api'

/**
 * FR-POS-01..04, NFR-USA-02: thu ngan quet ma vach + Enter (quet lai tang so luong), F9, chon tien, xac nhan
 * -> hoa don 80 mm hien va hop thoai in tu mo; ton kho tru dung. Chay o 3 kich thuoc man hinh.
 */
test('thu ngân quét 2 lần, F9, thanh toán tiền mặt, in hóa đơn tự động', async ({ page }) => {
  const shop = await newShop('pos')
  const barcode = `893${Date.now().toString().slice(-9)}`
  const skuId = await stockedSku(shop, `E2E-${shop.runId}`, barcode, 10)
  const cashierEmail = await cashier(shop)

  // Dem so lan goi window.print (FR-POS-03: thanh toan xong tu mo hop thoai in)
  await page.addInitScript(() => {
    const w = window as unknown as { __printCalls: number }
    w.__printCalls = 0
    window.print = () => { w.__printCalls++ }
  })

  await page.goto(POS_URL)
  await page.getByLabel('Email hoặc số điện thoại').fill(cashierEmail)
  await page.getByLabel('Mật khẩu').fill(PASSWORD)
  await page.getByRole('button', { name: 'Đăng nhập' }).click()

  const search = page.getByLabel('Tìm sản phẩm')
  await expect(search).toBeVisible()
  // Quet ma vach = go nhanh + Enter; quet lai thi tang so luong
  for (let i = 0; i < 2; i++) {
    await search.fill(barcode)
    await search.press('Enter')
    await expect(page.getByText(`Đã thêm E2E-${shop.runId}`)).toBeVisible()
  }
  await expect(page.getByLabel(`Số lượng E2E-${shop.runId}`)).toHaveValue('2')

  // Toi da 3 thao tac sau khi quet (NFR-USA-02): F9 -> 500k -> Xac nhan
  await page.keyboard.press('F9')
  const payDialog = page.getByRole('dialog', { name: 'Thanh toán' })
  await expect(payDialog).toBeVisible()
  await payDialog.getByRole('button', { name: '500k' }).click()
  await payDialog.getByRole('button', { name: /Xác nhận & In bill/ }).click()

  const receipt = page.getByRole('dialog', { name: 'Hóa đơn thanh toán' })
  await expect(receipt).toBeVisible()
  await expect(receipt).toContainText('300.000')
  await expect.poll(() => page.evaluate(() => (window as unknown as { __printCalls: number }).__printCalls)).toBe(1)

  // Khong tran ngang man hinh o moi kich thuoc
  const overflow = await page.evaluate(() => document.documentElement.scrollWidth - window.innerWidth)
  expect(overflow).toBeLessThanOrEqual(1)

  // Ton kho tru dung trong cung giao dich (FR-POS-04)
  const stock = await api<{ content: { onHand: number; reserved: number }[] }>('GET', `/stock?skuId=${skuId}`, shop.token)
  expect(stock.content[0].onHand).toBe(8)
  expect(stock.content[0].reserved).toBe(0)
})

test('thêm vượt tồn khả dụng bị chặn và báo số còn lại (FR-POS-02)', async ({ page }) => {
  const shop = await newShop('posx')
  const barcode = `894${Date.now().toString().slice(-9)}`
  await stockedSku(shop, `E2X-${shop.runId}`, barcode, 1)
  const cashierEmail = await cashier(shop)

  await page.goto(POS_URL)
  await page.getByLabel('Email hoặc số điện thoại').fill(cashierEmail)
  await page.getByLabel('Mật khẩu').fill(PASSWORD)
  await page.getByRole('button', { name: 'Đăng nhập' }).click()

  const search = page.getByLabel('Tìm sản phẩm')
  await search.fill(barcode)
  await search.press('Enter')
  await search.fill(barcode)
  await search.press('Enter')
  await expect(page.getByRole('status').filter({ hasText: /còn 1|Còn 1|chỉ còn 1/ })).toBeVisible()
  await expect(page.getByLabel(`Số lượng E2X-${shop.runId}`)).toHaveValue('1')
})
