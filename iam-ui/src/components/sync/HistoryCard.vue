<script setup lang="ts">
import {ExternalLink} from 'lucide-vue-next'
import StatusBadge from '@/components/common/StatusBadge.vue'
import OperationBadge from '@/components/common/OperationBadge.vue'
import {formatDateTime} from '@/utils/date'
import type {HistoryLog} from '@/types'

defineProps<{
  history: HistoryLog[]
  type?: 'SOURCE' | 'INTEGRATION' | 'AUDIT'
  selectedId?: string
}>()

defineEmits<{
  (e: 'rowClick', log: HistoryLog): void
}>()
</script>

<template>
  <div class="space-y-2">
    <div 
      v-for="log in history" 
      :key="log.id" 
      @click="$emit('rowClick', log)"
      class="p-3 border rounded-md transition-all cursor-pointer group flex flex-col gap-2 relative overflow-hidden"
      :class="[
         log.id === selectedId
           ? (type === 'INTEGRATION'
               ? 'bg-purple-50 border-purple-300 shadow-md ring-1 ring-purple-200'
               : 'bg-blue-50 border-blue-300 shadow-md ring-1 ring-blue-200')
           : 'bg-white hover:border-blue-300 hover:shadow-sm'
      ]"
    >
      <div v-if="log.id === selectedId" class="absolute left-0 top-0 bottom-0 w-1" :class="type === 'INTEGRATION' ? 'bg-purple-500' : 'bg-blue-500'"></div>
      <!-- Header: Trace ID & Time -->
      <div class="flex items-center justify-between">
         <div class="flex items-center gap-1.5">
            <span class="font-mono text-[10px] text-neutral-400 bg-neutral-50 px-1 py-0.5 rounded">{{ log.traceId }}</span>
            <ExternalLink class="size-3 text-neutral-300 hidden group-hover:block" />
         </div>
         <span class="text-[10px] text-neutral-400 font-mono">{{ formatDateTime(log.time) }}</span>
      </div>

      <!-- Main: Target Info -->
      <div class="flex items-center justify-between">
         <div class="flex flex-col">
            <span class="text-[11px] font-bold text-neutral-800">{{ log.target }}</span>
            <span class="text-[10px] text-neutral-500">
               <span v-if="type === 'SOURCE'">{{ log.sourceSystem || 'HR' }}</span>
               <span v-else-if="type === 'INTEGRATION'">{{ log.targetSystem || 'Target System' }}</span>
               <span v-else>{{ log.target }}</span>
            </span>
         </div>
         <StatusBadge :status="log.status" />
      </div>

      <!-- Footer: Operation Badge -->
      <div class="flex items-center">
         <OperationBadge v-if="log.syncType" :type="log.syncType" />
         <span v-else class="text-[9px] text-neutral-300">-</span>
      </div>
    </div>
  </div>
</template>
