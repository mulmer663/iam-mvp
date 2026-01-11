<script setup lang="ts">
import {Badge} from '@/components/ui/badge'
import {formatDateTime} from '@/utils/date'
import type {HistoryLog} from '@/types'
import {useMillerStore} from '@/stores/miller'
import {SYSTEM_THEMES} from '@/utils/theme'
import {HistoryService} from '@/api/HistoryService'

const props = defineProps<{
  event: HistoryLog
  history: HistoryLog[]
  paneIndex?: number
}>()

const millerStore = useMillerStore()

function openRelatedEvent(log: HistoryLog) {
  const targetPaneId = `syncdetail-${log.id}`
  const existingPane = millerStore.panes.find(p => p.id === targetPaneId)
  
  if (existingPane) {
    millerStore.highlightPane(targetPaneId)
    millerStore.activePaneId = targetPaneId
    return
  }

  const nextPaneData = {
    id: targetPaneId,
    type: 'SyncDetail',
    title: `Event: ${log.traceId}`,
    data: { event: log }
  }

  if (props.paneIndex !== undefined) {
    millerStore.setPane(props.paneIndex + 1, nextPaneData)
  } else {
    millerStore.pushPane(nextPaneData)
  }
}

async function openModificationLedger() {
  const userId = props.event.userId || props.event.resultData?.id || props.event.requestPayload?.id || (props.event.payload as any)?.id
  const traceId = props.event.traceId

  // 1. Check if the Modification Ledger for this user/trace is already open
  const targetLedgerId = `userchangehistory-${traceId || userId || 'global'}`
  const existingLedger = millerStore.panes.find(p => p.id === targetLedgerId)
  if (existingLedger) {
    millerStore.highlightPane(targetLedgerId)
    millerStore.activePaneId = targetLedgerId
    return
  }

  try {
    const response = await HistoryService.getUserRevisionHistory({ traceId, size: 1 })
    
    if (response.content && response.content.length > 0) {
      const rev = response.content[0]
      if (rev) {
        const detailPaneId = `syncdetail-${rev.revId}`
        const existingDetail = millerStore.panes.find(p => p.id === detailPaneId)
        if (existingDetail) {
           millerStore.highlightPane(detailPaneId)
           millerStore.activePaneId = detailPaneId
           return
        }

        const syntheticEvent: HistoryLog = {
          id: String(rev.revId),
          traceId: rev.traceId,
          eventType: 'USER_UPDATE', // Triggers SYSTEM_THEMES.AUDIT
          status: 'SUCCESS',
          target: rev.profile.userName,
          time: rev.timestamp,
          syncType: rev.operationType as any,
          snapshot: { layer: 'IAM', data: rev.profile },
          payload: rev.profile,
          // operator: rev.operatorId
        }

        const detailPane = {
          id: detailPaneId,
          type: 'SyncDetail',
          title: `Event: ${rev.traceId}`,
          data: { event: syntheticEvent }
        }

        if (props.paneIndex !== undefined) {
          millerStore.setPane(props.paneIndex + 1, detailPane)
        } else {
          millerStore.pushPane(detailPane)
        }
        return
      }
    }
  } catch (e) {
    console.error('Failed to fetch specific revision detail', e)
  }

  // Fallback to ledger list
  const nextPaneData = {
    id: targetLedgerId,
    type: 'UserChangeHistory',
    title: `${SYSTEM_THEMES.AUDIT.label}: ${traceId}`,
    data: { userId, traceId }
  }

  if (props.paneIndex !== undefined) {
    millerStore.setPane(props.paneIndex + 1, nextPaneData)
  } else {
    millerStore.pushPane(nextPaneData)
  }
}
</script>

<template>
  <section v-if="history.length > 0">
    <h3 class="text-[10px] font-black text-neutral-400 uppercase tracking-widest mb-4 flex items-center gap-2">
      Sync Pipeline
    </h3>
    
    <div class="flex flex-col gap-6 relative">
      <!-- Connection Lines (Visual) -->
      <div class="absolute left-3 top-3 bottom-3 w-px bg-neutral-100 -z-0"></div>

      <!-- Step 1: Source -->
      <div class="relative z-10 flex flex-col gap-2">
        <div class="flex items-center gap-2 mb-1">
          <div class="size-6 rounded-full bg-blue-50 border border-blue-100 flex items-center justify-center text-[10px] font-bold text-blue-600 shadow-sm">1</div>
          <span class="text-[10px] font-black text-neutral-500 uppercase tracking-tighter">Source Systems</span>
        </div>
        <div class="ml-8 space-y-1">
          <div v-for="h in history.filter(x => x.traceId === event.traceId && x.syncDirection === 'RECON')" :key="h.id"
            @click="openRelatedEvent(h)"
            class="p-2 border rounded-md text-[11px] cursor-pointer transition-all flex items-center justify-between group shadow-sm bg-white"
            :class="h.id === event.id ? 'border-blue-200 ring-2 ring-blue-100' : 'border-neutral-100 hover:border-blue-200'"
          >
            <div class="flex flex-col flex-1">
              <span class="font-bold text-neutral-700">{{ h.target || 'HR System' }}</span>
              <span class="text-[9px] text-neutral-400 font-mono">{{ formatDateTime(h.time) }}</span>
            </div>
            <div class="flex items-center shrink-0">
              <div class="size-1.5 rounded-full" :class="h.status === 'SUCCESS' ? 'bg-emerald-500' : 'bg-red-500'"></div>
            </div>
          </div>
        </div>
      </div>

      <!-- Step 2: Core -->
      <div class="relative z-10 flex flex-col gap-2">
        <div class="flex items-center gap-2 mb-1">
          <div class="size-6 rounded-full bg-orange-50 border border-orange-100 flex items-center justify-center text-[10px] font-bold text-orange-600 shadow-sm">2</div>
          <span class="text-[10px] font-black text-neutral-500 uppercase tracking-tighter">IAM Core</span>
        </div>
        <div class="ml-8 space-y-1">
          <!-- Case A: Explicit Identity Sync Event Found -->
          <div 
            v-for="h in history.filter(x => x.traceId === event.traceId && x.syncDirection !== 'RECON' && x.syncDirection !== 'PROV')" :key="h.id"
            @click="openRelatedEvent(h)"
            class="p-2 border rounded-md text-[11px] cursor-pointer transition-all flex items-center justify-between group shadow-sm bg-white"
            :class="h.id === event.id ? 'border-orange-200 ring-2 ring-orange-100' : 'border-neutral-100 hover:border-orange-200'"
          >
            <div class="flex flex-col flex-1">
              <span class="font-bold text-neutral-700">{{ h.target || 'IAM Core' }}</span>
              <span class="text-[9px] text-neutral-400 font-mono">{{ formatDateTime(h.time) }}</span>
            </div>
            <div class="flex items-center shrink-0">
              <div class="size-1.5 rounded-full" :class="h.status === 'SUCCESS' ? 'bg-emerald-500' : 'bg-red-500'"></div>
            </div>
          </div>

          <!-- Case B: No explicit event, but we can look at Modification Ledger (Revision History) -->
          <div 
            v-if="history.filter(x => x.traceId === event.traceId && x.syncDirection !== 'RECON' && x.syncDirection !== 'PROV').length === 0"
            @click="openModificationLedger()"
            class="p-2 border bg-white rounded-md text-[11px] cursor-pointer transition-all flex items-center justify-between group overflow-hidden shadow-sm"
            :class="(!event.syncDirection || (event.syncDirection !== 'RECON' && event.syncDirection !== 'PROV')) ? 'border-orange-200 ring-2 ring-orange-100' : 'border-neutral-100 hover:border-orange-200'"
          >
            <div class="flex flex-col flex-1">
              <span class="font-bold text-neutral-700">{{ event.target || 'IAM Core' }}</span>
              <span class="text-[9px] text-neutral-400 font-mono">{{ formatDateTime(event.time) }}</span>
            </div>
            <div class="flex items-center shrink-0">
              <div class="size-1.5 rounded-full bg-emerald-500"></div>
            </div>
          </div>
        </div>
      </div>

      <!-- Step 3: Targets -->
      <div class="relative z-10 flex flex-col gap-2">
        <div class="flex items-center gap-2 mb-1">
          <div class="size-6 rounded-full bg-purple-50 border border-purple-100 flex items-center justify-center text-[10px] font-bold text-purple-600 shadow-sm">3</div>
          <span class="text-[10px] font-black text-neutral-500 uppercase tracking-tighter">Target Provisioning</span>
          <Badge variant="outline" class="h-3 text-[8px] border-purple-200 text-purple-600">{{ history.filter(x => x.traceId === event.traceId && x.syncDirection === 'PROV').length }} Targets</Badge>
        </div>
        <div class="ml-8">
          <div class="grid grid-cols-2 gap-2">
            <div 
              v-for="h in history.filter(x => x.traceId === event.traceId && x.syncDirection === 'PROV')" :key="h.id"
              @click="openRelatedEvent(h)"
              class="p-2 border rounded-md text-[10px] cursor-pointer transition-all flex flex-col gap-1 group bg-white shadow-sm"
              :class="h.id === event.id ? 'border-purple-200 ring-2 ring-purple-100' : 'border-neutral-100 hover:border-purple-200'"
            >
              <div class="flex items-center justify-between">
                <span class="font-black text-neutral-500 uppercase tracking-tighter truncate max-w-[70%]">
                  {{ h.target || 'Target System' }}
                </span>
                <div class="size-1.5 rounded-full" :class="h.status === 'SUCCESS' ? 'bg-emerald-500' : 'bg-red-500'"></div>
              </div>
              <div class="text-[8px] text-neutral-400 font-mono">{{ formatDateTime(h.time)?.split(' ')[1] }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>
