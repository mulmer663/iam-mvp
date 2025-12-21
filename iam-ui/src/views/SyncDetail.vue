<script setup lang="ts">
import { History, ArrowRight, Code } from 'lucide-vue-next'
import { Separator } from '@/components/ui/separator'
import type { HistoryLog } from '@/types'
import { Badge } from '@/components/ui/badge'

defineProps<{
  event: HistoryLog
}>()

const getSyncTypeVariant = (type?: string) => {
  switch (type) {
    case 'JOIN': return 'default' // Dark/Black for emphasis
    case 'REJOIN': return 'secondary'
    case 'UPDATE_CRITICAL': return 'destructive'
    case 'UPDATE_SIMPLE': return 'outline'
    case 'LEAVE': return 'destructive'
    default: return 'outline'
  }
}
</script>

<template>
  <div class="h-full flex flex-col bg-white">
    <!-- Header -->
    <div class="p-4 border-b border-neutral-100 bg-neutral-50/30">
      <div class="flex items-center gap-2 mb-2">
        <History class="size-4 text-neutral-400" />
        <span class="text-xs font-black text-neutral-700 uppercase tracking-tight">Event Detail</span>
      </div>
      <div class="flex items-center justify-between">
        <div class="text-[13px] font-bold text-neutral-900">{{ event.target }}</div>
        <div class="text-[10px] font-mono text-neutral-400">{{ event.traceId }}</div>
      </div>
    </div>

    <div class="flex-1 overflow-y-auto p-4 space-y-6">
      
      <!-- Event Info -->
      <section>
        <h3 class="text-[10px] font-black text-neutral-400 uppercase tracking-widest mb-3 flex items-center gap-2">
          <div class="size-1 bg-neutral-300 rounded-full"></div> Meta Information
        </h3>
        <div class="grid grid-cols-2 gap-4 bg-neutral-50 p-3 rounded-md border border-neutral-100">
          <div>
             <div class="text-[9px] uppercase font-bold text-neutral-400 mb-1">Event Type</div>
             <Badge :variant="getSyncTypeVariant(event.syncType)" class="h-5 text-[10px] uppercase font-bold tracking-tight">
               {{ event.syncType || event.type }}
             </Badge>
          </div>
          <div>
             <div class="text-[9px] uppercase font-bold text-neutral-400 mb-1">Timestamp</div>
             <div class="text-[11px] font-medium text-neutral-700">{{ event.time }}</div>
          </div>
          <div>
             <div class="text-[9px] uppercase font-bold text-neutral-400 mb-1">Status</div>
             <div class="text-[11px] font-bold" :class="event.status === 'SUCCESS' ? 'text-green-600' : 'text-red-500'">
               {{ event.status }}
             </div>
          </div>
        </div>
      </section>

      <Separator />

      <!-- Payloads (Mock for now) -->
      <section>
        <h3 class="text-[10px] font-black text-neutral-400 uppercase tracking-widest mb-3 flex items-center gap-2">
          <div class="size-1 bg-neutral-300 rounded-full"></div> Data Changes
        </h3>
        
        <div v-if="event.syncType === 'UPDATE_CRITICAL'" class="space-y-2">
           <div class="flex items-center gap-3 justify-center mb-2 bg-red-50/50 p-2 rounded border border-dashed border-red-100">
              <div class="flex-1 text-center">
                <div class="text-[8px] uppercase font-bold text-neutral-400 mb-0.5">Position</div>
                <div class="text-[11px] text-neutral-500 line-through font-medium truncate">Senior Engineer</div>
              </div>
              <ArrowRight class="size-3 text-red-300 shrink-0" />
              <div class="flex-1 text-center">
                <div class="text-[8px] uppercase font-bold text-neutral-400 mb-0.5">Position</div>
                <div class="text-[11px] text-red-700 font-bold truncate">Principal Engineer</div>
              </div>
           </div>
        </div>
        
        <div v-else-if="event.syncType === 'JOIN'" class="p-4 bg-blue-50/30 border border-blue-100 rounded text-center">
           <span class="text-[11px] text-blue-600 font-medium">✨ New Employee Registration</span>
        </div>

        <div v-else class="p-2 bg-neutral-900 rounded-md text-neutral-300 font-mono text-[10px] overflow-x-auto">
          <div class="flex items-center gap-2 mb-2 pb-2 border-b border-neutral-700 text-neutral-500">
             <Code class="size-3" /> <span>Raw Payload</span>
          </div>
          <pre>{
  "source": "HR_SYSTEM",
  "sync_mode": "FULL",
  "attributes": {
    "email": "changed@iam.com",
    "updated_at": "2025-12-21T10:00:00Z"
  }
}</pre>
        </div>
      </section>

    </div>
  </div>
</template>
