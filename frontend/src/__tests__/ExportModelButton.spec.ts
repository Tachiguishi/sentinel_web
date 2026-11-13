import { afterEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import ExportModelButton from '../components/ExportModelButton.vue'

afterEach(() => {
  vi.restoreAllMocks()
  vi.unstubAllGlobals()
  vi.useRealTimers()
})

describe('ExportModelButton', () => {
  it('downloads the selected station as XML and releases the download URL', async () => {
    vi.useFakeTimers()
    const blob = new Blob(['<substation/>'], { type: 'application/xml' })
    let finish!: (response: unknown) => void
    const fetch = vi.fn().mockImplementation(
      () =>
        new Promise((resolve) => {
          finish = resolve
        }),
    )
    vi.stubGlobal('fetch', fetch)
    const create = vi.fn().mockReturnValue('blob:model')
    const revoke = vi.fn()
    vi.stubGlobal('URL', { createObjectURL: create, revokeObjectURL: revoke })
    const downloads: { name: string; href: string }[] = []
    vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(
      function (this: HTMLAnchorElement) {
        downloads.push({ name: this.download, href: this.href })
      },
    )
    const wrapper = mount(ExportModelButton, { props: { name: '站点 A&B' } })

    await wrapper.get('button').trigger('click')
    expect(wrapper.get('button').attributes('disabled')).toBeDefined()
    finish({ ok: true, blob: async () => blob })
    await flushPromises()

    expect(fetch).toHaveBeenCalledWith('/api/stations/model?name=%E7%AB%99%E7%82%B9+A%26B', {
      signal: expect.any(AbortSignal),
    })
    expect(create).toHaveBeenCalledWith(blob)
    expect(downloads).toEqual([{ name: '站点 A&B.xml', href: 'blob:model' }])
    expect(wrapper.get('[role=status]').text()).toContain('已开始下载')
    expect(wrapper.get('button').attributes('disabled')).toBeUndefined()
    vi.runAllTimers()
    expect(revoke).toHaveBeenCalledWith('blob:model')
    wrapper.unmount()
  })

  it('shows missing SCD errors and allows retrying', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ ok: false, status: 409 }))
    const wrapper = mount(ExportModelButton, { props: { name: 'station' } })
    await wrapper.get('button').trigger('click')
    await flushPromises()
    expect(wrapper.get('[role=alert]').text()).toContain('请先更新该站点的 SCD')
    expect(wrapper.get('button').attributes('disabled')).toBeUndefined()
    wrapper.unmount()
  })

  it('cancels an export when the selected station changes', async () => {
    let finish!: (response: unknown) => void
    const fetch = vi.fn().mockImplementation(
      () =>
        new Promise((resolve) => {
          finish = resolve
        }),
    )
    vi.stubGlobal('fetch', fetch)
    const create = vi.fn()
    vi.stubGlobal('URL', { createObjectURL: create })
    const wrapper = mount(ExportModelButton, { props: { name: 'old' } })
    await wrapper.get('button').trigger('click')
    const signal = fetch.mock.calls[0]![1].signal as AbortSignal
    await wrapper.setProps({ name: 'new' })
    expect(signal.aborted).toBe(true)
    finish({ ok: true, blob: async () => new Blob(['old']) })
    await flushPromises()
    expect(create).not.toHaveBeenCalled()
    expect(wrapper.get('[role=status]').text()).toBe('')
    wrapper.unmount()
  })
})
