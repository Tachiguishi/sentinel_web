import { expect, test, type Page } from '@playwright/test'

async function mockStation(page: Page, failCatalog = false) {
  const devices = [
    { name: 'IED1', description: '线路A保护', category: 1, categoryLabel: '保护' },
    { name: 'IED2', description: '线路A测控', category: 2, categoryLabel: '测控' },
    { name: 'IED3', description: '特高压保护', category: 0, categoryLabel: '未知' },
    { name: 'IED4', description: '35kV测控', category: 2, categoryLabel: '测控' },
    { name: 'IED5', description: '500kV保护', category: 1, categoryLabel: '保护' },
    { name: 'IED6', description: '线路B保护', category: 1, categoryLabel: '保护' },
    { name: 'IED7', description: '110kV测控', category: 2, categoryLabel: '测控' },
    { name: 'IED8', description: '电压未填写装置', category: 1, categoryLabel: '保护' },
    { name: 'IED9', description: '未分配装置', category: 1, categoryLabel: '保护' },
    { name: 'IED10', description: '失效间隔装置', category: 1, categoryLabel: '保护' },
  ]
  const bays = [
    { code: 'Bay35', name: 'Line35', description: '35kV线路间隔', voltage: 35 },
    { code: 'Bay220A', name: 'LineA', description: '线路A间隔', voltage: 220 },
    { code: 'Bay220B', name: 'LineB', description: null, voltage: 220 },
    { code: 'Bay500', name: 'Line500', description: '500kV线路间隔', voltage: 500 },
    { code: 'Bay1000', name: 'Line1000', description: '特高压间隔', voltage: 1000 },
    { code: 'Bay110', name: 'Line110', description: '110kV线路间隔', voltage: 110 },
    { code: 'Bay0', name: 'Line0', description: '电压未填写间隔', voltage: null },
    { code: 'Bay750', name: 'Empty', description: '无装置间隔', voltage: 750 },
  ]
  const assignments: Record<string, string | null> = {
    IED1: 'Bay220A',
    IED2: 'Bay220A',
    IED3: 'Bay1000',
    IED4: 'Bay35',
    IED5: 'Bay500',
    IED6: 'Bay220B',
    IED7: 'Bay110',
    IED8: 'Bay0',
    IED9: null,
    IED10: 'RemovedBay',
  }
  const datasetRequests: string[] = []
  const batchWrites: string[][] = []
  let catalogAttempts = 0
  await page.route('**/api/stations/**', async (route) => {
    const url = new URL(route.request().url())
    switch (url.pathname) {
      case '/api/stations/detail':
        await route.fulfill({
          json: { name: 'StationA', description: '示例变电站', scdUpdatedAt: null },
        })
        break
      case '/api/stations/bays':
        catalogAttempts++
        await route.fulfill(failCatalog && catalogAttempts === 1 ? { status: 503 } : { json: bays })
        break
      case '/api/stations/scd/devices':
        await route.fulfill({ json: devices })
        break
      case '/api/stations/scd/bay-mappings':
        await route.fulfill({
          json: devices.map((device) => ({ name: device.name, bayCode: assignments[device.name] })),
        })
        break
      case '/api/stations/scd/datasets':
        datasetRequests.push(url.searchParams.get('device') ?? '')
        await route.fulfill({ json: [] })
        break
      case '/api/stations/scd/device-categories':
        if (route.request().method() === 'PUT') {
          batchWrites.push(route.request().postDataJSON() as string[])
          await route.fulfill({ json: { value: 2, label: '测控' } })
        } else {
          await route.fulfill({
            json: [
              { value: 1, label: '保护' },
              { value: 2, label: '测控' },
            ],
          })
        }
        break
      default:
        await route.fulfill({ status: 404 })
    }
  })
  await page.goto('/stations/StationA')
  return { datasetRequests, batchWrites }
}

test('groups unmatched devices first and sorts voltage levels numerically in descending order', async ({
  page,
}, testInfo) => {
  const { datasetRequests } = await mockStation(page)
  const tree = page.getByRole('list', { name: '装置', exact: true })
  await expect(tree.locator('details[data-voltage]')).toHaveCount(7)
  expect(
    await tree
      .locator('details[data-voltage]')
      .evaluateAll((nodes) => nodes.map((node) => node.getAttribute('data-voltage'))),
  ).toEqual(['0', '1000', '500', '220', '110', '35', '0'])
  const unknown = tree.locator('details[data-unmatched="true"]')
  await expect(unknown.locator('details > summary')).toHaveText('未知间隔 2')
  await expect(unknown.getByRole('button')).toHaveCount(2)
  await expect(unknown.getByRole('button', { name: /IED9/ })).toBeVisible()
  await expect(unknown.getByRole('button', { name: /IED10/ })).toBeVisible()
  const voltage220 = tree.locator('details[data-voltage="220"]')
  await expect(voltage220.locator('details[data-bay-code]')).toHaveCount(2)
  await expect(
    voltage220.locator('details[data-bay-code="Bay220A"]').getByRole('button'),
  ).toHaveCount(2)
  await expect(tree.locator('details[data-bay-code="Bay220B"] > summary')).toHaveText('LineB 1')
  await expect(tree.getByText('无装置间隔')).toHaveCount(0)
  const device = voltage220.getByRole('button', { name: /线路A保护/ })
  await device.click()
  await expect(device).toHaveAttribute('aria-pressed', 'true')
  await expect(page.getByText('该装置没有数据集。')).toBeVisible()
  expect(datasetRequests).toEqual(['IED1'])
  await page.screenshot({ path: testInfo.outputPath('station-device-tree.png'), fullPage: true })
})

test('supports native keyboard expansion and opens matching search results', async ({ page }) => {
  await mockStation(page)
  const tree = page.getByRole('list', { name: '装置', exact: true })
  const voltage = tree.locator('details[data-voltage="220"]')
  await expect(voltage).toHaveAttribute('open', '')
  await voltage.locator(':scope > summary').focus()
  await page.keyboard.press('Space')
  await expect(voltage).not.toHaveAttribute('open')
  await expect(tree.getByRole('button', { name: /线路A保护/ })).toBeHidden()
  await page.getByRole('searchbox', { name: '筛选装置', exact: true }).fill('线路A保护')
  await expect(voltage).toHaveAttribute('open', '')
  await expect(tree.getByRole('button', { name: /线路A保护/ })).toBeVisible()
  await expect(tree.locator('details[data-voltage]')).toHaveCount(1)
  await tree.locator('details[data-bay-code="Bay220A"] > summary').click()
  await expect(tree.getByRole('button', { name: /线路A保护/ })).toBeHidden()
  await page.getByRole('searchbox', { name: '筛选装置', exact: true }).fill('IED1')
  await expect(tree.getByRole('button', { name: /线路A保护/ })).toBeVisible()
  await page.getByRole('searchbox', { name: '筛选装置', exact: true }).fill('没有这个装置')
  await expect(page.getByText('没有匹配的装置。')).toBeVisible()
})

test('keeps batch category editing across voltage and bay groups on narrow screens', async ({
  page,
}) => {
  await page.setViewportSize({ width: 390, height: 844 })
  const { batchWrites } = await mockStation(page)
  await page.getByRole('button', { name: '批量修改类型', exact: true }).click()
  await page.getByRole('checkbox', { name: '选择 IED1', exact: true }).check()
  await page.getByRole('checkbox', { name: '选择 IED9', exact: true }).check()
  await page.getByRole('button', { name: '修改所选类型', exact: true }).click()
  const dialog = page.getByRole('dialog')
  await dialog.getByLabel('装置类型', { exact: true }).selectOption('2')
  await dialog.getByRole('button', { name: '保存', exact: true }).click()
  await expect(dialog).toBeHidden()
  expect(batchWrites).toEqual([['IED1', 'IED9']])
  const tree = page.getByRole('list', { name: '装置', exact: true })
  await expect(
    tree.locator('[data-bay-code="Bay220A"]').getByRole('button', { name: /线路A保护/ }),
  ).toContainText('测控')
  await expect(
    tree.locator('[data-unmatched="true"]').getByRole('button', { name: /IED9/ }),
  ).toContainText('测控')
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(
    true,
  )
})

test('shows a directory error and retries without classifying a failed query as unmatched', async ({
  page,
}) => {
  await mockStation(page, true)
  await expect(page.getByRole('alert')).toHaveText('无法读取装置列表或间隔信息。')
  await expect(page.getByRole('list', { name: '装置', exact: true })).toHaveCount(0)
  await page.getByRole('button', { name: '重试', exact: true }).click()
  await expect(
    page.getByRole('list', { name: '装置', exact: true }).locator('details[data-voltage]'),
  ).toHaveCount(7)
})
