<script setup lang="ts">
import { HistoryService } from '@/api/HistoryService'
import { History, Clock, ExternalLink } from 'lucide-vue-next'
import { onMounted, ref } from 'vue'
import type { UserRevisionHistory } from '@/types'
import { Badge } from '@/components/ui/badge'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { useMillerStore } from '@/stores/miller'
import { formatDateTime } from '@/utils/date'
import OperationBadge from '@/components/common/OperationBadge.vue'

const props = defineProps<{
  userId?: string
  traceId?: string
  paneIndex?: number
}>()

const millerStore = useMillerStore()
const revisions = ref<UserRevisionHistory[]>([])
const loading = ref(false)

onMounted(async () => {
    loading.value = true
    try {
        const response = await HistoryService.getUserRevisionHistory({ 
            userId: props.userId,
            traceId: props.traceId
        })
        revisions.value = response.content
    } catch (e) {
        console.error('Failed to load revision history', e)
    } finally {
        loading.value = false
    }
})

function openSnapshotDetail(rev: UserRevisionHistory) {
  // Map UserRevisionHistory to HistoryLog structure for SyncDetail compatibility
  const syntheticEvent = {
    id: rev.revId,
    traceId: rev.traceId,
    eventType: 'USER_UPDATE', // Triggers SYSTEM_THEMES.AUDIT (Orange)
    status: 'SUCCESS',
    target: rev.profile.userName,
    time: rev.timestamp,
    syncType: rev.operationType,
    snapshot: { data: rev.profile },
    payload: rev.profile,
    operator: rev.operatorId
  }

  const detailPane = {
    id: `rev-detail-${rev.revId}`,
    type: 'SyncDetail',
    title: `Event: ${rev.traceId}`,
    data: { event: syntheticEvent }
  }

  if (typeof props.paneIndex === 'number') {
    millerStore.setPane(props.paneIndex + 1, detailPane)
  } else {
    millerStore.pushPane(detailPane)
  }
}


</script>

<template>
  <div class="h-full flex flex-col bg-white">
    <!-- Header -->
    <div class="h-10 border-b border-neutral-100 flex items-center px-4 bg-white shrink-0 justify-between">
      <div class="flex items-center gap-2">
        <History class="size-4 text-amber-600" />
        <h2 class="text-sm font-bold text-neutral-800 uppercase">Modification Ledger</h2>
        <Badge v-if="userId || traceId" variant="outline" class="h-4 text-[9px] font-mono lowercase">
          {{ userId || traceId }}
        </Badge>
      </div>
    </div>
    
    <!-- Table Content -->
    <div class="flex-1 p-3 overflow-auto">
      <div v-if="loading" class="h-full flex flex-col items-center justify-center gap-2">
         <Clock class="size-5 text-neutral-200 animate-pulse" />
         <span class="text-[10px] text-neutral-400">Loading ledger...</span>
      </div>
      
      <Table v-else class="border">
        <TableHeader class="bg-neutral-50">
          <TableRow class="h-7 hover:bg-transparent">
            <TableHead class="text-[10px] uppercase font-bold p-2 text-center w-24">Rev</TableHead>
            <TableHead class="text-[10px] uppercase font-bold p-2">Trace ID</TableHead>
            <TableHead class="text-[10px] uppercase font-bold p-2">Operator</TableHead>
            <TableHead class="text-[10px] uppercase font-bold p-2 text-right">Timestamp</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          <TableRow 
            v-for="rev in revisions" 
            :key="rev.revId" 
            @click="openSnapshotDetail(rev)"
            class="h-8 hover:bg-neutral-50 transition-colors cursor-pointer group"
          >
            <TableCell class="p-2 py-1 text-center">
               <div class="flex items-center justify-center gap-2">
                 <span class="font-mono text-[10px] text-neutral-400">#{{ rev.revId }}</span>
                 <OperationBadge :type="rev.operationType" />
               </div>
            </TableCell>
            <TableCell class="p-2 py-1 font-mono text-[10px] text-neutral-400">
               <div class="flex items-center gap-1">
                  <span>{{ rev.traceId }}</span>
                  <ExternalLink class="size-2 hidden group-hover:block" />
               </div>
            </TableCell>
            <TableCell class="p-2 py-1 text-[11px] font-medium text-neutral-600">
               {{ rev.operatorId }}
            </TableCell>
            <TableCell class="p-2 py-1 text-right text-[10px] text-neutral-400 font-mono">
              {{ formatDateTime(rev.timestamp) }}
            </TableCell>
          </TableRow>
        </TableBody>
      </Table>

      <div v-if="!loading && revisions.length === 0" class="py-20 text-center text-neutral-300 italic text-[11px]">
        No audit events found for current selection.
      </div>
    </div>
    
    <!-- Footer -->
    <div class="h-6 border-t border-neutral-100 bg-neutral-50/50 flex items-center px-3 justify-between shrink-0">
      <div class="text-[9px] text-neutral-400 italic">Source: Hibernate Envers Audit Logs</div>
      <div class="text-[9px] font-bold text-neutral-500 uppercase">{{ revisions.length }} Revisions</div>
    </div>
  </div>
</template>
