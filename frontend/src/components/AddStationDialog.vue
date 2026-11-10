<script setup lang="ts">
import { computed, ref } from 'vue'

defineProps<{ disabled: boolean }>()
const emit = defineEmits<{ added: [] }>()

interface Candidate {
  code: string
  description: string | null
}

const dialog = ref<HTMLDialogElement | null>(null)
const candidates = ref<Candidate[]>([])
const query = ref('')
const selection = ref('')
const loading = ref(false)
const saving = ref(false)
const loadFailed = ref(false)
const saveError = ref('')
const visibleCandidates = computed(() => {
  const term = query.value.trim().toLowerCase()
  return candidates.value.filter(
    ({ code, description }) =>
      code.toLowerCase().includes(term) || description?.toLowerCase().includes(term),
  )
})

async function loadCandidates() {
  if (loading.value) return
  loading.value = true
  loadFailed.value = false
  try {
    const response = await fetch('/api/stations/candidates')
    if (!response.ok) throw new Error(`HTTP ${response.status}`)
    candidates.value = (await response.json()) as Candidate[]
  } catch {
    loadFailed.value = true
  } finally {
    loading.value = false
  }
}

function clearSelection() {
  selection.value = ''
  saveError.value = ''
}

function openDialog() {
  query.value = ''
  clearSelection()
  candidates.value = []
  dialog.value?.showModal()
  void loadCandidates()
}

async function addStation() {
  if (!selection.value || saving.value) return

  saving.value = true
  saveError.value = ''
  try {
    const response = await fetch('/api/stations', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ code: selection.value }),
    })
    if (response.status === 409) {
      saveError.value = '站点已经存在。'
    } else if (response.status === 404) {
      saveError.value = '该站点已不在目录中，请重新打开后选择。'
    } else if (!response.ok) {
      saveError.value = '无法添加站点，请重试。'
    } else {
      dialog.value?.close()
      emit('added')
    }
  } catch {
    saveError.value = '无法添加站点，请重试。'
  } finally {
    saving.value = false
  }
}

function preventCloseWhileSaving(event: Event) {
  if (saving.value) event.preventDefault()
}
</script>

<template>
  <button
    type="button"
    :disabled="disabled"
    class="rounded-lg bg-teal-800 px-4 py-2 text-sm font-medium text-white hover:bg-teal-900 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-teal-700 disabled:opacity-50"
    @click="openDialog"
  >
    添加
  </button>

  <dialog
    ref="dialog"
    aria-labelledby="add-station-title"
    class="m-auto w-[calc(100%-2rem)] max-w-lg overscroll-contain rounded-2xl border border-slate-200 bg-white p-6 text-slate-900 shadow-xl backdrop:overscroll-contain backdrop:bg-slate-950/50"
    @cancel="preventCloseWhileSaving"
  >
    <h2 id="add-station-title" class="text-lg font-semibold">添加站点</h2>
    <p class="mt-1 text-sm text-slate-600">从站点目录中选择一个站点添加到列表。</p>

    <form class="mt-5 space-y-4" @submit.prevent="addStation">
      <div>
        <label for="candidate-query" class="mb-1 block text-sm font-medium">筛选目录</label>
        <input
          id="candidate-query"
          v-model="query"
          name="query"
          type="search"
          autofocus
          :disabled="saving"
          placeholder="输入站点代码或描述"
          class="min-h-12 w-full rounded-lg border border-slate-400 px-3 focus-visible:outline-2 focus-visible:outline-teal-700"
          @input="clearSelection"
        />
      </div>
      <div v-if="loadFailed" class="text-sm">
        <p role="alert" class="text-red-800">无法加载站点目录。</p>
        <button
          type="button"
          class="mt-2 font-medium text-teal-800 underline"
          @click="loadCandidates"
        >
          重新加载
        </button>
      </div>
      <p v-else-if="loading" role="status" class="text-sm text-slate-600">正在加载站点目录…</p>
      <p v-else-if="!visibleCandidates.length" role="status" class="text-sm text-slate-600">
        没有匹配的站点
      </p>
      <div v-else>
        <label for="station-candidate" class="mb-1 block text-sm font-medium">选择站点</label>
        <select
          id="station-candidate"
          v-model="selection"
          name="code"
          required
          :disabled="saving"
          class="min-h-12 w-full rounded-lg border border-slate-400 bg-white px-3 focus-visible:outline-2 focus-visible:outline-teal-700"
          @change="saveError = ''"
        >
          <option disabled value="">请选择一个站点</option>
          <option
            v-for="candidate in visibleCandidates"
            :key="candidate.code"
            :value="candidate.code"
          >
            {{
              candidate.description
                ? `${candidate.code} · ${candidate.description}`
                : candidate.code
            }}
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
          :disabled="loading || saving || loadFailed || !visibleCandidates.length"
          class="min-h-12 rounded-lg bg-teal-800 px-4 text-sm font-medium text-white hover:bg-teal-900 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-teal-700 disabled:opacity-50"
        >
          {{ saving ? '正在添加…' : '确认添加' }}
        </button>
      </div>
    </form>
  </dialog>
</template>
