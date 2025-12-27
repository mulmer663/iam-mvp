<script setup lang="ts">
import { ArrowRight, Code, ChevronDown, ChevronRight, Search } from 'lucide-vue-next'
import { Separator } from '@/components/ui/separator'
import type { HistoryLog } from '@/types'
import { HistoryService } from '@/api/HistoryService'
import { computed, onMounted, ref } from 'vue'

import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { SYSTEM_THEMES } from '@/utils/theme'
import { useMillerStore } from '@/stores/miller'

const props = defineProps<{
    event: HistoryLog
    paneIndex?: number
}>()

const millerStore = useMillerStore()
const isRawOpen = ref(false)
const viewMode = ref<'changes' | 'all'>(props.event.changes && props.event.changes.length > 0 ? 'changes' : 'all')
const searchQuery = ref('')
const allHistory = ref<HistoryLog[]>([])

onMounted(async () => {
    try {
        allHistory.value = await HistoryService.getHistory()
    } catch (e) {
        console.error('Failed to load history for related events', e)
    }
})

// relatedEvents is now handled directly in the template within the Sync Pipeline section


const theme = computed(() => {
  if (props.event.type === 'HR_SYNC') return SYSTEM_THEMES.SOURCE
  if (props.event.type === 'USER_UPDATE') return SYSTEM_THEMES.AUDIT
  return SYSTEM_THEMES.INTEGRATION
})

const snapshotTitle = computed(() => theme.value.subLabel)


function openRelatedEvent(log: HistoryLog) {
  const targetPaneId = `sync-detail-${log.id}`
  const existingPane = millerStore.panes.find(p => p.id === targetPaneId)
  
  if (existingPane) {
    millerStore.highlightPane(targetPaneId)
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

// Function to check if a value is a SCIM extension key
const isExtension = (key: string) => key.startsWith('urn:ietf:params:scim:schemas:extension:')

// Helper to get change for a specific field
const getChange = (key: string) => {
  return props.event.changes?.find(c => c.field === key)
}

const allSnapshotEntries = computed(() => {
  if (!props.event.snapshot?.data) return []
  const data = props.event.snapshot.data
  return Object.entries(data).filter(([key]) => key !== 'schemas' && !isExtension(key))
})

const filteredEntries = computed(() => {
  let entries = allSnapshotEntries.value
  
  // 1. Filter by Search Query
  if (searchQuery.value) {
    const q = searchQuery.value.toLowerCase()
    entries = entries.filter(([key, val]) => 
      key.toLowerCase().includes(q) || 
      String(val).toLowerCase().includes(q)
    )
  }

  // 2. Filter by View Mode (only if not searching)
  if (viewMode.value === 'changes' && !searchQuery.value) {
    entries = entries.filter(([key]) => !!getChange(key))
  }

  // 3. Sort: Changed first
  return entries.sort(([keyA], [keyB]) => {
    const changeA = getChange(keyA) ? 1 : 0
    const changeB = getChange(keyB) ? 1 : 0
    return changeB - changeA
  })
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
         <div class="text-[11px] text-neutral-500">{{ event.time }}</div>
      </div>
    </div>

    <div class="flex-1 overflow-y-auto p-4 space-y-6">
      

      <!-- Attribution Mapping (Context Aware 2-Way) -->
      <section v-if="event.mappings && event.mappings.length > 0">
        <h3 class="text-[10px] font-black text-neutral-400 uppercase tracking-widest mb-3 flex items-center gap-2">
          Attribute Mapping ({{ event.mappings?.[0]?.fromLabel }} → {{ event.mappings?.[0]?.toLabel }})
        </h3>
        <div class="border rounded-md overflow-hidden bg-neutral-50/50">
           <Table>
             <TableHeader class="bg-white">
               <TableRow class="h-8 hover:bg-transparent shadow-sm">
                 <TableHead class="text-[9px] font-bold uppercase py-0 px-3 w-[45%]">{{ event.mappings?.[0]?.fromLabel }} Field</TableHead>
                 <TableHead class="text-[9px] font-bold uppercase py-0 px-3 text-center w-[10%]"></TableHead>
                 <TableHead class="text-[9px] font-bold uppercase py-0 px-3 w-[45%]">{{ event.mappings?.[0]?.toLabel }} Field</TableHead>
               </TableRow>
             </TableHeader>
             <TableBody>
                <TableRow v-for="(m, idx) in event.mappings" :key="idx" class="h-9 hover:bg-white border-b-neutral-100 last:border-0">
                  <!-- From Field -->
                  <TableCell class="py-1 px-3 text-[10px] font-mono" 
                             :class="[
                               m.fromLabel === 'HR' ? 'text-blue-600' : '',
                               m.fromLabel === 'IAM' ? 'text-orange-600 font-bold' : ''
                             ]">
                    {{ m.fromField }}
                  </TableCell>

                  <TableCell class="py-1 px-3 text-center"><ArrowRight class="size-2.5 text-neutral-300 mx-auto" /></TableCell>

                  <!-- To Field -->
                  <TableCell class="py-1 px-3 text-[10px] font-mono"
                             :class="[
                               m.toLabel === 'IAM' ? 'text-orange-600 font-bold' : '',
                               m.toLabel !== 'IAM' ? 'text-purple-600 font-bold' : ''
                             ]">
                    {{ m.toField }}
                  </TableCell>
                </TableRow>
             </TableBody>
           </Table>
        </div>
      </section>

      <!-- Layer Snapshot (SCIM Aware) -->
      <section v-if="event.snapshot">
        <div class="flex flex-col gap-3 mb-4">
           <div class="flex items-center justify-between">
              <div class="flex items-center gap-2">
                 <h3 class="text-[10px] font-black text-neutral-400 uppercase tracking-widest">
                   {{ snapshotTitle }} Snapshot
                 </h3>
                 <span class="px-1.5 py-0.5 bg-neutral-100 text-[8px] font-bold text-neutral-500 rounded-sm">
                   {{ filteredEntries.length }} / {{ allSnapshotEntries.length }}
                 </span>
              </div>
              
              <!-- View Mode Toggle -->
              <div v-if="event.changes && event.changes.length > 0" class="flex items-center gap-1 p-0.5 bg-neutral-100 rounded-sm">
                <button 
                  @click="viewMode = 'changes'; searchQuery = ''"
                  class="px-2 py-0.5 text-[9px] font-bold rounded-sm transition-all"
                  :class="viewMode === 'changes' && !searchQuery ? 'bg-white text-blue-600 shadow-sm' : 'text-neutral-400 hover:text-neutral-600'"
                >
                  CHANGES
                </button>
                <button 
                  @click="viewMode = 'all'; searchQuery = ''"
                  class="px-2 py-0.5 text-[9px] font-bold rounded-sm transition-all"
                  :class="viewMode === 'all' && !searchQuery ? 'bg-white text-neutral-700 shadow-sm' : 'text-neutral-400 hover:text-neutral-600'"
                >
                  ALL
                </button>
              </div>
           </div>
           
           <!-- Snapshot Search -->
           <div v-if="allSnapshotEntries.length > 0" class="relative group">
              <Search class="absolute left-2 top-1/2 -translate-y-1/2 size-3 text-neutral-300 group-focus-within:text-blue-500 transition-colors" />
              <input 
                v-model="searchQuery"
                type="text" 
                :placeholder="`Search in ${allSnapshotEntries.length} fields...`" 
                class="w-full h-7 bg-neutral-50/50 border border-neutral-100 rounded-sm pl-7 text-[10px] focus:bg-white focus:border-blue-200 focus:ring-1 focus:ring-blue-100/50 transition-all outline-none"
              />
           </div>
        </div>

        <div class="space-y-4">
           <!-- No Results State -->
           <div v-if="filteredEntries.length === 0" class="py-10 text-center border border-dashed border-neutral-200 rounded-md">
              <div class="text-[10px] text-neutral-400 italic">No matching fields found.</div>
              <Button v-if="searchQuery" variant="ghost" size="xs" @click="searchQuery = ''" class="mt-2 text-[9px] h-6">Clear Search</Button>
           </div>

           <!-- Standard Attributes -->
           <div v-else class="grid grid-cols-1 gap-2">
              <div v-for="[key, val] in filteredEntries" :key="key">
                 <div class="flex items-center justify-between border rounded p-2 transition-colors relative transition-all duration-300"
                      :class="[
                        getChange(key) 
                          ? theme.container 
                          : 'bg-neutral-50/30 border-neutral-100 group hover:bg-white'
                      ]">
                    <!-- Change Indicator (Vertical Accent) -->
                    <div v-if="getChange(key)" class="absolute left-0 top-0 bottom-0 w-0.5" :class="theme.indicator"></div>

                    <div class="min-w-0 flex-1">
                       <div class="flex items-center gap-2 mb-0.5">
                          <div class="text-[9px] uppercase font-bold tracking-tighter"
                               :class="getChange(key) ? theme.text : 'text-neutral-400'">
                            {{ key }}
                          </div>
                          <div v-if="getChange(key)" 
                               class="text-[7px] font-black px-1 text-white rounded-[2px] tracking-widest uppercase"
                               :class="theme.indicator">
                            Updated
                          </div>
                       </div>
                       
                       <div class="flex flex-wrap items-baseline gap-x-2">
                          <div class="text-[11px] font-semibold"
                               :class="getChange(key) ? 'text-neutral-900' : 'text-neutral-800'">
                             <template v-if="Array.isArray(val)">
                                <div v-for="(item, i) in val" :key="i" class="bg-white px-1.5 py-0.5 rounded border border-neutral-100 mt-1 inline-block mr-1">
                                   {{ item.value || item }}
                                </div>
                             </template>
                             <template v-else-if="typeof val === 'object'">
                                <span class="text-[9px] font-mono text-neutral-400">{{ JSON.stringify(val) }}</span>
                             </template>
                             <template v-else>{{ val }}</template>
                          </div>
                          
                          <!-- Inline History -->
                          <div v-if="getChange(key)" class="text-[9px] text-neutral-400 font-medium italic">
                             (was: <span class="decoration-neutral-300">{{ getChange(key)?.old }}</span>)
                          </div>
                       </div>
                    </div>
                 </div>
              </div>
           </div>

           <!-- Extensions -->
           <div v-for="(val, key) in event.snapshot.data" :key="key">
              <div v-if="isExtension(key as string)" class="space-y-2">
                 <div class="text-[9px] font-black uppercase tracking-widest flex items-center gap-2" :class="theme.text">
                    <Separator class="flex-1" />
                    <span>Extension: {{ (key as string).split(':').pop() }}</span>
                    <Separator class="flex-1" />
                 </div>
                 <div class="grid grid-cols-2 gap-2">
                    <div v-for="(extVal, extKey) in (val as any)" :key="extKey" 
                         class="border rounded p-2" :class="[theme.bg, theme.border]">
                       <div class="text-[9px] uppercase font-bold mb-0.5" :class="theme.text">{{ extKey }}</div>
                       <div class="text-[11px] font-medium text-neutral-800">{{ extVal }}</div>
                    </div>
                 </div>
              </div>
           </div>
        </div>
      </section>

      <Separator />

      <!-- Sync Pipeline Visualization -->
      <section v-if="allHistory.length > 0">
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
              <div 
                v-for="h in allHistory.filter(x => x.traceId === event.traceId && x.type === 'HR_SYNC')" :key="h.id"
                @click="openRelatedEvent(h)"
                class="p-2 border rounded-md text-[11px] cursor-pointer transition-all flex items-center justify-between group"
                :class="h.id === event.id ? 'bg-blue-50 border-blue-200 ring-2 ring-blue-100' : 'bg-white border-neutral-100 hover:border-blue-200'"
              >
                <div class="flex flex-col">
                  <span class="font-bold text-neutral-700">HR System</span>
                  <span class="text-[9px] text-neutral-400 font-mono">{{ h.time }}</span>
                </div>
                <Badge :variant="h.status === 'SUCCESS' ? 'default' : 'destructive'" class="h-3 text-[8px] px-1">{{ h.status }}</Badge>
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
              <div 
                v-for="h in allHistory.filter(x => x.traceId === event.traceId && x.type === 'USER_UPDATE')" :key="h.id"
                @click="openRelatedEvent(h)"
                class="p-2 border rounded-md text-[11px] cursor-pointer transition-all flex items-center justify-between group"
                :class="h.id === event.id ? 'bg-orange-50 border-orange-200 ring-2 ring-orange-100' : 'bg-white border-neutral-100 hover:border-orange-200'"
              >
                <div class="flex flex-col">
                  <span class="font-bold text-neutral-700">Identity Management</span>
                  <span class="text-[9px] text-neutral-400 font-mono">{{ h.time }}</span>
                </div>
                <Badge :variant="h.status === 'SUCCESS' ? 'default' : 'destructive'" class="h-3 text-[8px] px-1">{{ h.status }}</Badge>
              </div>
            </div>
          </div>

          <!-- Step 3: Targets -->
          <div class="relative z-10 flex flex-col gap-2">
            <div class="flex items-center gap-2 mb-1">
              <div class="size-6 rounded-full bg-purple-50 border border-purple-100 flex items-center justify-center text-[10px] font-bold text-purple-600 shadow-sm">3</div>
              <span class="text-[10px] font-black text-neutral-500 uppercase tracking-tighter">Target Provisioning</span>
              <Badge variant="outline" class="h-3 text-[8px] border-purple-200 text-purple-600">{{ allHistory.filter(x => x.traceId === event.traceId && x.type === 'AD_PROVISION').length }} Targets</Badge>
            </div>
            <div class="ml-8">
              <div class="grid grid-cols-2 gap-2">
                <div 
                  v-for="h in allHistory.filter(x => x.traceId === event.traceId && x.type === 'AD_PROVISION')" :key="h.id"
                  @click="openRelatedEvent(h)"
                  class="p-2 border rounded-md text-[10px] cursor-pointer transition-all flex flex-col gap-1 group bg-white"
                  :class="h.id === event.id ? 'bg-purple-50 border-purple-200 ring-2 ring-purple-100' : 'border-neutral-100 hover:border-purple-200'"
                >
                  <div class="flex items-center justify-between">
                    <span class="font-black text-neutral-500 uppercase tracking-tighter truncate max-w-[70%]">
                      {{ h.payload?.targetSystem || 'Target System' }}
                    </span>
                    <div class="size-1.5 rounded-full" :class="h.status === 'SUCCESS' ? 'bg-green-500' : 'bg-red-500'"></div>
                  </div>
                  <div class="text-[8px] text-neutral-400 font-mono">{{ h.time.split(' ')[1] }}</div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- Technical Audit Log (Raw Data) -->
      <section class="mt-auto pt-4">
        <button 
          @click="isRawOpen = !isRawOpen"
          class="flex items-center justify-between w-full p-2 bg-neutral-900 rounded-t-md text-neutral-400 font-mono text-[10px] hover:text-white transition-colors"
          :class="{ 'rounded-md': !isRawOpen }"
        >
          <div class="flex items-center gap-2">
             <Code class="size-3" /> <span>RAW DATA AUDIT</span>
          </div>
          <component :is="isRawOpen ? ChevronDown : ChevronRight" class="size-3" />
        </button>
        <div v-if="isRawOpen" class="p-3 bg-neutral-900 rounded-b-md text-neutral-300 font-mono text-[10px] border-t border-neutral-800">
          <pre class="overflow-x-auto">{{ JSON.stringify(event, null, 2) }}</pre>
        </div>
      </section>

    </div>
  </div>
</template>
