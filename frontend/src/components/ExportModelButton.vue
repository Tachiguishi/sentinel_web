<script setup lang="ts">
import { onBeforeUnmount, ref, watch } from 'vue'

const props = defineProps<{ name: string | null; disabled?: boolean }>()
const exporting = ref(false)
const error = ref('')
const message = ref('')
let request: AbortController | undefined

function cancelExport() {
  request?.abort()
  request = undefined
  exporting.value = false
  error.value = ''
  message.value = ''
}

watch(() => props.name, cancelExport)
onBeforeUnmount(cancelExport)

async function exportModel() {
  if (!props.name || props.disabled || exporting.value) return
  const name = props.name
  const current = new AbortController()
  request = current
  exporting.value = true
  error.value = ''
  message.value = ''
  let failure = '模型导出失败，请重试。'
  try {
    const response = await fetch(`/api/stations/model?${new URLSearchParams({ name })}`, {
      signal: current.signal,
    })
    if (!response.ok) {
      failure =
        response.status === 409
          ? '请先更新该站点的 SCD 文件。'
          : response.status === 404
            ? '站点不存在，请刷新页面。'
            : '模型导出失败，请重试。'
      throw new Error(failure)
    }
    const model = await response.blob()
    if (current.signal.aborted) return
    const url = URL.createObjectURL(model)
    const link = document.createElement('a')
    link.href = url
    link.download = `${name.replace(/[\u0000-\u001f\u007f-\u009f\\/:*?"<>|]/g, '_')}.xml`
    document.body.append(link)
    link.click()
    link.remove()
    // Give the browser time to start reading the download before releasing its URL.
    setTimeout(() => URL.revokeObjectURL(url), 1000)
    message.value = '模型已生成，已开始下载。'
  } catch {
    if (!current.signal.aborted) {
      error.value = failure
    }
  } finally {
    if (request === current) {
      request = undefined
      exporting.value = false
    }
  }
}
</script>

<template>
  <div>
    <button
      type="button"
      :disabled="!name || disabled || exporting"
      :aria-busy="exporting"
      class="rounded-lg bg-teal-800 px-4 py-2 text-sm font-medium text-white hover:bg-teal-900 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-teal-700 disabled:opacity-50"
      @click="exportModel"
    >
      {{ exporting ? '正在导出…' : '导出模型' }}
    </button>
    <p v-if="error" role="alert" class="mt-2 text-sm text-red-800">{{ error }}</p>
    <p role="status" class="mt-2 text-sm text-teal-800">{{ message }}</p>
  </div>
</template>
