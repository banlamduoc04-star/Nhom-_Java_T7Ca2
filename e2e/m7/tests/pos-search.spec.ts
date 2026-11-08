import { test, expect } from '@playwright/test'

async function loginAsCashier(page: import('@playwright/test').Page) {
  const email = process.env.M7_POS_EMAIL
  const password = process.env.M7_POS_PASSWORD

  if (!email || !password) {
    throw new Error('Missing M7_POS_EMAIL or M7_POS_PASSWORD')
  }

  await page.goto('/')
  await page.locator('input[autocomplete="username"]').fill(email)
  await page.locator('input[autocomplete="current-password"]').fill(password)
  await page.locator('form.login button[type="submit"]').click()
  await expect(page.locator('input[aria-label="Tìm sản phẩm"]')).toBeVisible()
}

test('M7 POS - search SKU and add to cart', async ({ page }) => {
  await loginAsCashier(page)

  const search = page.locator('input[aria-label="Tìm sản phẩm"]')
  await search.fill('JEAN-L')
  await search.press('Enter')

  await expect(page.locator('.cart')).toContainText('JEAN-L')
})

test('M7 POS - scan barcode and add to cart', async ({ page }) => {
  await loginAsCashier(page)

  const search = page.locator('input[aria-label="Tìm sản phẩm"]')
  await search.fill('8930000000132')
  await search.press('Enter')

  await expect(page.locator('.cart')).toContainText('JEAN-L')
})
