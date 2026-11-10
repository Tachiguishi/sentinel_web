<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'

interface Station {
  name: string
  description: string | null
  ipAddress: string | null
  instantNo: number | null
  createAt: string
}

interface StationPage {
  rows: Station[]
  page: number
  size: number
  total: number
  totalPages: number
}

const result = ref<StationPage | null>(null)
const pageSize = ref(20)
const searchInput = ref('')
const keyword = ref('')
const requestedPage = ref(1)
const loading = ref(true)
const error = ref(false)
const rangeStart = computed(() =>
  result.value?.rows.length ? (result.value.page - 1) * result.value.size + 1 : 0,
)
const rangeEnd = computed(() => rangeStart.value + (result.value?.rows.length ?? 0) - 1)

async function loadPage(page: number) {
  requestedPage.value = page
  loading.value = true
  error.value = false

  try {
    const params = new URLSearchParams({ page: String(page), size: String(pageSize.value) })
    if (keyword.value) params.set('keyword', keyword.value)
    const response = await fetch(`/api/stations?${params}`)
    if (!response.ok) throw new Error(`HTTP ${response.status}`)
    result.value = (await response.json()) as StationPage
  } catch {
    error.value = true
  } finally {
    loading.value = false
  }
}

function applySearch() {
  keyword.value = searchInput.value.trim()
  void loadPage(1)
}

function clearSearch() {
  searchInput.value = ''
  applySearch()
}

function changePageSize(event: Event) {
  pageSize.value = Number((event.target as HTMLSelectElement).value)
  void loadPage(1)
}

onMounted(() => void loadPage(1))
</script>

<template>
  <div class="min-h-screen bg-[#f2f5f4] text-slate-900">
    <header class="border-b border-slate-800 bg-[#101e28] text-white">
      <div class="mx-auto flex max-w-6xl items-center gap-3 px-5 py-4 sm:px-8">
        <div
          class="grid size-9 place-items-center rounded-lg border border-teal-300/35 bg-teal-300/10 text-teal-200"
        >
          <svg
            aria-hidden="true"
            viewBox="0 0 24 24"
            class="size-5"
            fill="none"
            stroke="currentColor"
            stroke-width="1.6"
            stroke-linecap="round"
            stroke-linejoin="round"
          >
            <path d="M12 3 4.5 7v5c0 4 3.3 7 7.5 9 4.2-2 7.5-5 7.5-9V7L12 3Z" />
            <path d="m8 12 2.5 2.5L16 9" />
          </svg>
        </div>
        <span class="text-sm font-semibold tracking-[0.16em]">SENTINEL</span>
        <span class="ml-auto text-sm text-slate-300">站点台账</span>
      </div>
    </header>

    <main class="mx-auto max-w-6xl px-5 py-9 sm:px-8 sm:py-14">
      <p class="mb-3 text-xs font-semibold tracking-[0.2em] text-teal-800">站点管理</p>
      <h1 class="text-3xl font-bold tracking-tight sm:text-4xl">站点列表</h1>
      <p class="mt-3 text-sm leading-6 text-slate-600">
        查看 Sentinel 站点的名称、说明、IP 地址、实例编号和创建时间。
      </p>

      <section
        aria-labelledby="directory-title"
        class="mt-8 rounded-2xl border border-slate-200 bg-white shadow-sm"
      >
        <div class="flex flex-wrap items-center justify-between gap-3 px-5 py-5 sm:px-6">
          <div>
            <h2 id="directory-title" class="font-semibold">站点记录</h2>
            <p class="mt-1 text-xs text-slate-600">按创建时间从新到旧排序</p>
          </div>
          <label class="flex items-center gap-2 text-sm text-slate-700">
            每页
            <select
              :value="pageSize"
              :disabled="loading"
              class="rounded-lg border border-slate-300 bg-white px-2.5 py-1.5 tabular-nums focus-visible:outline-2 focus-visible:outline-teal-700 disabled:opacity-50"
              @change="changePageSize"
            >
              <option :value="10">10 条</option>
              <option :value="20">20 条</option>
              <option :value="50">50 条</option>
            </select>
          </label>
        </div>

        <form
          role="search"
          class="flex flex-wrap items-end gap-2 px-5 pb-5 sm:px-6"
          @submit.prevent="applySearch"
        >
          <div class="min-w-40 flex-1 sm:max-w-sm">
            <label for="station-keyword" class="mb-1 block text-sm font-medium text-slate-700">
              站名或描述
            </label>
            <input
              id="station-keyword"
              v-model="searchInput"
              name="keyword"
              type="search"
              enterkeyhint="search"
              placeholder="输入关键词"
              class="min-h-12 w-full rounded-lg border border-slate-400 px-3 focus-visible:outline-2 focus-visible:outline-teal-700"
            />
          </div>
          <button
            type="submit"
            :disabled="loading"
            class="min-h-12 rounded-lg bg-teal-800 px-4 text-sm font-medium text-white hover:bg-teal-900 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-teal-700 disabled:opacity-50"
          >
            搜索
          </button>
          <button
            type="button"
            :disabled="loading || (!keyword && !searchInput)"
            class="min-h-12 rounded-lg border border-slate-300 px-4 text-sm font-medium hover:bg-slate-50 focus-visible:outline-2 focus-visible:outline-teal-700 disabled:opacity-50"
            @click="clearSearch"
          >
            清除
          </button>
        </form>

        <div
          role="region"
          aria-labelledby="directory-title"
          :aria-busy="loading"
          tabindex="0"
          class="max-h-[65vh] overflow-auto border-y border-slate-200 focus-visible:outline-2 focus-visible:-outline-offset-2 focus-visible:outline-teal-700"
        >
          <table
            class="w-full border-separate border-spacing-0 text-left text-sm"
            :class="{ 'min-w-[44rem]': !loading && !error && result?.rows.length }"
          >
            <thead class="sticky top-0 z-10 bg-[#edf3f1] text-slate-700">
              <tr>
                <th scope="col" class="sticky left-0 bg-[#edf3f1] px-3 py-3 sm:px-6">站点名</th>
                <th scope="col" class="px-3 py-3 sm:px-5">站点描述</th>
                <th scope="col" class="px-3 py-3 whitespace-nowrap sm:px-5">IP 地址</th>
                <th scope="col" class="px-3 py-3 text-right whitespace-nowrap sm:px-5">实例号</th>
                <th scope="col" aria-sort="descending" class="px-3 py-3 whitespace-nowrap sm:px-6">
                  创建时间
                </th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="loading">
                <td colspan="5" class="px-5 py-16 text-center text-slate-600">正在加载站点记录…</td>
              </tr>
              <tr v-else-if="error">
                <td colspan="5" class="px-5 py-12 text-center">
                  <p role="alert" class="text-red-800">无法加载站点记录，请重试。</p>
                  <button
                    type="button"
                    class="mt-3 rounded-lg border border-slate-300 px-4 py-2 font-medium text-teal-800 hover:bg-teal-50 focus-visible:outline-2 focus-visible:outline-teal-700"
                    @click="loadPage(requestedPage)"
                  >
                    重试
                  </button>
                </td>
              </tr>
              <tr v-else-if="!result?.rows.length">
                <td colspan="5" class="px-5 py-16 text-center text-slate-600">
                  {{ keyword ? '没有匹配的站点' : '暂无站点记录' }}
                </td>
              </tr>
              <template v-else>
                <tr v-for="station in result.rows" :key="station.name" class="group">
                  <th
                    scope="row"
                    class="sticky left-0 border-b border-slate-100 bg-white px-5 py-4 font-semibold whitespace-nowrap group-hover:bg-slate-50 sm:px-6"
                  >
                    {{ station.name }}
                  </th>
                  <td
                    class="border-b border-slate-100 px-5 py-4 text-slate-700 group-hover:bg-slate-50"
                  >
                    {{ station.description ?? '—' }}
                  </td>
                  <td
                    class="border-b border-slate-100 px-5 py-4 font-mono text-slate-700 group-hover:bg-slate-50"
                  >
                    {{ station.ipAddress ?? '—' }}
                  </td>
                  <td
                    class="border-b border-slate-100 px-5 py-4 text-right text-slate-700 tabular-nums group-hover:bg-slate-50"
                  >
                    {{ station.instantNo ?? '—' }}
                  </td>
                  <td
                    class="border-b border-slate-100 px-5 py-4 whitespace-nowrap text-slate-700 tabular-nums group-hover:bg-slate-50 sm:px-6"
                  >
                    <time :datetime="station.createAt">{{
                      station.createAt.replace('T', ' ')
                    }}</time>
                  </td>
                </tr>
              </template>
            </tbody>
          </table>
        </div>

        <div class="flex flex-wrap items-center justify-between gap-4 px-5 py-4 sm:px-6">
          <p class="text-sm text-slate-600 tabular-nums" role="status">
            <template v-if="loading">正在加载…</template>
            <template v-else-if="error">加载失败</template>
            <template v-else-if="result?.total"
              >显示第 {{ rangeStart }}–{{ rangeEnd }} 条，共 {{ result.total }} 条</template
            >
            <template v-else>共 0 条</template>
          </p>
          <nav aria-label="站点分页" class="flex items-center gap-3 text-sm">
            <button
              type="button"
              :disabled="loading || error || !result || result.page <= 1"
              class="rounded-lg border border-slate-300 px-3 py-2 font-medium hover:bg-slate-50 focus-visible:outline-2 focus-visible:outline-teal-700 disabled:cursor-not-allowed disabled:text-slate-400"
              @click="result && loadPage(result.page - 1)"
            >
              上一页
            </button>
            <span class="text-slate-600 tabular-nums"
              >{{ result?.totalPages ? result.page : 0 }} / {{ result?.totalPages ?? 0 }}</span
            >
            <button
              type="button"
              :disabled="loading || error || !result || result.page >= result.totalPages"
              class="rounded-lg border border-slate-300 px-3 py-2 font-medium hover:bg-slate-50 focus-visible:outline-2 focus-visible:outline-teal-700 disabled:cursor-not-allowed disabled:text-slate-400"
              @click="result && loadPage(result.page + 1)"
            >
              下一页
            </button>
          </nav>
        </div>
      </section>
    </main>
  </div>
</template>
