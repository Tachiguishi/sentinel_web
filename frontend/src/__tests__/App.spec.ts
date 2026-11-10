import { afterEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import App from '../App.vue'

afterEach(() => vi.unstubAllGlobals())

describe('App', () => {
  it('shows the empty result and disables pagination', async () => {
    const fetch = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({ rows: [], page: 1, size: 20, total: 0, totalPages: 0 }),
    })
    vi.stubGlobal('fetch', fetch)

    const wrapper = mount(App, { global: { stubs: { RouterLink: true } } })
    await flushPromises()

    expect(fetch).toHaveBeenCalledWith('/api/stations?page=1&size=20')
    expect(wrapper.get('tbody').text()).toContain('暂无站点记录')
    expect(wrapper.findAll('nav button:disabled')).toHaveLength(2)
  })

  it('keeps the applied keyword when paging and clears it on request', async () => {
    const fetch = vi.fn(async (url: string) => {
      const params = new URL(url, 'http://localhost').searchParams
      const page = Number(params.get('page'))
      return {
        ok: true,
        json: async () => ({
          rows: [
            {
              name: '测试站',
              description: null,
              ipAddress: null,
              instantNo: 3,
              createAt: '2026-11-10T10:00:00',
              scdUpdatedAt: '2026-11-10T12:34:56',
            },
          ],
          page,
          size: 20,
          total: 21,
          totalPages: 2,
        }),
      }
    })
    vi.stubGlobal('fetch', fetch)

    const wrapper = mount(App, { global: { stubs: { RouterLink: true } } })
    await flushPromises()
    const search = wrapper.get('form[role=search]')
    const input = search.get<HTMLInputElement>('input[type=search]')
    await input.setValue(' East & West ')
    await search.trigger('submit')
    await flushPromises()
    expect(fetch).toHaveBeenLastCalledWith('/api/stations?page=1&size=20&keyword=East+%26+West')
    expect(wrapper.get('time').text()).toBe('2026-11-10 10:00:00')
    expect(wrapper.get('time[datetime="2026-11-10T12:34:56"]').text()).toBe('2026-11-10 12:34:56')

    await wrapper.get('nav button:last-child').trigger('click')
    await flushPromises()
    expect(fetch).toHaveBeenLastCalledWith('/api/stations?page=2&size=20&keyword=East+%26+West')

    await search.get('button[type=button]').trigger('click')
    await flushPromises()
    expect(input.element.value).toBe('')
    expect(fetch).toHaveBeenLastCalledWith('/api/stations?page=1&size=20')
  })
})
