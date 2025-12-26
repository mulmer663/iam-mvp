<script setup lang="ts">
import { ArrowRight, Code, Database, Activity, Server, ChevronDown, ChevronRight } from 'lucide-vue-next'
import { Separator } from '@/components/ui/separator'
import type { HistoryLog } from '@/types'
import { MOCK_HISTORY } from '@/mocks/data'
import { computed, ref } from 'vue'
import { useMillerStore } from '@/stores/miller'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'

const props = defineProps<{
  event: HistoryLog
  paneIndex?: number
}>()

const millerStore = useMillerStore()
const isRawOpen = ref(false)

const relatedEvents = computed(() => {
  return (MOCK_HISTORY as HistoryLog[]).filter(h => h.traceId === props.event.traceId && h.id !== props.event.id)
})

const theme = computed(() => {
  if (props.event.type === 'HR_SYNC') {
    return {
      title: 'HR Source System',
      icon: Database,
      bg: 'bg-blue-50',
      border: 'border-blue-200',
      text: 'text-blue-700',
    }
  }
  if (props.event.type === 'USER_UPDATE') {
    return {
      title: 'IAM Core (SCIM 2.0)',
      icon: Activity,
      bg: 'bg-orange-50',
      border: 'border-orange-200',
      text: 'text-orange-700',
    }
  }
  return {
    title: 'AD Target System',
    icon: Server,
    bg: 'bg-purple-50',
    border: 'border-purple-200',
    text: 'text-purple-700',
  }
})

function openRelatedEvent(log: HistoryLog) {
  const targetPaneId = `sync-detail-${log.id}`
  const existingPane = millerStore.panes.find(p => p.id === targetPaneId)
  if (existingPane) {
    millerStore.highlightPane(targetPaneId)
    return
  }
  millerStore.pushPane({
    id: targetPaneId,
    type: 'SyncDetail',
    title: `Event: ${log.traceId}`,
    data: { event: log }
  })
}

// Function to check if a value is a SCIM extension key
const isExtension = (key: string) => key.startsWith('urn:ietf:params:scim:schemas:extension:')
</script>

<template>
  <div class="h-full flex flex-col bg-white">
    <!-- Layered Header -->
    <div class="p-4 border-b flex flex-col gap-3" :class="[theme.bg, theme.border]">
      <div class="flex items-center justify-between">
        <div class="flex items-center gap-2">
           <component :is="theme.icon" class="size-5" :class="theme.text" />
           <span class="text-sm font-black uppercase tracking-tight" :class="theme.text">
             {{ theme.title }}
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
        <h3 class="text-[10px] font-black text-neutral-400 uppercase tracking-widest mb-3">
          {{ event.snapshot.layer }} Layer Snapshot
        </h3>
        <div class="space-y-4">
           <!-- Standard Attributes -->
           <div class="grid grid-cols-1 gap-2">
              <div v-for="(val, key) in event.snapshot.data" :key="key">
                 <div v-if="key !== 'schemas' && !isExtension(key as string)" 
                      class="flex items-center justify-between border border-neutral-100 rounded p-2 bg-neutral-50/30 group hover:bg-white transition-colors">
                    <div class="min-w-0 flex-1">
                       <div class="text-[9px] uppercase font-bold text-neutral-400 mb-0.5 truncate">{{ key }}</div>
                       <div class="text-[11px] font-medium text-neutral-800 break-words line-clamp-2">
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
                    </div>
                 </div>
              </div>
           </div>

           <!-- Extensions -->
           <div v-for="(val, key) in event.snapshot.data" :key="key">
              <div v-if="isExtension(key as string)" class="space-y-2">
                 <div class="text-[9px] font-black text-blue-500 uppercase tracking-widest flex items-center gap-2">
                    <Separator class="flex-1" />
                    <span>Extension: {{ (key as string).split(':').pop() }}</span>
                    <Separator class="flex-1" />
                 </div>
                 <div class="grid grid-cols-2 gap-2">
                    <div v-for="(extVal, extKey) in (val as any)" :key="extKey" 
                         class="border border-blue-50 rounded p-2 bg-blue-50/10">
                       <div class="text-[9px] uppercase font-bold text-blue-400 mb-0.5">{{ extKey }}</div>
                       <div class="text-[11px] font-medium text-neutral-800">{{ extVal }}</div>
                    </div>
                 </div>
              </div>
           </div>
        </div>
      </section>

      <Separator />

      <!-- Linked Trace Events -->
      <section v-if="relatedEvents.length > 0">
        <h3 class="text-[10px] font-black text-neutral-400 uppercase tracking-widest mb-3 flex items-center gap-2">
          Linked Trace Events
        </h3>
        <div class="bg-white border border-neutral-100 rounded-md overflow-hidden">
          <div 
            v-for="rel in relatedEvents" :key="rel.id" @click="openRelatedEvent(rel)"
            class="flex items-center gap-3 p-2.5 border-b border-neutral-50 last:border-0 hover:bg-neutral-50 cursor-pointer group transition-colors"
          >
             <div class="size-2 rounded-full" :class="{
                'bg-blue-400': rel.type === 'HR_SYNC',
                'bg-orange-400': rel.type === 'USER_UPDATE',
                'bg-purple-400': rel.type === 'AD_PROVISION'
             }"></div>
             <div class="flex-1 min-w-0">
               <div class="flex items-center gap-2">
                 <span class="text-[10px] font-bold text-neutral-700">{{ rel.type.replace('_', ' ') }}</span>
                 <span class="text-[9px] text-neutral-400">{{ rel.time.split(' ')[1] }}</span>
               </div>
               <div class="text-[10px] text-neutral-500 truncate mt-0.5">{{ rel.target }}</div>
             </div>
             <ArrowRight class="size-3 text-neutral-300 group-hover:text-black" />
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
