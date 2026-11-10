<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import EditDeviceCategoryDialog from '../components/EditDeviceCategoryDialog.vue'

const props = defineProps<{ name: string }>()

interface StationDetail {
  name: string
  description: string | null
  scdUpdatedAt: string | null
}

interface DeviceSummary {
  name: string
  description: string | null
  category: number
  categoryLabel: string
}

interface DataSetSummary {
  identity: string
  name: string
  description: string | null
  controls: ControlBlock[]
}

interface ControlBlock {
  id: number
  type: string
  name: string | null
  reference: string | null
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
const scdUpdateHint = computed(() => {
  if (!station.value) return undefined
  const updatedAt = station.value.scdUpdatedAt
  return updatedAt ? `上次更新 SCD：${updatedAt.replace('T', ' ')}` : '尚未更新 SCD'
})
const detailError = ref('')
const fileInput = ref<HTMLInputElement | null>(null)
const uploading = ref(false)
const uploadError = ref('')
const scdToast = ref<HTMLDivElement | null>(null)
const scdToastMessage = ref('')
const supportsPopover = typeof HTMLElement !== 'undefined' && 'showPopover' in HTMLElement.prototype
let scdToastTimer: ReturnType<typeof setTimeout> | undefined
const selectedDevice = ref<string | null>(null)
const selectedDataSet = ref<string | null>(null)
const deviceQuery = ref('')
const categoryDialog = ref<InstanceType<typeof EditDeviceCategoryDialog> | null>(null)
const batchMode = ref(false)
const batchSelection = ref(new Set<string>())

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
const currentDevice = computed(() =>
  devices.value.find(({ name }) => name === selectedDevice.value),
)
const deviceNameOrder = new Intl.Collator('zh-CN', { numeric: true })

function categoryOrder(device: DeviceSummary) {
  return device.categoryLabel === '未知' ? 0 : device.category
}

const visibleDevices = computed(() => {
  const keyword = deviceQuery.value.trim().toLowerCase()
  const matching = devices.value.filter(
    ({ name, description }) =>
      !keyword ||
      name.toLowerCase().includes(keyword) ||
      description?.toLowerCase().includes(keyword),
  )
  return matching.sort(
    (a, b) => categoryOrder(a) - categoryOrder(b) || deviceNameOrder.compare(a.name, b.name),
  )
})
const visibleSelectionCount = computed(
  () => visibleDevices.value.filter(({ name }) => batchSelection.value.has(name)).length,
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

function updateCategory(
  target: { station: string; devices: string[]; batch: boolean },
  category: { value: number; label: string },
) {
  if (station.value?.name !== target.station) return
  const names = new Set(target.devices)
  for (const device of devices.value) {
    if (names.has(device.name)) {
      device.category = category.value
      device.categoryLabel = category.label
    }
  }
  if (target.batch) exitBatchMode()
}

function exitBatchMode() {
  batchMode.value = false
  batchSelection.value.clear()
}

function selectVisibleDevices(event: Event) {
  const checked = (event.target as HTMLInputElement).checked
  for (const { name } of visibleDevices.value) {
    if (checked) batchSelection.value.add(name)
    else batchSelection.value.delete(name)
  }
}

function dismissScdToast() {
  clearTimeout(scdToastTimer)
  scdToastMessage.value = ''
  scdToast.value?.hidePopover?.()
}

function showScdToast(deviceCount: number) {
  if (!scdToast.value) return
  scdToastMessage.value = `SCD 已更新，共导入 ${deviceCount} 个装置。`
  scdToast.value.showPopover?.()
  scdToastTimer = setTimeout(dismissScdToast, 5000)
}

onBeforeUnmount(dismissScdToast)

watch(
  () => props.name,
  async (name, _, onCleanup) => {
    const request = new AbortController()
    onCleanup(() => request.abort())
    station.value = null
    deviceQuery.value = ''
    exitBatchMode()
    selectedDevice.value = null
    selectedDataSet.value = null
    detailError.value = ''
    dismissScdToast()
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
  dismissScdToast()
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
      const result = (await response.json()) as { deviceCount: number; scdUpdatedAt: string }
      if (station.value === target) {
        showScdToast(result.deviceCount)
        target.scdUpdatedAt = result.scdUpdatedAt
        exitBatchMode()
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
              :title="scdUpdateHint"
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
          <div class="mt-4" role="search" aria-label="筛选装置列表">
            <label for="device-query" class="mb-1 block text-sm font-medium text-slate-700">
              搜索装置
            </label>
            <input
              id="device-query"
              v-model="deviceQuery"
              type="search"
              placeholder="输入装置名或描述"
              class="min-h-12 w-full rounded-lg border border-slate-400 px-3 focus-visible:outline-2 focus-visible:outline-teal-700"
            />
          </div>
          <button
            v-if="!batchMode"
            type="button"
            :disabled="uploading || !devices.length"
            class="mt-3 rounded-lg border border-slate-300 px-3 py-2 text-sm font-medium text-teal-800 hover:bg-teal-50 focus-visible:outline-2 focus-visible:outline-teal-700 disabled:opacity-50"
            @click="batchMode = true"
          >
            批量修改类型
          </button>
          <div
            v-else
            class="mt-3 space-y-2 rounded-lg border border-slate-200 bg-slate-50 px-3 pb-3"
          >
            <label class="flex min-h-11 cursor-pointer items-center gap-2 text-sm text-slate-700">
              <input
                type="checkbox"
                :checked="
                  visibleDevices.length > 0 && visibleSelectionCount === visibleDevices.length
                "
                :indeterminate="
                  visibleSelectionCount > 0 && visibleSelectionCount < visibleDevices.length
                "
                :disabled="!visibleDevices.length"
                class="size-4 accent-teal-800 focus-visible:outline-2 focus-visible:outline-teal-700 disabled:opacity-50"
                @change="selectVisibleDevices"
              />
              全选当前筛选结果
            </label>
            <p class="text-sm text-slate-600" role="status">
              已选 {{ batchSelection.size }} 个装置
              <span v-if="batchSelection.size > visibleSelectionCount">
                （{{ batchSelection.size - visibleSelectionCount }} 个不在当前结果中）
              </span>
            </p>
            <div class="flex flex-wrap gap-2">
              <button
                type="button"
                :disabled="
                  !station || uploading || devicesLoading || devicesError || !batchSelection.size
                "
                class="rounded-lg bg-teal-800 px-3 py-2 text-sm font-medium text-white hover:bg-teal-900 focus-visible:outline-2 focus-visible:outline-teal-700 disabled:opacity-50"
                @click="station && categoryDialog?.open(station.name, [...batchSelection])"
              >
                修改所选类型
              </button>
              <button
                type="button"
                class="rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm hover:bg-slate-100 focus-visible:outline-2 focus-visible:outline-teal-700"
                @click="exitBatchMode"
              >
                取消批量选择
              </button>
            </div>
          </div>
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
          <p v-else-if="!visibleDevices.length" role="status" class="mt-4 text-sm text-slate-600">
            没有匹配的装置。
          </p>
          <ul v-else class="mt-4 max-h-[32rem] space-y-1 overflow-auto p-1" aria-label="装置">
            <li v-for="device in visibleDevices" :key="device.name" class="flex items-center">
              <label
                v-if="batchMode"
                class="flex min-h-11 w-11 shrink-0 cursor-pointer items-center justify-center"
              >
                <input
                  v-model="batchSelection"
                  type="checkbox"
                  :value="device.name"
                  class="size-4 accent-teal-800 focus-visible:outline-2 focus-visible:outline-teal-700"
                />
                <span class="sr-only">选择 {{ device.name }}</span>
              </label>
              <button
                type="button"
                :aria-pressed="selectedDevice === device.name"
                class="min-w-0 flex-1 rounded-lg px-3 py-2.5 text-left text-sm focus-visible:outline-2 focus-visible:outline-teal-700 aria-pressed:bg-teal-50 aria-pressed:text-teal-900"
                :class="
                  device.categoryLabel === '未知'
                    ? 'bg-amber-50 hover:bg-amber-100'
                    : 'hover:bg-slate-50'
                "
                @click="selectDevice(device.name)"
              >
                <span class="block font-semibold break-words">
                  {{ device.description?.trim() || '未填写描述' }}
                </span>
                <span class="mt-1 flex flex-wrap items-center gap-x-2 gap-y-1">
                  <span class="min-w-0 text-xs break-words text-slate-600">
                    {{ device.name }}
                  </span>
                  <span
                    :title="`分类代码：${device.category}`"
                    class="rounded px-1.5 py-0.5 text-xs"
                    :class="
                      device.categoryLabel === '未知'
                        ? 'bg-amber-100 font-semibold text-amber-950 ring-1 ring-amber-300 ring-inset'
                        : 'bg-slate-100 text-slate-700'
                    "
                  >
                    {{ device.categoryLabel }}
                  </span>
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
            <div class="mt-1 flex flex-wrap items-center gap-2">
              <p class="min-w-0 break-words">
                <span class="font-semibold">
                  {{ currentDevice?.description?.trim() || '未填写描述' }}
                </span>
                <span class="ml-2 text-sm text-slate-600">{{ selectedDevice }}</span>
              </p>
              <template v-if="station && currentDevice">
                <span class="rounded bg-slate-100 px-1.5 py-0.5 text-xs text-slate-700">
                  {{ currentDevice.categoryLabel }}
                </span>
                <button
                  type="button"
                  :disabled="uploading"
                  class="rounded-lg border border-slate-300 px-3 py-1.5 text-sm font-medium text-teal-800 hover:bg-teal-50 focus-visible:outline-2 focus-visible:outline-teal-700 disabled:opacity-50"
                  @click="categoryDialog?.open(station.name, currentDevice)"
                >
                  修改类型
                </button>
              </template>
            </div>
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
                      class="flex w-full items-start gap-3 rounded-lg px-3 py-2.5 text-left text-sm hover:bg-slate-50 focus-visible:outline-2 focus-visible:outline-teal-700 aria-pressed:bg-teal-50 aria-pressed:text-teal-900"
                      @click="selectedDataSet = dataSet.identity"
                    >
                      <span class="min-w-0 flex-1">
                        <span class="block font-semibold break-words">
                          {{ dataSet.description?.trim() || '未填写描述' }}
                        </span>
                        <span class="mt-1 block text-xs break-words text-slate-600">
                          {{ dataSet.name }}
                        </span>
                      </span>
                      <span v-if="dataSet.controls?.length" class="w-[52%] min-w-0 space-y-1">
                        <span
                          v-for="control in dataSet.controls"
                          :key="`${control.type}-${control.id}`"
                          :title="control.reference ?? undefined"
                          class="flex flex-col items-end gap-0.5 text-right text-xs text-slate-700"
                        >
                          <span class="shrink-0 rounded bg-slate-100 px-1.5 py-0.5 font-semibold">
                            {{ control.type }}
                          </span>
                          <span class="min-w-0 break-words">
                            {{ control.name || control.reference || '未命名控制块' }}
                          </span>
                        </span>
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
      <EditDeviceCategoryDialog
        v-if="station"
        :key="station.name"
        ref="categoryDialog"
        @saved="updateCategory"
      />
    </main>
    <p role="status" class="sr-only">{{ scdToastMessage }}</p>
    <div
      ref="scdToast"
      :popover="supportsPopover ? 'manual' : undefined"
      :class="{ hidden: !supportsPopover && !scdToastMessage }"
      class="fixed inset-auto right-4 bottom-4 left-4 z-30 m-0 w-auto rounded-xl border border-teal-200 bg-white px-4 py-3 text-teal-900 shadow-lg sm:left-auto sm:max-w-sm"
    >
      <div class="flex items-center gap-3">
        <p aria-hidden="true" class="text-sm font-medium">{{ scdToastMessage }}</p>
        <button
          type="button"
          aria-label="关闭 SCD 更新提示"
          class="ml-auto grid size-8 shrink-0 place-items-center rounded text-lg leading-none text-teal-900 hover:bg-teal-50 focus-visible:outline-2 focus-visible:outline-teal-700"
          @click="dismissScdToast"
        >
          ×
        </button>
      </div>
    </div>
  </div>
</template>
