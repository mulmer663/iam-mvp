<script setup lang="ts">
import {computed, onMounted, onUnmounted, ref} from 'vue'
import {HistoryService} from '@/api/HistoryService'
import {SYSTEM_THEMES} from '@/utils/theme'
import {formatDateTime} from '@/utils/date'
import type {HistoryLog} from '@/types'
import OperationBadge from '@/components/common/OperationBadge.vue'
import UserProfileViewer from '@/components/common/UserProfileViewer.vue'
import AttributeMappingTable from '@/components/sync/AttributeMappingTable.vue'
import SyncPipeline from '@/components/sync/SyncPipeline.vue'
import RawPayloadViewer from '@/components/sync/RawPayloadViewer.vue'
import {Separator} from '@/components/ui/separator'

const props = defineProps<{
    event: HistoryLog
    paneIndex?: number
}>()

const allHistory = ref<HistoryLog[]>([])
// historicalMappings is kept for the AttributeMappingTable
const historicalMappings = ref<any[]>([])
const loadingMappings = ref(false)

const isMounted = ref(false)
onMounted(async () => {
    isMounted.value = true
    
    // 1. Fetch related history for the pipeline
    try {
        const data = await HistoryService.getHistory({ 
            userId: props.event.userId || (props.event.resultData?.id as string) 
        })
        if (isMounted.value) {
            allHistory.value = data
        }
    } catch (e) {
        if (isMounted.value) {
            console.error('Failed to load history for related events', e)
        }
    }

    // 2. Fetch historical mappings
    const systemId = props.event.sourceSystem || props.event.targetSystem
    if (systemId && props.event.ruleRevId && (!props.event.mappings || props.event.mappings.length === 0)) {
        loadingMappings.value = true
        try {
            const mappings = await HistoryService.getRuleMappingHistory(systemId, props.event.ruleRevId)
            if (isMounted.value) {
                historicalMappings.value = mappings
            }
        } catch (e) {
            console.error('Failed to load historical mappings', e)
        } finally {
            if (isMounted.value) {
                loadingMappings.value = false
            }
        }
    }
})

onUnmounted(() => {
    isMounted.value = false
})

const theme = computed(() => {
  if (props.event.syncDirection === 'RECON') return SYSTEM_THEMES.SOURCE
  if (props.event.syncDirection === 'PROV') return SYSTEM_THEMES.INTEGRATION
  return SYSTEM_THEMES.AUDIT
})

const snapshotTitle = computed(() => theme.value.subLabel + ' Snapshot')

const snapshotData = computed(() => {
    // Pass the raw data object, UserProfileViewer will unwrap common wrappers like event.payload if needed
    // But we should prioritize specific fields logic from previous implementation
    return props.event.requestPayload || props.event.snapshot?.data || props.event.payload
})

const mappingLabels = computed(() => {
  if (!props.event.mappings || props.event.mappings.length === 0) return { from: 'Source', to: 'Target' }
  const first = props.event.mappings[0]
  if (first && first.fromLabel && first.toLabel) return { from: first.fromLabel, to: first.toLabel }
  if (props.event.syncDirection === 'RECON') return { from: 'HR', to: 'IAM' }
  if (props.event.syncDirection === 'PROV') return { from: 'IAM', to: 'AD' }
  return { from: 'Source', to: 'Target' }
})

const mappings = computed(() => {
    return (props.event.mappings && props.event.mappings.length > 0) ? props.event.mappings : historicalMappings.value
})
</script>

<template>
  <div class="h-full flex flex-col bg-white">
    <!-- Layered Header -->
    <div class="p-4 border-b flex flex-col gap-3" :class="[theme.bg, theme.border]">
      <div class="flex items-center justify-between">
        <div class="flex items-center gap-2">
           <component :is="theme.icon" class="size-5" :class="theme.text" />
           <span class="text-sm font-black uppercase tracking-tight" :class="theme.text">
             {{ theme.label }}
           </span>
        </div>
        <div class="text-[10px] font-mono text-neutral-500 bg-white/50 px-2 py-1 rounded">
          TRACE: {{ event.traceId }}
        </div>
      </div>
      <div>
         <div class="text-lg font-bold text-neutral-900 leading-none mb-1">{{ event.target }}</div>
         <div class="flex items-center justify-between">
            <div class="text-[11px] text-neutral-500">{{ formatDateTime(event.time) }}</div>
            <OperationBadge v-if="event.syncType" :type="event.syncType" />
         </div>
      </div>
    </div>

    <div class="flex-1 overflow-y-auto p-4 space-y-6">
      
      <!-- Layer Snapshot -->
      <UserProfileViewer 
        v-if="snapshotData"
        :data="snapshotData" 
        :changes="event.changes" 
        :title="snapshotTitle"
      />

      <!-- Attribution Mapping -->
      <AttributeMappingTable 
        :mappings="mappings"
        :applied-rules="event.appliedRules"
        :loading="loadingMappings"
        :from-label="mappingLabels.from"
        :to-label="mappingLabels.to"
        :is-historical="historicalMappings.length > 0 && (!event.mappings || event.mappings.length === 0)"
      />

      <Separator v-if="allHistory.length > 0" />

      <!-- Sync Pipeline Visualization -->
      <SyncPipeline 
        :event="event" 
        :history="allHistory" 
        :pane-index="paneIndex" 
      />

      <!-- Technical Audit Log -->
      <RawPayloadViewer :event="event" />

    </div>
  </div>
</template>
