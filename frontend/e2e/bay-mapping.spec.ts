import { expect, test, type Page } from '@playwright/test'

async function mockStation(page: Page, failFirstSave = false) {
  const devices = [
    { name: 'IED1', description: '线路保护装置', bayCode: null as string | null },
    { name: 'IED2', description: '线路测控装置', bayCode: null as string | null },
    { name: 'IED3', description: '母线保护装置', bayCode: 'Bay002' as string | null },
  ]
  let attempts = 0
  const writes: { device: string; bayCode: string | null }[][] = []
  await page.route('**/api/stations/**', async (route) => {
    const url = new URL(route.request().url())
    if (url.pathname.endsWith('/detail')) {
      await route.fulfill({ json: { name: 'StationA', description: '示例变电站' } })
    } else if (url.pathname.endsWith('/bays')) {
      await route.fulfill({
        json: [
          { code: 'Bay001', name: 'Line1', description: '220kV 线路一间隔', voltage: 220 },
          { code: 'Bay002', name: 'Bus1', description: '110kV 母线间隔', voltage: 110 },
        ],
      })
    } else if (route.request().method() === 'PUT') {
      attempts++
      if (failFirstSave && attempts === 1) {
        await route.fulfill({ status: 500 })
        return
      }
      const assignments = route.request().postDataJSON() as {
        device: string
        bayCode: string | null
      }[]
      writes.push(assignments)
      for (const assignment of assignments) {
        const device = devices.find((device) => device.name === assignment.device)
        if (device) device.bayCode = assignment.bayCode
      }
      await route.fulfill({ status: 204 })
    } else {
      await route.fulfill({ json: devices })
    }
  })
  await page.goto('/stations/StationA/bay-mapping')
  await expect(page.getByRole('heading', { name: '装置与间隔配置' })).toBeVisible()
  await expect(page.getByRole('checkbox', { name: '选择 IED1', exact: true })).toBeVisible()
  return writes
}

test('drags multiple devices, saves and restores assignments after reload', async ({
  page,
}, testInfo) => {
  const writes = await mockStation(page)
  await expect(page.getByRole('checkbox', { name: '选择 IED3', exact: true })).toHaveCount(0)
  await page.getByRole('checkbox', { name: '选择 IED1', exact: true }).check()
  await page.getByRole('checkbox', { name: '选择 IED2', exact: true }).check()
  const target = page
    .getByRole('listitem')
    .filter({ has: page.getByRole('heading', { name: '220kV 线路一间隔' }) })
  await page.getByText('线路保护装置', { exact: true }).dragTo(target)
  await expect(target).toContainText('已关联 2 个装置')
  await expect(page.getByRole('checkbox', { name: '选择 IED1', exact: true })).toHaveCount(0)
  await expect(page.getByRole('checkbox', { name: '选择 IED2', exact: true })).toHaveCount(0)
  await expect(page.getByText('所有装置均已分配间隔。')).toBeVisible()
  await expect(page.getByText('2 项未保存')).toBeVisible()
  await page.getByRole('button', { name: '保存配置', exact: true }).click()
  await expect(page.getByRole('status')).toHaveText('已保存 2 个装置的间隔配置。')
  expect(writes).toEqual([
    [
      { device: 'IED1', bayCode: 'Bay001' },
      { device: 'IED2', bayCode: 'Bay001' },
    ],
  ])
  await page.reload()
  await expect(target).toContainText('已关联 2 个装置')
  await expect(page.getByText('0 项未保存')).toBeVisible()
  await page.screenshot({ path: testInfo.outputPath('bay-mapping-desktop.png'), fullPage: true })
})

test('drags card whitespace and device names without selecting the devices first', async ({
  page,
}) => {
  await mockStation(page)
  const target = page
    .getByRole('listitem')
    .filter({ has: page.getByRole('heading', { name: '220kV 线路一间隔' }) })
  const card = page
    .locator('[draggable="true"]')
    .filter({ has: page.getByRole('checkbox', { name: '选择 IED1', exact: true }) })
  await card.dragTo(target, { sourcePosition: { x: 3, y: 3 } })
  await expect(target).toContainText('已关联 1 个装置')
  await expect(page.getByRole('checkbox', { name: '选择 IED2', exact: true })).not.toBeChecked()
  await page.getByText('IED2', { exact: true }).dragTo(target)
  await expect(target).toContainText('已关联 2 个装置')
  await expect(page.getByText('所有装置均已分配间隔。')).toBeVisible()
})

test('supports keyboard assignment, reset and retry after a failed save', async ({ page }) => {
  const writes = await mockStation(page, true)
  const checkbox = page.getByRole('checkbox', { name: '选择 IED1', exact: true })
  await checkbox.focus()
  await page.keyboard.press('Space')
  const assign = page.getByRole('button', {
    name: '分配所选装置到 220kV 线路一间隔 (Bay001)',
    exact: true,
  })
  await assign.focus()
  await page.keyboard.press('Enter')
  await expect(page.getByText('1 项未保存')).toBeVisible()
  await page.getByRole('button', { name: '撤销修改', exact: true }).click()
  await expect(page.getByText('0 项未保存')).toBeVisible()
  await expect(checkbox).toBeVisible()
  await checkbox.check()
  await assign.click()
  await page.getByRole('button', { name: '保存配置', exact: true }).click()
  await expect(page.getByRole('alert')).toContainText('未保存的修改仍保留')
  await expect(page.getByText('1 项未保存')).toBeVisible()
  await page.getByRole('button', { name: '保存配置', exact: true }).click()
  await expect(page.getByRole('status')).toContainText('已保存 1 个')
  expect(writes).toHaveLength(1)
  await page.getByRole('button', { name: '移出 IED1', exact: true }).click()
  await expect(checkbox).toBeVisible()
  await page.getByRole('button', { name: '保存配置', exact: true }).click()
  await expect(page.getByRole('status')).toContainText('已保存 1 个')
  expect(writes[1]).toEqual([{ device: 'IED1', bayCode: null }])
})

test('supports touch-sized screens and protects unsaved changes', async ({ page }, testInfo) => {
  await page.setViewportSize({ width: 390, height: 844 })
  await mockStation(page)
  await page.getByRole('checkbox', { name: '选择 IED1', exact: true }).check()
  await page
    .getByRole('button', { name: '分配所选装置到 220kV 线路一间隔 (Bay001)', exact: true })
    .click()
  await expect(page.getByText('1 项未保存')).toBeVisible()
  await Promise.all([
    page.waitForEvent('dialog').then(async (dialog) => {
      expect(dialog.message()).toContain('尚未保存')
      await dialog.dismiss()
    }),
    page.getByRole('link', { name: '返回站点详情' }).click(),
  ])
  await expect(page).toHaveURL(/bay-mapping$/)
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(
    true,
  )
  await page.getByRole('button', { name: '保存配置', exact: true }).click()
  await expect(page.getByRole('status')).toContainText('已保存 1 个')
  await page.screenshot({ path: testInfo.outputPath('bay-mapping-mobile.png'), fullPage: true })
})

test('refreshes only the bay catalog while retaining filters, selection and unsaved assignments', async ({
  page,
}) => {
  await mockStation(page)
  await page.getByRole('checkbox', { name: '选择 IED1', exact: true }).check()
  await page
    .getByRole('button', { name: '分配所选装置到 220kV 线路一间隔 (Bay001)', exact: true })
    .click()
  await page.getByRole('checkbox', { name: '选择 IED2', exact: true }).check()
  const filter = page.getByRole('searchbox', { name: '筛选间隔', exact: true })
  await filter.fill('Bay001')
  const requests: string[] = []
  page.on('request', (request) => {
    requests.push(new URL(request.url()).pathname)
  })
  let release!: () => void
  const pending = new Promise<void>((resolve) => {
    release = resolve
  })
  await page.route('**/api/stations/bays?*', async (route) => {
    expect(new URL(route.request().url()).searchParams.get('name')).toBe('StationA')
    await pending
    await route.fulfill({
      json: [
        { code: 'Bay001', name: 'Line1', description: '线路一间隔更新', voltage: 220 },
        { code: 'Bay002', name: 'Bus1', description: '110kV 母线间隔', voltage: 110 },
        { code: 'Bay003', name: 'NewLine', description: '新增间隔', voltage: 500 },
      ],
    })
  })
  const refresh = page.getByRole('button', { name: '刷新间隔', exact: true })
  await refresh.click()
  await expect(refresh).toBeDisabled()
  await expect(page.getByRole('progressbar', { name: '正在刷新间隔目录' })).toBeVisible()
  release()
  await expect(page.getByRole('status')).toHaveText('间隔目录已刷新，共 3 个间隔。')
  await expect(refresh).toBeEnabled()
  await expect(filter).toHaveValue('Bay001')
  await expect(page.getByRole('checkbox', { name: '选择 IED2', exact: true })).toBeChecked()
  await expect(page.getByText('1 项未保存')).toBeVisible()
  await expect(page.getByRole('heading', { name: '线路一间隔更新', exact: true })).toBeVisible()
  await expect(page.getByRole('button', { name: '移出 IED1', exact: true })).toBeVisible()
  expect(requests).toEqual(['/api/stations/bays'])
  await filter.fill('')
  await expect(page.getByRole('heading', { name: '新增间隔', exact: true })).toBeVisible()
})

test('retains the old catalog after refresh failure and recovers devices from deleted bays', async ({
  page,
}) => {
  await mockStation(page)
  let attempts = 0
  await page.route('**/api/stations/bays?*', async (route) => {
    attempts++
    await route.fulfill(
      attempts === 1
        ? { status: 503 }
        : {
            json: [
              { code: 'Bay001', name: 'Line1', description: '220kV 线路一间隔', voltage: 220 },
            ],
          },
    )
  })
  const refresh = page.getByRole('button', { name: '刷新间隔', exact: true })
  await refresh.click()
  await expect(page.getByRole('alert')).toHaveText('刷新间隔失败，已保留当前目录，请重试。')
  await expect(page.getByRole('heading', { name: '110kV 母线间隔', exact: true })).toBeVisible()
  await expect(refresh).toBeEnabled()
  await refresh.click()
  await expect(page.getByRole('status')).toHaveText('间隔目录已刷新，共 1 个间隔。')
  await expect(page.getByRole('alert')).toHaveCount(0)
  await page.getByRole('button', { name: '解除关联 IED3', exact: true }).click()
  await expect(page.getByRole('checkbox', { name: '选择 IED3', exact: true })).toBeVisible()
  await expect(page.getByText('1 项未保存')).toBeVisible()
})
