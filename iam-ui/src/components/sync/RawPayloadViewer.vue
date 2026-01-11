<script setup lang="ts">
import {ref} from 'vue'
import {ChevronDown, ChevronRight, Code} from 'lucide-vue-next'
import type {HistoryLog} from '@/types'

const props = defineProps<{
  event: HistoryLog
}>()

const isOpen = ref(false)
</script>

<template>
  <section class="mt-auto pt-4">
    <button 
      @click="isOpen = !isOpen"
      class="flex items-center justify-between w-full p-2 bg-neutral-50 rounded-t-md text-neutral-400 font-mono text-[10px] hover:text-neutral-600 hover:bg-neutral-100 transition-colors border-x border-t border-neutral-200"
      :class="{ 'rounded-md border-b': !isOpen }"
    >
      <div class="flex items-center gap-2">
         <Code class="size-3" /> <span>RAW PAYLOAD AUDIT</span>
      </div>
      <component :is="isOpen ? ChevronDown : ChevronRight" class="size-3" />
    </button>
    <div v-if="isOpen" class="p-3 bg-neutral-50 rounded-b-md text-neutral-500 font-mono text-[10px] border-x border-b border-neutral-200 flex flex-col gap-3">
      <div v-if="event.requestPayload">
         <pre class="overflow-x-auto whitespace-pre-wrap break-all">{{ JSON.stringify(event.requestPayload, null, 2) }}</pre>
      </div>
      <div v-if="event.resultData && Object.keys(event.resultData).some(k => !['status', 'syncType', 'target'].includes(k))">
         <pre class="overflow-x-auto whitespace-pre-wrap break-all">{{ JSON.stringify(event.resultData, null, 2) }}</pre>
      </div>
      <div v-if="!event.requestPayload && !event.resultData">
         <pre class="overflow-x-auto whitespace-pre-wrap break-all">{{ JSON.stringify(event.payload || event, null, 2) }}</pre>
      </div>
    </div>
  </section>
</template>
