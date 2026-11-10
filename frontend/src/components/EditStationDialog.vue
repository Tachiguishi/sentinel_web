<script setup lang="ts">
import { ref } from 'vue'

interface StationSettings {
  name: string
  ipAddress: string | null
  instantNo: number | null
}

const emit = defineEmits<{ saved: [settings: StationSettings] }>()

const dialog = ref<HTMLDialogElement | null>(null)
const stationName = ref('')
const stationDescription = ref<string | null>(null)
const ipAddress = ref('')
const instantNo = ref<number | ''>('')
const saving = ref(false)
const saveError = ref('')

function open(station: StationSettings & { description: string | null }) {
  stationName.value = station.name
  stationDescription.value = station.description
  ipAddress.value = station.ipAddress ?? ''
  instantNo.value = station.instantNo ?? ''
  saveError.value = ''
  dialog.value?.showModal()
}

defineExpose({ open })

async function save() {
  if (saving.value) return

  saving.value = true
  saveError.value = ''
  try {
    const response = await fetch('/api/stations/settings', {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        name: stationName.value,
        ipAddress: ipAddress.value.trim() || null,
        instantNo: instantNo.value === '' ? null : instantNo.value,
      }),
    })
    if (response.status === 400) {
      saveError.value = '请输入有效的 IPv4 地址和整数实例号，或留空。'
    } else if (response.status === 404) {
      saveError.value = '站点已不存在，请刷新列表。'
    } else if (!response.ok) {
      saveError.value = '无法保存，请重试。'
    } else {
      const settings = (await response.json()) as StationSettings
      dialog.value?.close()
      emit('saved', settings)
    }
  } catch {
    saveError.value = '无法保存，请重试。'
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
    aria-labelledby="edit-station-title"
    class="m-auto w-[calc(100%-2rem)] max-w-lg overscroll-contain rounded-2xl border border-slate-200 bg-white p-6 text-slate-900 shadow-xl backdrop:overscroll-contain backdrop:bg-slate-950/50"
    @cancel="preventCloseWhileSaving"
  >
    <h2 id="edit-station-title" class="text-lg font-semibold">编辑站点</h2>
    <p class="mt-2 text-sm break-words text-slate-600">
      <span>{{ stationName }}</span>
      <span class="ml-2">{{ stationDescription?.trim() || '—' }}</span>
    </p>

    <form class="mt-5 space-y-4" @submit.prevent="save">
      <div>
        <label for="edit-ip-address" class="mb-1 block text-sm font-medium">IP 地址</label>
        <input
          id="edit-ip-address"
          v-model="ipAddress"
          name="ipAddress"
          type="text"
          maxlength="15"
          autocomplete="off"
          spellcheck="false"
          autofocus
          :disabled="saving"
          placeholder="例如 192.0.2.10"
          class="min-h-12 w-full rounded-lg border border-slate-400 px-3 focus-visible:outline-2 focus-visible:outline-teal-700"
        />
      </div>
      <div>
        <label for="edit-instant-no" class="mb-1 block text-sm font-medium">实例号</label>
        <input
          id="edit-instant-no"
          v-model="instantNo"
          name="instantNo"
          type="number"
          min="-2147483648"
          max="2147483647"
          step="1"
          :disabled="saving"
          class="min-h-12 w-full rounded-lg border border-slate-400 px-3 focus-visible:outline-2 focus-visible:outline-teal-700"
        />
      </div>
      <p class="text-sm text-slate-600">留空可清除对应字段。</p>
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
          :disabled="saving"
          class="min-h-12 rounded-lg bg-teal-800 px-4 text-sm font-medium text-white hover:bg-teal-900 focus-visible:outline-2 focus-visible:outline-teal-700 disabled:opacity-50"
        >
          {{ saving ? '正在保存…' : '保存' }}
        </button>
      </div>
    </form>
  </dialog>
</template>
