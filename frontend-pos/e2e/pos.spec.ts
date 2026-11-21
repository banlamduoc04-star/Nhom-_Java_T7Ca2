import { test, expect, type Page } from '@playwright/test'

async function loginMock(page: Page) {
  await page.goto('/')
  await page.getByRole('button', { name: 'Đăng nhập' }).click()
  await expect(page.getByLabel('Tìm sản phẩm')).toBeVisible()
}

async function searchProduct(page: Page, keyword: string) {
  await page.getByLabel('Tìm sản phẩm').fill(keyword)
  await page.getByRole('button', { name: 'Tìm', exact: true }).click()
}

async function addPoloS(page: Page) {
  await searchProduct(page, 'Ao Polo Nam')
  await page.getByRole('button', { name: /POLO-S/ }).click()
  await expect(page.locator('.cart-line')).toHaveCount(1)
}

async function trackPrint(page: Page) {
  await page.addInitScript(() => {
    Object.defineProperty(window, '__printCount', { value: 0, writable: true })
    window.print = () => {
      ;(window as Window & { __printCount: number }).__printCount += 1
    }
  })
}

test.describe('OISM POS - E2E Mock', () => {
  test('01 - Đăng nhập, tìm sản phẩm và cập nhật giỏ hàng', async ({ page }) => {
    await loginMock(page)
    await addPoloS(page)

    const quantity = page.getByRole('spinbutton', { name: 'Số lượng POLO-S' })
    await expect(quantity).toHaveValue('1')
    await expect(page.locator('.cart .total strong')).toContainText('150.000')

    await page.getByRole('button', { name: 'Tăng' }).click()
    await expect(quantity).toHaveValue('2')
    await expect(page.locator('.cart .total strong')).toContainText('300.000')

    await page.getByRole('button', { name: 'Xóa POLO-S' }).click()
    await expect(page.locator('.cart-line')).toHaveCount(0)
  })

  test('02 - Máy quét USB nhập mã vạch và Enter để thêm hàng', async ({ page }) => {
    await loginMock(page)
    const search = page.getByLabel('Tìm sản phẩm')
    await search.fill('8930000000012')
    await search.press('Enter')

    await expect(page.locator('.cart-line')).toHaveCount(1)
    await expect(page.locator('.cart-info')).toContainText('POLO-S')
    await expect(page.locator('.cart .total strong')).toContainText('150.000')
  })

  test('03 - Không cho phép giỏ hàng vượt tồn khả dụng', async ({ page }) => {
    await loginMock(page)

    // POLO-M chỉ có một kết quả nên hệ thống tự thêm vào giỏ
    await searchProduct(page, 'POLO-M')

    const cartLine = page.locator('.cart-line')
    await expect(cartLine).toHaveCount(1)

    const quantity = page.getByRole('spinbutton', { name: 'Số lượng POLO-M' })
    await expect(quantity).toHaveValue('1')

    // Nhập số lượng vượt tồn khả dụng (3)
    await quantity.fill('4')

    await expect(quantity).toHaveValue('1')
    await expect(page.getByRole('alert')).toContainText('chỉ còn 3')
  })

  test('04 - Phím tắt F2, F9 và Esc hoạt động', async ({ page }) => {
    await loginMock(page)
    await addPoloS(page)

    await page.keyboard.press('F2')
    await expect(page.getByLabel('Tìm sản phẩm')).toBeFocused()

    await page.keyboard.press('F9')
    const dialog = page.getByRole('dialog', { name: 'Thanh toán đơn hàng' })
    await expect(dialog).toBeVisible()

    await page.keyboard.press('Escape')
    await expect(dialog).toBeHidden()
  })

  test('05 - Tiền mặt tính tiền thối và hoàn tất đơn demo', async ({ page }) => {
    await trackPrint(page)
    await loginMock(page)
    await addPoloS(page)
    await page.keyboard.press('F9')

    await page.locator('#cash-given').fill('200000')
    await expect(page.locator('.change')).toContainText('50.000')
    await page.getByRole('button', { name: 'Xác nhận & In bill (F9)' }).click()

    const receipt = page.getByRole('dialog', { name: 'Hóa đơn thanh toán' })
    await expect(receipt).toBeVisible()
    await expect(receipt).toContainText('150.000')
    await expect(receipt).toContainText('50.000')
    await expect.poll(() => page.evaluate(() => (window as Window & { __printCount: number }).__printCount)).toBe(1)
  })

  test('06 - QR được sinh và cảnh báo chưa tích hợp ngân hàng', async ({ page }) => {
    await loginMock(page)
    await addPoloS(page)
    await page.keyboard.press('F9')

    await page.getByRole('button', { name: /Chuyển khoản QR/ }).click()
    await expect(page.getByText('Mã QR tham chiếu đơn hàng')).toBeVisible()
    await expect(page.getByText(/chưa tích hợp ngân hàng\/VietQR/)).toBeVisible()
    await expect(page.getByRole('img', { name: 'Mã QR thanh toán' })).toHaveAttribute('src', /^data:image\/png/)
  })

  test('07 - Có thể mở và in lại hóa đơn gần nhất', async ({ page }) => {
    await trackPrint(page)
    await loginMock(page)
    await addPoloS(page)
    await page.keyboard.press('F9')
    await page.getByRole('button', { name: 'Xác nhận & In bill (F9)' }).click()

    await expect(page.getByRole('dialog', { name: 'Hóa đơn thanh toán' })).toBeVisible()
    await page.getByRole('button', { name: 'Đơn mới (Esc)' }).click()
    await expect(page.getByRole('dialog', { name: 'Hóa đơn thanh toán' })).toBeHidden()

    await page.getByRole('button', { name: 'In lại hóa đơn' }).click()
    await expect(page.getByRole('dialog', { name: 'Hóa đơn thanh toán' })).toBeVisible()
    await page.getByRole('button', { name: 'In hóa đơn' }).click()
    await expect.poll(() => page.evaluate(() => (window as Window & { __printCount: number }).__printCount)).toBeGreaterThanOrEqual(2)
  })
})


test('08 - Responsive tren desktop, tablet va mobile', async ({ page }) => {
  await page.setViewportSize({ width: 1280, height: 800 })
  await loginMock(page)

  for (const viewport of [
    { width: 1280, height: 800 },
    { width: 768, height: 1024 },
    { width: 390, height: 844 },
  ]) {
    await page.setViewportSize(viewport)

    await expect(page.getByLabel('Tìm sản phẩm')).toBeVisible()
    await expect(page.locator('.layout')).toBeVisible()

    const overflow = await page.evaluate(
      () => document.documentElement.scrollWidth > window.innerWidth,
    )

    expect(overflow, `Tràn chiều ngang tại ${viewport.width}px`).toBe(false)
  }
})

test('09 - PWA có manifest và đăng ký Service Worker', async ({ page }) => {
  await page.goto('/')

  const manifest = await page.evaluate(async () => {
    const response = await fetch('/manifest.webmanifest')
    return response.json()
  })

  expect(manifest.display).toBe('standalone')
  expect(manifest.icons.length).toBeGreaterThan(0)

  await expect.poll(async () => {
    return page.evaluate(async () => {
      return Boolean(await navigator.serviceWorker.getRegistration('/'))
    })
  }).toBe(true)
})

test('10 - Hóa đơn có chiều rộng 80mm khi in', async ({ page }) => {
  await loginMock(page)
  await addPoloS(page)
  await page.keyboard.press('F9')

  await page.getByRole('button', { name: /Xác nhận.*In bill/ }).click()

  const receipt = page.locator('.receipt-paper')
  await expect(receipt).toBeVisible()

  await page.emulateMedia({ media: 'print' })

  const width = await receipt.evaluate(
    (element) => getComputedStyle(element).width,
  )

  expect(parseFloat(width)).toBeCloseTo(302.36, 0)
})
