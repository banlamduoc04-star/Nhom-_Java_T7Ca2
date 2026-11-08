import { test, expect } from '@playwright/test'

test('M7 POS - complete CASH checkout and display receipt', async ({ page }) => {
  const email = process.env.M7_POS_EMAIL
  const password = process.env.M7_POS_PASSWORD

  if (!email || !password) {
    throw new Error('Missing M7_POS_EMAIL or M7_POS_PASSWORD')
  }

  await page.goto('/')
  await page.locator('input[autocomplete="username"]').fill(email)
  await page.locator('input[autocomplete="current-password"]').fill(password)
  await page.locator('form.login button[type="submit"]').click()

  await expect(page.locator('.cart')).toBeVisible()

  const search = page.locator('input[aria-label="Tìm sản phẩm"]')
  await search.fill('8930000000132')
  await search.press('Enter')

  await expect(page.locator('.cart')).toContainText('JEAN-L')

  await page.locator('.cart .actions button.primary').click()

  const dialog = page.getByRole('dialog', {
    name: 'Thanh toán',
    exact: true,
  })

  await expect(dialog).toBeVisible()

  const confirm = dialog.locator('button.primary')
  await expect(confirm).toBeEnabled()
  await confirm.click()

  await expect(dialog).toBeHidden()

  await expect(
    page.getByRole('dialog', {
      name: 'Hóa đơn thanh toán',
      exact: true,
    }),
  ).toBeVisible()

  await expect(page.locator('body')).toContainText('JEAN-L')
  await expect(page.locator('body')).toContainText('200.000')
})