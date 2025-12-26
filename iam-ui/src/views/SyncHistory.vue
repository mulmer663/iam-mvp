<script setup lang="ts">
import { MOCK_HISTORY } from '@/mocks/data'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { Badge } from '@/components/ui/badge'
import { ExternalLink } from 'lucide-vue-next'
import { computed } from 'vue'
import type { HistoryLog } from '@/types'
import { useMillerStore } from '@/stores/miller'

import { SYSTEM_THEMES } from '@/utils/theme'

const props = defineProps<{
  type: 'SOURCE' | 'INTEGRATION' | 'AUDIT'
  userId?: string
  paneIndex?: number
}>()

const millerStore = useMillerStore()
const currentTheme = computed(() => SYSTEM_THEMES[props.type])

const filteredHistory = computed((): HistoryLog[] => {
  let baseList = MOCK_HISTORY as HistoryLog[]
  
  if (props.userId) {
    baseList = baseList.filter(h => h.userId === props.userId)
  }

  if (props.type === 'AUDIT') return baseList.filter(h => h.type === 'USER_UPDATE')
  if (props.type === 'SOURCE') return baseList.filter(h => h.type === 'HR_SYNC')
  return baseList.filter(h => h.type === 'AD_PROVISION')
})


const getStatusVariant = (status: string) => {
  if (status === 'SUCCESS') return 'default'
  if (status === 'PENDING') return 'secondary'
  return 'destructive'
}

const getSyncTypeVariant = (item: HistoryLog) => {
  if (item.syncType === 'JOIN') return 'default'
  if (item.syncType === 'UPDATE_CRITICAL') return 'secondary' // Toned down
  return 'outline'
}

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
               <TableHead class="text-[10px] uppercase font-bold p-2">Sync Type</TableHead>
               <TableHead class="text-[10px] uppercase font-bold p-2">Target</TableHead>
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
               <TableCell class="p-2 py-1">
                 <Badge 
                   v-if="log.syncType" 
                   :variant="getSyncTypeVariant(log)" 
                   class="h-4 px-1 text-[8px] uppercase tracking-tighter"
                   :class="{ 'text-amber-600 bg-amber-50 border-amber-200': log.syncType === 'UPDATE_CRITICAL' }"
                 >
                   {{ log.syncType }}
                 </Badge>
                 <span v-else class="text-[9px] text-neutral-300">-</span>
               </TableCell>
               <TableCell class="p-2 py-1 text-[11px] font-medium text-neutral-700">{{ log.target }}</TableCell>
               <TableCell class="p-2 py-1 text-center">
                  <Badge :variant="getStatusVariant(log.status)" class="px-1 py-0 h-4 text-[9px] font-bold rounded-sm">
                     {{ log.status }}
                  </Badge>
               </TableCell>
               <TableCell class="p-2 py-1 text-right text-[10px] text-neutral-400">{{ log.time }}</TableCell>
            </TableRow>
         </TableBody>
       </Table>
    </div>
  </div>
</template>
