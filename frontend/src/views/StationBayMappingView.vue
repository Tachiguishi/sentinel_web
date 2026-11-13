<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { onBeforeRouteLeave, onBeforeRouteUpdate, RouterLink } from 'vue-router'

const props = defineProps<{ name: string }>()

interface Bay {
  code: string
  name: string | null
  description: string | null
  voltage: number | null
}
interface Device {
  name: string
  description: string | null
  bayCode: string | null
}
interface Station {
  name: string
  description: string | null
}

const station = ref<Station | null>(null)
const bays = ref<Bay[]>([])
const devices = ref<Device[]>([])
const original = ref(new Map<string, string | null>())
const selected = ref(new Set<string>())
const deviceQuery = ref('')
const bayQuery = ref('')
const loading = ref(false)
const saving = ref(false)
const refreshingBays = ref(false)
const catalogError = ref('')
const loadError = ref('')
const saveError = ref('')
const message = ref('')
const revision = ref(0)
const dragNames = ref<string[]>([])
const dropTarget = ref<string | null>(null)
let saveRequest: AbortController | undefined
let catalogRequest: AbortController | undefined

const changes = computed(() =>
  devices.value.filter((device) => original.value.get(device.name) !== device.bayCode),
)
const assignedCount = computed(
  () => devices.value.filter((device) => device.bayCode !== null).length,
)
const unassignedDevices = computed(() => devices.value.filter((device) => device.bayCode === null))
const knownCodes = computed(() => new Set(bays.value.map((bay) => bay.code)))
const orphaned = computed(() =>
  devices.value.filter(
    (device) => device.bayCode !== null && !knownCodes.value.has(device.bayCode),
  ),
)
const visibleDevices = computed(() => {
  const query = deviceQuery.value.trim().toLocaleLowerCase()
  return unassignedDevices.value.filter((device) =>
    `${device.name} ${device.description ?? ''}`.toLocaleLowerCase().includes(query),
  )
})
const visibleBays = computed(() => {
  const query = bayQuery.value.trim().toLocaleLowerCase()
  return bays.value.filter((bay) =>
    `${bay.code} ${bay.name ?? ''} ${bay.description ?? ''} ${bay.voltage ?? ''}`
      .toLocaleLowerCase()
      .includes(query),
  )
})
const devicesByBay = computed(() => {
  const grouped = new Map<string, Device[]>()
  for (const device of devices.value) {
    if (device.bayCode === null) continue
    const group = grouped.get(device.bayCode) ?? []
    group.push(device)
    grouped.set(device.bayCode, group)
  }
  return grouped
})

function bayLabel(bay: Bay) {
  return bay.description?.trim() || bay.name?.trim() || bay.code
}
function currentBay(code: string | null) {
  if (code === null) return '未分配间隔'
  const bay = bays.value.find((bay) => bay.code === code)
  return bay ? bayLabel(bay) : `目录中不存在：${code}`
}

watch(
  [() => props.name, revision],
  async ([name], _, onCleanup) => {
    const request = new AbortController()
    onCleanup(() => request.abort())
    saveRequest?.abort()
    catalogRequest?.abort()
    catalogRequest = undefined
    refreshingBays.value = false
    catalogError.value = ''
    station.value = null
    devices.value = []
    bays.value = []
    original.value = new Map()
    selected.value.clear()
    deviceQuery.value = ''
    bayQuery.value = ''
    endDrag()
    loading.value = true
    saving.value = false
    loadError.value = ''
    saveError.value = ''
    message.value = ''
    try {
      const query = new URLSearchParams({ name })
      const responses = await Promise.all([
        fetch(`/api/stations/detail?${query}`, { signal: request.signal }),
        fetch(`/api/stations/bays?${query}`, { signal: request.signal }),
        fetch(`/api/stations/scd/bay-mappings?${query}`, { signal: request.signal }),
      ])
      if (responses.some((response) => !response.ok)) throw new Error('load failed')
      const [detail, catalog, mappings] = await Promise.all(
        responses.map((response) => response.json()),
      )
      if (request.signal.aborted) return
      station.value = detail as Station
      bays.value = catalog as Bay[]
      devices.value = mappings as Device[]
      original.value = new Map(devices.value.map((device) => [device.name, device.bayCode]))
    } catch {
      if (!request.signal.aborted)
        loadError.value = '无法加载厂站间隔或 SCD 装置，请检查连接后重试。'
    } finally {
      if (!request.signal.aborted) loading.value = false
    }
  },
  { immediate: true },
)

function toggle(name: string, checked: boolean) {
  if (checked) selected.value.add(name)
  else selected.value.delete(name)
}
function selectVisible() {
  visibleDevices.value.forEach((device) => selected.value.add(device.name))
}
// Assigned rows leave the source list immediately; hidden rows must not remain selected.
watch(unassignedDevices, (rows) => {
  const names = new Set(rows.map((device) => device.name))
  for (const name of selected.value) if (!names.has(name)) selected.value.delete(name)
})

async function refreshBays() {
  if (loading.value || saving.value || refreshingBays.value || !station.value) return
  const target = props.name
  const request = new AbortController()
  catalogRequest = request
  refreshingBays.value = true
  catalogError.value = ''
  try {
    const response = await fetch(`/api/stations/bays?${new URLSearchParams({ name: target })}`, {
      signal: request.signal,
      cache: 'no-store',
    })
    if (!response.ok) throw new Error(`HTTP ${response.status}`)
    const catalog = (await response.json()) as Bay[]
    if (request.signal.aborted || catalogRequest !== request || target !== props.name) return
    bays.value = catalog
    message.value = `间隔目录已刷新，共 ${catalog.length} 个间隔。`
  } catch {
    if (!request.signal.aborted && catalogRequest === request)
      catalogError.value = '刷新间隔失败，已保留当前目录，请重试。'
  } finally {
    if (catalogRequest === request) {
      refreshingBays.value = false
      catalogRequest = undefined
    }
  }
}

function assign(names: string[], code: string | null) {
  if (saving.value || (code !== null && !knownCodes.value.has(code))) return
  const targets = new Set(names)
  let count = 0
  for (const device of devices.value) {
    if (targets.has(device.name) && device.bayCode !== code) {
      device.bayCode = code
      count++
    }
  }
  if (count) {
    message.value = `${count} 个装置已${code === null ? '移至未分配' : `分配至${currentBay(code)}`}，请保存配置。`
    saveError.value = ''
  }
}
function startDrag(event: DragEvent, device: Device) {
  if (saving.value || !event.dataTransfer) {
    event.preventDefault()
    return
  }
  dragNames.value = selected.value.has(device.name) ? [...selected.value] : [device.name]
  event.dataTransfer.effectAllowed = 'move'
  event.dataTransfer.setData(
    'application/x-sentinel-devices',
    JSON.stringify({ station: props.name, devices: dragNames.value }),
  )
}
function dragOver(event: DragEvent, code: string) {
  if (saving.value || !dragNames.value.length) return
  event.preventDefault()
  if (event.dataTransfer) event.dataTransfer.dropEffect = 'move'
  dropTarget.value = code
}
function drop(event: DragEvent, code: string | null) {
  event.preventDefault()
  // Only a drag started on this page can change assignments. External text/files are ignored.
  if (!saving.value && dragNames.value.length) assign(dragNames.value, code)
  endDrag()
}
function endDrag() {
  dragNames.value = []
  dropTarget.value = null
}
function reset() {
  if (saving.value) return
  devices.value.forEach((device) => {
    device.bayCode = original.value.get(device.name) ?? null
  })
  saveError.value = ''
  message.value = '已撤销未保存的修改。'
}
async function save() {
  if (!changes.value.length || saving.value || refreshingBays.value) return
  const target = props.name
  const assignments = changes.value.map((device) => ({
    device: device.name,
    bayCode: device.bayCode,
  }))
  const request = new AbortController()
  saveRequest = request
  saving.value = true
  endDrag()
  saveError.value = ''
  message.value = ''
  try {
    const response = await fetch(
      `/api/stations/scd/bay-mappings?${new URLSearchParams({ name: target })}`,
      {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(assignments),
        signal: request.signal,
      },
    )
    if (request.signal.aborted || target !== props.name) return
    if (!response.ok) {
      saveError.value =
        response.status === 400 || response.status === 404
          ? '装置或间隔目录已变化，请核对后重新加载。未保存的修改仍保留。'
          : '无法保存配置，请重试。未保存的修改仍保留。'
      return
    }
    original.value = new Map(devices.value.map((device) => [device.name, device.bayCode]))
    message.value = `已保存 ${assignments.length} 个装置的间隔配置。`
  } catch {
    if (!request.signal.aborted) saveError.value = '无法保存配置，请重试。未保存的修改仍保留。'
  } finally {
    if (!request.signal.aborted && target === props.name) saving.value = false
  }
}
function canLeave() {
  if (saving.value) return false
  return !changes.value.length || window.confirm('间隔配置尚未保存，确定离开并放弃修改吗？')
}
onBeforeRouteLeave(canLeave)
onBeforeRouteUpdate(canLeave)
function beforeUnload(event: BeforeUnloadEvent) {
  if (changes.value.length || saving.value) {
    event.preventDefault()
    event.returnValue = ''
  }
}
window.addEventListener('beforeunload', beforeUnload)
onBeforeUnmount(() => {
  saveRequest?.abort()
  catalogRequest?.abort()
  window.removeEventListener('beforeunload', beforeUnload)
})
</script>

<template>
  <div class="min-h-screen bg-[#f2f5f4] text-slate-900">
    <header class="border-b border-slate-800 bg-[#101e28] text-white">
      <div class="mx-auto flex max-w-[100rem] items-center justify-between gap-3 px-5 py-5 sm:px-8">
        <span class="text-sm font-semibold tracking-[0.16em]">SENTINEL</span>
        <RouterLink
          :to="{ name: 'station-detail', params: { name } }"
          class="text-sm text-teal-200 hover:underline"
          >返回站点详情</RouterLink
        >
      </div>
    </header>
    <main class="mx-auto max-w-[100rem] px-5 py-9 sm:px-8">
      <p class="text-xs font-semibold tracking-[0.2em] text-teal-800">
        {{ station?.description || name }} · {{ name }}
      </p>
      <div class="mt-3 flex flex-wrap items-center justify-between gap-4">
        <h1 class="text-2xl font-bold">装置与间隔配置</h1>
        <div class="flex flex-wrap items-center gap-3">
          <span class="text-sm text-slate-600">{{ changes.length }} 项未保存</span>
          <button
            type="button"
            :disabled="saving || !changes.length"
            class="action-secondary"
            @click="reset"
          >
            撤销修改
          </button>
          <button
            type="button"
            :disabled="loading || saving || refreshingBays || !!loadError || !changes.length"
            class="action-primary"
            @click="save"
          >
            {{ saving ? '正在保存…' : '保存配置' }}
          </button>
        </div>
      </div>
      <p id="mapping-help" class="mt-3 text-sm leading-6 text-slate-600">
        拖动整张装置卡片到目标间隔；勾选多个装置后，拖动任意已选卡片可一起分配，也可点击“分配所选装置”。每个装置对应一个间隔，一个间隔可包含多个装置。修改后点击“保存配置”。
      </p>
      <p role="status" class="mt-3 min-h-6 text-sm text-teal-800">{{ message }}</p>
      <div v-if="loadError" class="mt-4 rounded-xl border border-red-200 bg-white p-5">
        <p role="alert" class="text-red-800">{{ loadError }}</p>
        <button type="button" class="action-secondary mt-3" @click="revision++">重新加载</button>
      </div>
      <p v-else-if="loading" class="mt-5 text-slate-600">正在加载厂站间隔与 SCD 装置…</p>
      <template v-else>
        <div v-if="saveError" class="mb-4 flex flex-wrap items-center gap-3">
          <p role="alert" class="text-sm text-red-800">{{ saveError }}</p>
          <button
            type="button"
            :disabled="saving"
            class="action-secondary"
            @click="canLeave() && revision++"
          >
            重新加载
          </button>
        </div>
        <p
          v-if="!devices.length"
          class="mb-4 rounded-xl border border-slate-200 bg-white p-4 text-sm"
        >
          尚未导入 SCD 装置，请返回站点详情更新 SCD。
        </p>
        <div class="grid items-start gap-5 lg:grid-cols-[minmax(18rem,2fr)_minmax(0,5fr)]">
          <section class="min-w-0 rounded-2xl border border-slate-200 bg-white p-5">
            <h2 class="font-semibold">
              未分配装置
              <span class="text-sm font-normal text-slate-600"
                >{{ unassignedDevices.length }} 个 / 共 {{ devices.length }} 个</span
              >
            </h2>
            <label for="mapping-device-query" class="mt-4 block text-sm font-medium"
              >筛选装置</label
            >
            <input
              id="mapping-device-query"
              v-model="deviceQuery"
              type="search"
              placeholder="名称或描述"
              class="search-input mt-1"
            />
            <div class="mt-3 flex flex-wrap items-center gap-2 text-sm">
              <button
                type="button"
                :disabled="saving || !visibleDevices.length"
                class="action-secondary"
                @click="selectVisible"
              >
                全选筛选结果
              </button>
              <button
                type="button"
                :disabled="saving || !selected.size"
                class="action-secondary"
                @click="selected.clear()"
              >
                清空选择
              </button>
              <span>已选 {{ selected.size }} 个</span>
            </div>
            <p v-if="!visibleDevices.length" class="mt-4 text-sm text-slate-600">
              {{
                unassignedDevices.length ? '没有符合条件的未分配装置。' : '所有装置均已分配间隔。'
              }}
            </p>
            <div class="mt-3 max-h-[65vh] space-y-2 overflow-y-auto p-1">
              <div
                v-for="device in visibleDevices"
                :key="device.name"
                :draggable="!saving"
                class="device-card flex items-start gap-2 rounded-xl border p-2"
                :class="
                  selected.has(device.name) ? 'border-teal-600 bg-teal-50' : 'border-slate-200'
                "
                @dragstart="startDrag($event, device)"
                @dragend="endDrag"
              >
                <button
                  type="button"
                  :disabled="saving"
                  :aria-label="`选择或拖动 ${device.name}`"
                  :aria-pressed="selected.has(device.name)"
                  aria-describedby="mapping-help"
                  class="drag-handle shrink-0"
                  @click="toggle(device.name, !selected.has(device.name))"
                >
                  ⠿
                </button>
                <label
                  :for="`mapping-device-${encodeURIComponent(device.name)}`"
                  class="flex min-w-0 flex-1 items-start gap-2 py-2"
                >
                  <input
                    :id="`mapping-device-${encodeURIComponent(device.name)}`"
                    type="checkbox"
                    class="mt-1 shrink-0"
                    :aria-label="`选择 ${device.name}`"
                    :checked="selected.has(device.name)"
                    :disabled="saving"
                    @change="toggle(device.name, ($event.target as HTMLInputElement).checked)"
                  />
                  <span class="min-w-0 text-sm">
                    <span class="block font-semibold break-words">{{
                      device.description || device.name
                    }}</span>
                    <span class="block break-all text-slate-600">{{ device.name }}</span>
                    <span class="mt-1 block break-words text-teal-800">{{
                      currentBay(device.bayCode)
                    }}</span>
                  </span>
                </label>
              </div>
            </div>
          </section>
          <section class="min-w-0">
            <div
              class="rounded-2xl border-2 border-dashed bg-white p-5"
              :class="dropTarget === '' ? 'border-teal-700 bg-teal-50' : 'border-slate-300'"
              @dragover="dragOver($event, '')"
              @dragleave="dropTarget = null"
              @drop.stop="drop($event, null)"
            >
              <div class="flex flex-wrap items-center justify-between gap-3">
                <div>
                  <h2 class="font-semibold">未分配间隔</h2>
                  <p class="mt-1 text-sm text-slate-600">
                    {{ devices.length - assignedCount }} 个装置；点击间隔内的“移出”可解除关联。
                  </p>
                </div>
              </div>
            </div>
            <div
              v-if="orphaned.length"
              class="mt-4 rounded-xl border border-amber-300 bg-amber-50 p-4 text-sm text-amber-950"
            >
              <p>
                {{ orphaned.length }} 个装置的已配间隔在当前厂站目录中不存在，请重新分配或解除关联。
              </p>
              <div
                v-for="device in orphaned"
                :key="device.name"
                class="mt-2 flex items-center gap-2"
              >
                <p class="min-w-0 flex-1 break-words">{{ device.name }} → {{ device.bayCode }}</p>
                <button
                  type="button"
                  :disabled="saving"
                  :aria-label="`解除关联 ${device.name}`"
                  class="action-secondary shrink-0"
                  @click="assign([device.name], null)"
                >
                  解除关联
                </button>
              </div>
            </div>
            <div class="mt-5 flex flex-wrap items-end justify-between gap-3">
              <h2 class="font-semibold">
                厂站间隔
                <span class="text-sm font-normal text-slate-600">{{ bays.length }} 个</span>
              </h2>
              <div class="flex max-w-full min-w-0 flex-wrap items-end gap-2">
                <div class="min-w-0 flex-1">
                  <label for="mapping-bay-query" class="block text-sm font-medium">筛选间隔</label>
                  <input
                    id="mapping-bay-query"
                    v-model="bayQuery"
                    type="search"
                    placeholder="代码、名称、描述或电压"
                    class="search-input mt-1"
                  />
                </div>
                <button
                  type="button"
                  :disabled="loading || saving || refreshingBays"
                  :aria-busy="refreshingBays"
                  aria-label="刷新间隔"
                  class="action-secondary shrink-0"
                  @click="refreshBays"
                >
                  {{ refreshingBays ? '正在刷新…' : '刷新' }}
                </button>
              </div>
            </div>
            <progress
              v-if="refreshingBays"
              aria-label="正在刷新间隔目录"
              class="mt-3 h-2 w-full accent-teal-800"
            ></progress>
            <p v-if="catalogError" role="alert" class="mt-3 text-sm text-red-800">
              {{ catalogError }}
            </p>
            <p
              v-if="!bays.length"
              class="mt-4 rounded-xl border border-slate-200 bg-white p-5 text-sm text-slate-600"
            >
              该厂站在 TB1042_BAY 中没有间隔，请先维护间隔目录。
            </p>
            <p v-else-if="!visibleBays.length" class="mt-4 text-sm text-slate-600">
              没有符合条件的间隔。
            </p>
            <ul v-else role="list" class="mt-4 grid gap-4 xl:grid-cols-2">
              <li
                v-for="bay in visibleBays"
                :key="bay.code"
                class="min-w-0 rounded-2xl border-2 bg-white p-5"
                :class="dropTarget === bay.code ? 'border-teal-700 bg-teal-50' : 'border-slate-200'"
                @dragover="dragOver($event, bay.code)"
                @dragleave="dropTarget = null"
                @drop.stop="drop($event, bay.code)"
              >
                <div class="flex items-start justify-between gap-3">
                  <h3 class="font-semibold break-words">{{ bayLabel(bay) }}</h3>
                  <span class="shrink-0 rounded bg-teal-50 px-2 py-1 text-xs text-teal-900">{{
                    bay.voltage === null ? '电压未填写' : `${bay.voltage} kV`
                  }}</span>
                </div>
                <dl class="mt-3 space-y-1 text-sm text-slate-600">
                  <div class="flex gap-2">
                    <dt class="shrink-0">代码</dt>
                    <dd class="break-all">{{ bay.code }}</dd>
                  </div>
                  <div class="flex gap-2">
                    <dt class="shrink-0">名称</dt>
                    <dd class="break-words">{{ bay.name || '—' }}</dd>
                  </div>
                  <div class="flex gap-2">
                    <dt class="shrink-0">描述</dt>
                    <dd class="break-words">{{ bay.description || '—' }}</dd>
                  </div>
                </dl>
                <button
                  type="button"
                  :disabled="saving || !selected.size"
                  :aria-label="`分配所选装置到 ${bayLabel(bay)} (${bay.code})`"
                  class="action-secondary mt-4 w-full"
                  @click="assign([...selected], bay.code)"
                >
                  分配所选装置
                </button>
                <p class="mt-4 text-xs font-medium text-slate-600">
                  已关联 {{ devicesByBay.get(bay.code)?.length ?? 0 }} 个装置
                </p>
                <ul v-if="devicesByBay.get(bay.code)?.length" role="list" class="mt-2 space-y-2">
                  <li
                    v-for="device in devicesByBay.get(bay.code)"
                    :key="device.name"
                    class="flex items-center gap-2 rounded-lg bg-slate-50 px-3 py-1 text-sm"
                  >
                    <span class="min-w-0 flex-1 break-words"
                      >{{ device.description || device.name
                      }}<span class="block text-xs text-slate-600">{{ device.name }}</span></span
                    >
                    <button
                      type="button"
                      :disabled="saving"
                      :aria-label="`移出 ${device.name}`"
                      class="action-secondary shrink-0"
                      @click="assign([device.name], null)"
                    >
                      移出
                    </button>
                  </li>
                </ul>
              </li>
            </ul>
          </section>
        </div>
      </template>
    </main>
  </div>
</template>

<style scoped>
@reference '../style.css';
.action-primary {
  @apply min-h-11 rounded-lg bg-teal-800 px-4 py-2 text-sm font-medium text-white hover:bg-teal-900 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-teal-700 disabled:cursor-default disabled:opacity-50;
}
.action-secondary {
  @apply min-h-11 rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm font-medium text-teal-800 hover:bg-teal-50 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-teal-700 disabled:cursor-default disabled:opacity-50;
}
.search-input {
  @apply min-h-11 w-full rounded-lg border border-slate-400 bg-white px-3 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-teal-700;
}
.device-card {
  -webkit-user-select: none;
  user-select: none;
}
.device-card[draggable='true'],
.device-card[draggable='true'] label {
  cursor: grab;
}
.device-card[draggable='true']:active,
.device-card[draggable='true'] label:active {
  cursor: grabbing;
}
.device-card input {
  cursor: pointer;
}
.drag-handle {
  @apply grid min-h-11 min-w-11 cursor-grab place-items-center rounded-lg text-xl text-slate-600 hover:bg-teal-100 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-teal-700 disabled:cursor-default disabled:opacity-50;
  -webkit-user-select: none;
  user-select: none;
}
.drag-handle:active {
  cursor: grabbing;
}
@media (forced-colors: active) {
  [aria-pressed='true'] {
    outline: 2px solid Highlight;
  }
}
</style>
