<script setup lang="ts">
import {HistoryService} from '@/api/HistoryService'
import {computed, onMounted, onUnmounted, ref} from 'vue'
import type {HistoryLog} from '@/types'
import {useMillerStore} from '@/stores/miller'
import {SYSTEM_THEMES} from '@/utils/theme'
import {Badge} from '@/components/ui/badge'
import HistoryTable from '@/components/sync/HistoryTable.vue'
import HistoryCard from '@/components/sync/HistoryCard.vue'
import {useContainerWidth} from '@/composables/useContainerWidth'

const props = defineProps<{
  type: 'SOURCE' | 'INTEGRATION'
  userId?: string
  userName?: string
  paneIndex?: number
}>()

const millerStore = useMillerStore()
const history = ref<HistoryLog[]>([])
const currentTheme = computed(() => SYSTEM_THEMES[props.type])
const containerRef = ref<HTMLElement | null>(null)
const { containerWidth } = useContainerWidth(containerRef)

const isNarrow = computed(() => containerWidth.value < 600)

const isMounted = ref(false)
onMounted(async () => {
    isMounted.value = true
    try {
        const data = await HistoryService.getHistory({ 
            userId: props.userId, 
            userName: props.userName 
        })
        if (isMounted.value) {
            history.value = data
        }
    } catch (e) {
        if (isMounted.value) {
            console.error('Failed to load history', e)
        }
    }
})

onUnmounted(() => {
    isMounted.value = false
})

const filteredHistory = computed((): HistoryLog[] => {
  let baseList = history.value
  
  if (props.type === 'SOURCE') {
    return baseList.filter(h => h.syncDirection === 'RECON')
  }
  return baseList.filter(h => h.syncDirection === 'PROV')
})


const selectedId = ref<string | undefined>(undefined)

function onRowClick(log: HistoryLog) {
  selectedId.value = log.id

  const detailPaneId = `syncdetail-${log.id}`
  const existingPane = millerStore.panes.find(p => p.id === detailPaneId)
  
  if (existingPane) {
    millerStore.highlightPane(detailPaneId)
    millerStore.activePaneId = detailPaneId
    return
  }

  const detailPane = {
    id: detailPaneId,
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
  <div class="h-full flex flex-col" ref="containerRef">
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
       <HistoryCard 
         v-if="isNarrow" 
         :history="filteredHistory" 
         :type="type" 
         :selectedId="selectedId"
         @row-click="onRowClick" 
       />
       <HistoryTable 
         v-else 
         :history="filteredHistory" 
         :type="type" 
         :selectedId="selectedId"
         @row-click="onRowClick" 
       />
    </div>
  </div>
</template>
