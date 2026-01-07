<script setup lang="ts">
import { HistoryService } from '@/api/HistoryService'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { Badge } from '@/components/ui/badge'
import { ExternalLink } from 'lucide-vue-next'
import { computed, onMounted, ref } from 'vue'
import type { HistoryLog } from '@/types'
import { useMillerStore } from '@/stores/miller'
import StatusBadge from '@/components/common/StatusBadge.vue'
import OperationBadge from '@/components/common/OperationBadge.vue'
import { formatDateTime } from '@/utils/date'

import { SYSTEM_THEMES } from '@/utils/theme'

const props = defineProps<{
  type: 'SOURCE' | 'INTEGRATION'
  userId?: string
  userName?: string
  paneIndex?: number
}>()

const millerStore = useMillerStore()
const history = ref<HistoryLog[]>([])
const currentTheme = computed(() => SYSTEM_THEMES[props.type])

onMounted(async () => {
    try {
        history.value = await HistoryService.getHistory({ 
            userId: props.userId, 
            userName: props.userName 
        })
    } catch (e) {
        console.error('Failed to load history', e)
    }
})

const filteredHistory = computed((): HistoryLog[] => {
  let baseList = history.value
  
  if (props.type === 'SOURCE') {
    return baseList.filter(h => h.syncDirection === 'RECON')
  }
  return baseList.filter(h => h.syncDirection === 'PROV')
})


function onRowClick(log: HistoryLog) {
  const detailPane = {
    id: `sync-detail-${log.id}`,
    type: 'SyncDetail',
    title: `Event: ${log.traceId}`,
    data: { event: log }
  }

  if (typeof props.paneIndex === 'number') {
    millerStore.setPane(props.paneIndex + 1, detailPane)
  } else {
    millerStore.pushPane(detailPane)
  }
}
</script>

<template>
  <div class="h-full flex flex-col">
    <div class="h-10 border-b border-neutral-100 flex items-center px-4 bg-white shrink-0 justify-between">
       <div class="flex items-center gap-2">
         <component :is="currentTheme.icon" class="size-4" :class="currentTheme.text" />
         <h2 class="text-sm font-bold text-neutral-800">
           {{ currentTheme.label }}
         </h2>
         <Badge variant="outline" class="h-4 text-[9px] uppercase">{{ type }}</Badge>
       </div>
    </div>
    <div class="flex-1 p-3 overflow-auto">
       <Table class="border">
          <TableHeader class="bg-neutral-50">
             <TableRow class="h-7 hover:bg-transparent">
                <TableHead class="text-[10px] uppercase font-bold p-2">Trace ID</TableHead>
                 <TableHead class="text-[10px] uppercase font-bold p-2">User</TableHead>
                 <TableHead class="text-[10px] uppercase font-bold p-2">Sync Type</TableHead>
                 <TableHead class="text-[10px] uppercase font-bold p-2">
                   {{ type === 'SOURCE' ? 'Source' : 'Target' }}
                </TableHead>
                <TableHead class="text-[10px] uppercase font-bold p-2 text-center">Status</TableHead>
                <TableHead class="text-[10px] uppercase font-bold p-2 text-right">Timestamp</TableHead>
             </TableRow>
          </TableHeader>
          <TableBody>
             <TableRow 
               v-for="log in filteredHistory" 
               :key="log.id" 
               @click="onRowClick(log)"
               class="h-8 hover:bg-neutral-50 transition-colors cursor-pointer group"
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
                   {{ type === 'SOURCE' ? (log.sourceSystem || 'HR') : (log.targetSystem || 'Target System') }}
                </TableCell>
                <TableCell class="p-2 py-1 text-center">
                   <StatusBadge :status="log.status" />
                </TableCell>
                <TableCell class="p-2 py-1 text-right text-[10px] text-neutral-400">{{ formatDateTime(log.time) }}</TableCell>
             </TableRow>
          </TableBody>
       </Table>
    </div>
  </div>
</template>
