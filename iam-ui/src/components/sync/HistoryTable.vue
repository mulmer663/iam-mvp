<script setup lang="ts">
import {Table, TableBody, TableCell, TableHead, TableHeader, TableRow} from '@/components/ui/table'
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
  <Table class="border">
      <TableHeader class="bg-neutral-50">
        <TableRow class="h-7 hover:bg-transparent">
            <TableHead class="text-[10px] uppercase font-bold p-2">Trace ID</TableHead>
            <TableHead class="text-[10px] uppercase font-bold p-2">User</TableHead>
            <TableHead class="text-[10px] uppercase font-bold p-2">Sync Type</TableHead>
            <TableHead class="text-[10px] uppercase font-bold p-2">
              {{ type === 'SOURCE' ? 'Source' : (type === 'AUDIT' ? 'Target' : 'Target System') }}
            </TableHead>
            <TableHead class="text-[10px] uppercase font-bold p-2 text-center">Status</TableHead>
            <TableHead class="text-[10px] uppercase font-bold p-2 text-right">Timestamp</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        <TableRow 
          v-for="log in history" 
          :key="log.id" 
          @click="$emit('rowClick', log)"
          class="h-8 transition-colors cursor-pointer group border-l-2 border-transparent"
          :class="[
            log.id === selectedId 
              ? (type === 'INTEGRATION' 
                  ? 'bg-purple-50/50 border-l-purple-500 hover:bg-purple-50' 
                  : 'bg-blue-50/50 border-l-blue-500 hover:bg-blue-50')
              : 'hover:bg-neutral-50'
          ]"
        >
            <TableCell class="p-2 py-1 font-mono text-[10px] text-neutral-400">
              <div class="flex items-center gap-1">
                  <span>{{ log.traceId }}</span>
                  <ExternalLink class="size-2 hidden group-hover:block" />
              </div>
            </TableCell>
            <TableCell class="p-2 py-1 text-[11px] font-medium text-neutral-600">
                {{ log.target }}
            </TableCell>
            <TableCell class="p-2 py-1">
              <OperationBadge v-if="log.syncType" :type="log.syncType" />
              <span v-else class="text-[9px] text-neutral-300">-</span>
            </TableCell>
            <TableCell class="p-2 py-1 text-[11px] font-medium text-neutral-700">
              <span v-if="type === 'SOURCE'">{{ log.sourceSystem || 'HR' }}</span>
              <span v-else-if="type === 'INTEGRATION'">{{ log.targetSystem || 'Target System' }}</span>
              <span v-else>{{ log.target }}</span> 
            </TableCell>
            <TableCell class="p-2 py-1 text-center">
              <StatusBadge :status="log.status" />
            </TableCell>
            <TableCell class="p-2 py-1 text-right text-[10px] text-neutral-400">{{ formatDateTime(log.time) }}</TableCell>
        </TableRow>
      </TableBody>
  </Table>
</template>
