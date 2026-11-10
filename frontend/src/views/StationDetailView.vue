<script setup lang="ts">
import { ref, watch } from 'vue'
import { RouterLink } from 'vue-router'

const props = defineProps<{ name: string }>()

interface StationDetail {
  name: string
  description: string | null
}

interface DeviceSummary {
  name: string
  description: string | null
}

interface DataSetSummary {
  identity: string
  name: string
  description: string | null
}

interface FcdaRow {
  id: number
  index: number
  reference: string | null
  description: string | null
  fc: string | null
  cdc: string | null
}

const station = ref<StationDetail | null>(null)
const detailError = ref('')
const fileInput = ref<HTMLInputElement | null>(null)
const uploading = ref(false)
const uploadError = ref('')
const deviceCount = ref<number | null>(null)
const selectedDevice = ref<string | null>(null)
const selectedDataSet = ref<string | null>(null)

function useScdRows<T>(path: string, params: () => Record<string, string> | null) {
  const rows = ref<T[]>([])
  const loading = ref(false)
  const error = ref(false)
  const revision = ref(0)

  watch(
    [params, revision],
    async ([query], _, onCleanup) => {
      rows.value = []
      loading.value = !!query
      error.value = false
      if (!query) return

      const request = new AbortController()
      onCleanup(() => request.abort())
      try {
        const response = await fetch(`/api/stations/scd/${path}?${new URLSearchParams(query)}`, {
          signal: request.signal,
        })
        if (!response.ok) throw new Error(`HTTP ${response.status}`)
        const data = (await response.json()) as T[]
        if (!request.signal.aborted) rows.value = data
      } catch {
        if (!request.signal.aborted) error.value = true
      } finally {
        if (!request.signal.aborted) loading.value = false
      }
    },
    { immediate: true },
  )

  return { rows, loading, error, refresh: () => revision.value++ }
}

const {
  rows: devices,
  loading: devicesLoading,
  error: devicesError,
  refresh: refreshDevices,
} = useScdRows<DeviceSummary>('devices', () =>
  station.value ? { name: station.value.name } : null,
)
const {
  rows: dataSets,
  loading: dataSetsLoading,
  error: dataSetsError,
  refresh: refreshDataSets,
} = useScdRows<DataSetSummary>('datasets', () =>
  station.value && selectedDevice.value
    ? { name: station.value.name, device: selectedDevice.value }
    : null,
)
const {
  rows: fcda,
  loading: fcdaLoading,
  error: fcdaError,
  refresh: refreshFcda,
} = useScdRows<FcdaRow>('fcda', () =>
  station.value && selectedDevice.value && selectedDataSet.value
    ? { name: station.value.name, device: selectedDevice.value, dataset: selectedDataSet.value }
    : null,
)

function selectDevice(name: string) {
  if (name === selectedDevice.value) return
  selectedDataSet.value = null
  selectedDevice.value = name
}

watch(
  () => props.name,
  async (name, _, onCleanup) => {
    const request = new AbortController()
    onCleanup(() => request.abort())
    station.value = null
    selectedDevice.value = null
    selectedDataSet.value = null
    detailError.value = ''
    deviceCount.value = null
    uploadError.value = ''
    uploading.value = false

    try {
      const response = await fetch(`/api/stations/detail?${new URLSearchParams({ name })}`, {
        signal: request.signal,
      })
      if (!response.ok) {
        if (!request.signal.aborted) {
          detailError.value = response.status === 404 ? '站点不存在。' : '无法加载站点信息。'
        }
        return
      }
      const detail = (await response.json()) as StationDetail
      if (!request.signal.aborted) station.value = detail
    } catch {
      if (!request.signal.aborted) detailError.value = '无法加载站点信息。'
    }
  },
  { immediate: true },
)

async function uploadScd(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file || !station.value || uploading.value) return

  const target = station.value
  deviceCount.value = null
  uploadError.value = ''
  if (file.size > 50 * 1024 * 1024) {
    uploadError.value = 'SCD 文件不能超过 50 MB。'
    return
  }

  const form = new FormData()
  form.append('name', target.name)
  form.append('file', file)
  uploading.value = true
  try {
    const response = await fetch('/api/stations/scd', { method: 'POST', body: form })
    if (station.value !== target) return
    if (response.ok) {
      const result = (await response.json()) as { deviceCount: number }
      if (station.value === target) {
        deviceCount.value = result.deviceCount
        selectedDevice.value = null
        selectedDataSet.value = null
        refreshDevices()
      }
    } else if (response.status === 400) {
      uploadError.value = '无法读取装置，请检查 SCD 文件是否有效。'
    } else if (response.status === 413) {
      uploadError.value = 'SCD 文件不能超过 50 MB。'
    } else {
      uploadError.value = 'SCD 更新失败，请重试。'
    }
  } catch {
    if (station.value === target) uploadError.value = 'SCD 更新失败，请重试。'
  } finally {
    if (station.value === target) uploading.value = false
  }
}
</script>

<template>
  <div class="min-h-screen bg-[#f2f5f4] text-slate-900">
    <header class="border-b border-slate-800 bg-[#101e28] text-white">
      <div class="mx-auto flex max-w-[100rem] items-center justify-between gap-3 px-5 py-5 sm:px-8">
        <span class="text-sm font-semibold tracking-[0.16em]">SENTINEL</span>
        <RouterLink :to="{ name: 'stations' }" class="text-sm text-teal-200 hover:underline">
          返回站点列表
        </RouterLink>
      </div>
    </header>

    <main class="mx-auto max-w-[100rem] px-5 py-9 sm:px-8 sm:py-14">
      <p class="mb-3 text-xs font-semibold tracking-[0.2em] text-teal-800">站点详情</p>
      <h1 class="text-3xl font-bold break-words sm:text-4xl">{{ station?.name ?? name }}</h1>
      <p v-if="station?.description" class="mt-3 text-sm break-words text-slate-600">
        {{ station.description }}
      </p>
      <p v-if="detailError" role="alert" class="mt-3 text-sm text-red-800">{{ detailError }}</p>
      <p v-else-if="!station" role="status" class="mt-3 text-sm text-slate-600">
        正在加载站点信息…
      </p>

      <div class="mt-8 grid gap-5 lg:grid-cols-[minmax(15rem,1fr)_minmax(0,4fr)]">
        <section
          aria-labelledby="device-list"
          class="min-w-0 rounded-2xl border border-slate-200 bg-white p-5 shadow-sm"
        >
          <div class="flex flex-wrap items-center justify-between gap-3">
            <h2 id="device-list" class="font-semibold">装置列表</h2>
            <input ref="fileInput" type="file" accept=".scd" hidden @change="uploadScd" />
            <button
              type="button"
              :disabled="!station || uploading"
              class="rounded-lg bg-teal-800 px-4 py-2 text-sm font-medium text-white hover:bg-teal-900 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-teal-700 disabled:opacity-50"
              @click="fileInput?.click()"
            >
              {{ uploading ? '正在更新…' : '更新SCD' }}
            </button>
          </div>
          <p v-if="uploadError" role="alert" class="mt-4 text-sm text-red-800">{{ uploadError }}</p>
          <p v-else-if="uploading" role="status" class="mt-4 text-sm text-slate-600">
            正在上传并解析 SCD 文件…
          </p>
          <p v-else-if="deviceCount !== null" role="status" class="mt-4 text-sm text-teal-800">
            SCD 已更新，共导入 {{ deviceCount }} 个装置。
          </p>
          <div v-if="devicesError" class="mt-5 text-sm">
            <p role="alert" class="text-red-800">无法读取装置列表。</p>
            <button type="button" class="mt-2 text-teal-800 underline" @click="refreshDevices">
              重试
            </button>
          </div>
          <p v-else-if="devicesLoading" role="status" class="mt-5 text-sm text-slate-600">
            正在读取装置…
          </p>
          <p
            v-else-if="!devices.length"
            class="mt-5 rounded-lg bg-slate-50 px-4 py-6 text-sm text-slate-600"
          >
            {{ station ? '暂无装置，请先更新 SCD。' : '暂无装置' }}
          </p>
          <ul v-else class="mt-4 max-h-[32rem] space-y-1 overflow-auto p-1" aria-label="装置">
            <li v-for="device in devices" :key="device.name">
              <button
                type="button"
                :aria-pressed="selectedDevice === device.name"
                class="w-full rounded-lg px-3 py-2.5 text-left text-sm hover:bg-slate-50 focus-visible:outline-2 focus-visible:outline-teal-700 aria-pressed:bg-teal-50 aria-pressed:text-teal-900"
                @click="selectDevice(device.name)"
              >
                <span class="block font-semibold break-words">{{ device.name }}</span>
                <span
                  v-if="device.description"
                  class="mt-1 block text-xs break-words text-slate-600"
                >
                  {{ device.description }}
                </span>
              </button>
            </li>
          </ul>
        </section>

        <section
          aria-labelledby="device-detail"
          class="min-w-0 rounded-2xl border border-slate-200 bg-white p-5 shadow-sm"
        >
          <h2 id="device-detail" class="font-semibold">装置详情</h2>
          <p
            v-if="!selectedDevice"
            class="mt-5 rounded-lg bg-slate-50 px-4 py-6 text-sm text-slate-600"
          >
            请选择装置查看数据集。
          </p>
          <template v-else>
            <p class="mt-1 text-sm break-words text-slate-600">{{ selectedDevice }}</p>
            <div class="mt-4 grid min-w-0 gap-5 md:grid-cols-[minmax(10rem,1fr)_minmax(0,3fr)]">
              <section aria-labelledby="dataset-list" class="min-w-0">
                <h3 id="dataset-list" class="text-sm font-semibold">数据集列表</h3>
                <div v-if="dataSetsError" class="mt-3 text-sm">
                  <p role="alert" class="text-red-800">无法读取数据集。</p>
                  <button
                    type="button"
                    class="mt-2 text-teal-800 underline"
                    @click="refreshDataSets"
                  >
                    重试
                  </button>
                </div>
                <p v-else-if="dataSetsLoading" role="status" class="mt-3 text-sm text-slate-600">
                  正在读取数据集…
                </p>
                <p v-else-if="!dataSets.length" class="mt-3 text-sm text-slate-600">
                  该装置没有数据集。
                </p>
                <ul
                  v-else
                  class="mt-2 max-h-[28rem] space-y-1 overflow-auto p-1"
                  aria-label="数据集"
                >
                  <li v-for="dataSet in dataSets" :key="dataSet.identity">
                    <button
                      type="button"
                      :aria-pressed="selectedDataSet === dataSet.identity"
                      class="w-full rounded-lg px-3 py-2.5 text-left text-sm hover:bg-slate-50 focus-visible:outline-2 focus-visible:outline-teal-700 aria-pressed:bg-teal-50 aria-pressed:text-teal-900"
                      @click="selectedDataSet = dataSet.identity"
                    >
                      <span class="block font-semibold break-words">{{ dataSet.name }}</span>
                      <span class="mt-1 block text-xs break-words text-slate-600">
                        {{ dataSet.description || dataSet.identity }}
                      </span>
                    </button>
                  </li>
                </ul>
              </section>

              <section aria-labelledby="fcda-list" class="min-w-0">
                <h3 id="fcda-list" class="text-sm font-semibold">FCDA</h3>
                <p v-if="!selectedDataSet" class="mt-3 text-sm text-slate-600">请选择数据集。</p>
                <div v-else-if="fcdaError" class="mt-3 text-sm">
                  <p role="alert" class="text-red-800">无法读取 FCDA。</p>
                  <button type="button" class="mt-2 text-teal-800 underline" @click="refreshFcda">
                    重试
                  </button>
                </div>
                <p v-else-if="fcdaLoading" role="status" class="mt-3 text-sm text-slate-600">
                  正在读取 FCDA…
                </p>
                <p v-else-if="!fcda.length" class="mt-3 text-sm text-slate-600">
                  该数据集没有 FCDA。
                </p>
                <div
                  v-else
                  role="region"
                  aria-labelledby="fcda-list"
                  tabindex="0"
                  class="mt-3 max-h-[28rem] overflow-auto rounded-lg border border-slate-200 focus-visible:outline-2 focus-visible:outline-teal-700"
                >
                  <table
                    class="w-full min-w-[28rem] border-separate border-spacing-0 text-left text-sm"
                  >
                    <thead class="sticky top-0 bg-[#edf3f1] text-slate-700">
                      <tr>
                        <th scope="col" class="px-3 py-2">序号</th>
                        <th scope="col" class="px-3 py-2">引用</th>
                        <th scope="col" class="px-3 py-2">描述</th>
                        <th scope="col" class="px-3 py-2">FC</th>
                        <th scope="col" class="px-3 py-2">CDC</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr v-for="item in fcda" :key="item.id" class="even:bg-slate-50">
                        <td class="px-3 py-2 tabular-nums">{{ item.index + 1 }}</td>
                        <td class="px-3 py-2 font-mono break-all">{{ item.reference || '—' }}</td>
                        <td class="px-3 py-2 break-words">{{ item.description || '—' }}</td>
                        <td class="px-3 py-2">{{ item.fc || '—' }}</td>
                        <td class="px-3 py-2">{{ item.cdc || '—' }}</td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              </section>
            </div>
          </template>
        </section>
      </div>
    </main>
  </div>
</template>
