import { test, expect } from '@playwright/test'

test('M7 POS - cashier login successfully', async ({ page }) => {
  const email = process.env.M7_POS_EMAIL
  const password = process.env.M7_POS_PASSWORD

  test.skip(!email || !password, 'Set M7_POS_EMAIL and M7_POS_PASSWORD locally')

  await page.goto('/')

  await page.locator('input[autocomplete="username"]').fill(email!)
  await page.locator('input[autocomplete="current-password"]').fill(password!)
  await page.locator('form.login button[type="submit"]').click()

  await expect(page.locator('input[aria-label="Tìm sản phẩm"]')).toBeVisible()
  await expect(page.locator('.cart')).toBeVisible()
})
