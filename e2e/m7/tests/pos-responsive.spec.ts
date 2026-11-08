import { test, expect } from '@playwright/test'

const viewports = [
  { name: 'Desktop', width: 1366, height: 768 },
  { name: 'Tablet', width: 768, height: 1024 },
  { name: 'Mobile', width: 390, height: 844 },
]

for (const viewport of viewports) {
  test(`M7 POS - responsive ${viewport.name}`, async ({ page }) => {
    await page.setViewportSize({
      width: viewport.width,
      height: viewport.height,
    })

    await page.goto('/')
    await expect(page.locator('body')).toBeVisible()

    const overflowElements = await page.evaluate(() => {
      return Array.from(document.querySelectorAll('body *'))
        .map((element) => {
          const rect = element.getBoundingClientRect()

          return {
            tag: element.tagName,
            className: typeof element.className === 'string'
              ? element.className
              : '',
            width: Math.round(rect.width),
            left: Math.round(rect.left),
            right: Math.round(rect.right),
          }
        })
        .filter((element) =>
          element.right > document.documentElement.clientWidth ||
          element.left < 0
        )
    })

    console.log(`M7 ${viewport.name} OVERFLOW:`, overflowElements)

    const dimensions = await page.evaluate(() => ({
      viewportWidth: document.documentElement.clientWidth,
      pageWidth: document.documentElement.scrollWidth,
    }))

    expect(
      dimensions.pageWidth,
      `Horizontal overflow at ${viewport.name}: ${JSON.stringify(dimensions)}`,
    ).toBeLessThanOrEqual(dimensions.viewportWidth)
  })
}