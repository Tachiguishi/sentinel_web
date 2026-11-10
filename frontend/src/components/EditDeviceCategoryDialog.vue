<script setup lang="ts">
import { ref } from 'vue'

interface CategoryOption {
  value: number
  label: string
}

interface Target {
  station: string
  devices: string[]
  batch: boolean
}

const emit = defineEmits<{ saved: [target: Target, category: CategoryOption] }>()

const dialog = ref<HTMLDialogElement | null>(null)
const target = ref<Target | null>(null)
const options = ref<CategoryOption[]>([])
const selection = ref<number | ''>('')
const loading = ref(false)
const saving = ref(false)
const loadFailed = ref(false)
const saveError = ref('')

function validateSelection() {
  if (!options.value.some(({ value }) => value === selection.value)) selection.value = ''
}

async function loadOptions() {
  if (loading.value) return
  loading.value = true
  loadFailed.value = false
  try {
    const response = await fetch('/api/stations/scd/device-categories')
    if (!response.ok) throw new Error(`HTTP ${response.status}`)
    options.value = (await response.json()) as CategoryOption[]
    validateSelection()
  } catch {
    loadFailed.value = true
  } finally {
    loading.value = false
  }
}

function open(station: string, devices: string | string[], category: number | '' = '') {
  target.value =
    typeof devices === 'string'
      ? { station, devices: [devices], batch: false }
      : { station, devices: [...devices], batch: true }
  selection.value = category
  saveError.value = ''
  dialog.value?.showModal()
  if (options.value.length) validateSelection()
  else void loadOptions()
}

defineExpose({ open })

async function save() {
  if (!target.value || selection.value === '' || saving.value) return

  const current = target.value
  const query = new URLSearchParams({
    name: current.station,
    category: String(selection.value),
  })
  saving.value = true
  saveError.value = ''
  try {
    const response = await fetch(`/api/stations/scd/device-categories?${query}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(current.devices),
    })
    if (response.status === 404) {
      saveError.value = current.batch
        ? '有装置已不存在，请刷新列表。'
        : '装置已不存在，请刷新列表。'
    } else if (!response.ok) {
      saveError.value = '无法保存装置类型，请重试。'
    } else {
      const category = (await response.json()) as CategoryOption
      dialog.value?.close()
      emit('saved', current, category)
    }
  } catch {
    saveError.value = '无法保存装置类型，请重试。'
  } finally {
    saving.value = false
  }
}

function preventCloseWhileSaving(event: Event) {
  if (saving.value) event.preventDefault()
}
</script>

<template>
  <dialog
    ref="dialog"
    aria-labelledby="edit-device-category-title"
    class="m-auto w-[calc(100%-2rem)] max-w-lg overscroll-contain rounded-2xl border border-slate-200 bg-white p-6 text-slate-900 shadow-xl backdrop:overscroll-contain backdrop:bg-slate-950/50"
    @cancel="preventCloseWhileSaving"
  >
    <h2 id="edit-device-category-title" class="text-lg font-semibold">
      {{ target?.batch ? '批量修改装置类型' : '修改装置类型' }}
    </h2>
    <p class="mt-1 text-sm break-words text-slate-600">
      <template v-if="target?.batch">将 {{ target.devices.length }} 个装置设为同一类型。</template>
      <template v-else>{{ target?.devices[0] }}</template>
    </p>
    <form class="mt-5 space-y-4" @submit.prevent="save">
      <div v-if="loadFailed">
        <p role="alert" class="text-sm text-red-800">无法加载装置类型。</p>
        <button type="button" class="mt-2 text-sm text-teal-800 underline" @click="loadOptions">
          重试
        </button>
      </div>
      <p v-else-if="loading" role="status" class="text-sm text-slate-600">正在加载装置类型…</p>
      <div v-else>
        <label for="edit-device-category" class="mb-1 block text-sm font-medium">装置类型</label>
        <select
          id="edit-device-category"
          v-model="selection"
          name="category"
          required
          autofocus
          :disabled="saving"
          class="min-h-12 w-full rounded-lg border border-slate-400 bg-white px-3 focus-visible:outline-2 focus-visible:outline-teal-700"
          @change="saveError = ''"
        >
          <option disabled value="">请选择装置类型</option>
          <option v-for="option in options" :key="option.value" :value="option.value">
            {{ option.label }}
          </option>
        </select>
      </div>
      <p v-if="saveError" role="alert" class="text-sm text-red-800">{{ saveError }}</p>
      <div class="flex justify-end gap-2">
        <button
          type="button"
          :disabled="saving"
          class="min-h-12 rounded-lg border border-slate-300 px-4 text-sm font-medium hover:bg-slate-50 focus-visible:outline-2 focus-visible:outline-teal-700 disabled:opacity-50"
          @click="dialog?.close()"
        >
          取消
        </button>
        <button
          type="submit"
          :disabled="loading || saving || loadFailed"
          class="min-h-12 rounded-lg bg-teal-800 px-4 text-sm font-medium text-white hover:bg-teal-900 focus-visible:outline-2 focus-visible:outline-teal-700 disabled:opacity-50"
        >
          {{ saving ? '正在保存…' : '保存' }}
        </button>
      </div>
    </form>
  </dialog>
</template>
