import { expect, test } from '@playwright/test'

test('requests and displays the next page of station records', async ({ page }) => {
  await page.route('**/api/stations?*', async (route) => {
    const currentPage = Number(new URL(route.request().url()).searchParams.get('page'))
    await route.fulfill({
      json: {
        rows: [
          {
            name: currentPage === 1 ? '甲站' : '乙站',
            description: null,
            ipAddress: '192.0.2.10',
            instantNo: 3,
            createAt: '2026-11-10T10:00:00',
          },
        ],
        page: currentPage,
        size: 20,
        total: 21,
        totalPages: 2,
      },
    })
  })

  await page.goto('/')
  await expect(page.getByRole('heading', { level: 1 })).toHaveText('站点列表')
  await expect(page.getByRole('rowheader', { name: '甲站' })).toBeVisible()
  await expect(page.getByRole('button', { name: '上一页' })).toBeDisabled()
  await page.getByRole('button', { name: '下一页' }).click()
  await expect(page.getByRole('rowheader', { name: '乙站' })).toBeVisible()
  await expect(page.getByRole('button', { name: '下一页' })).toBeDisabled()
})
