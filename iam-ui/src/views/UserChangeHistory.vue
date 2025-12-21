<script setup lang="ts">
import { History, ArrowRight } from 'lucide-vue-next'


interface ChangeEvent {
  field: string
  before: string
  after: string
  date: string
  actor: string
}

const MOCK_CHANGES: ChangeEvent[] = [
  { field: 'position', before: 'Junior Dev', after: 'Senior Dev', date: '2023-12-01 10:00', actor: 'HR_ADMIN' },
  { field: 'deptCode', before: 'DEPT02', after: 'DEPT01', date: '2023-11-15 14:20', actor: 'SYSTEM' },
  { field: 'status', before: 'INACTIVE', after: 'ACTIVE', date: '2023-10-10 09:00', actor: 'MANAGER_04' },
  { field: 'email', before: 'old@example.com', after: 'gh.hong@example.com', date: '2023-09-01 11:30', actor: 'SELF' },
]
</script>

<template>
  <div class="h-full flex flex-col bg-white">
    <div class="p-3 border-b border-neutral-100 bg-neutral-50/30">
      <div class="flex items-center gap-2">
        <History class="size-3.5 text-neutral-400" />
        <span class="text-[11px] font-black text-neutral-700 uppercase tracking-tight">Modification Ledger</span>
      </div>
    </div>
    
    <div class="flex-1 overflow-y-auto">
      <div v-for="(change, idx) in MOCK_CHANGES" :key="idx" class="border-b border-neutral-50 p-3 hover:bg-neutral-50/50 transition-colors">
        <div class="flex justify-between items-start mb-2">
          <div class="px-1.5 py-0.5 bg-neutral-100 text-[9px] font-black text-neutral-500 rounded-sm uppercase tracking-tighter">
            {{ change.field }}
          </div>
          <div class="text-[9px] text-neutral-400 font-mono">{{ change.date }}</div>
        </div>
        
        <div class="flex items-center gap-3 justify-center mb-2 bg-neutral-50 p-1.5 rounded border border-dashed border-neutral-200">
          <div class="flex-1 text-center">
            <div class="text-[8px] uppercase font-bold text-neutral-400 mb-0.5">Before</div>
            <div class="text-[11px] text-red-600 line-through font-medium truncate">{{ change.before }}</div>
          </div>
          <ArrowRight class="size-3 text-neutral-300 shrink-0" />
          <div class="flex-1 text-center">
            <div class="text-[8px] uppercase font-bold text-neutral-400 mb-0.5">After</div>
            <div class="text-[11px] text-green-700 font-bold truncate">{{ change.after }}</div>
          </div>
        </div>
        
        <div class="flex items-center gap-1">
          <span class="text-[9px] text-neutral-400">Changed by</span>
          <span class="text-[9px] font-bold text-neutral-600">{{ change.actor }}</span>
        </div>
      </div>
    </div>
    
    <div class="p-3 bg-neutral-50 text-[10px] text-neutral-400 italic">
      Showing last {{ MOCK_CHANGES.length }} system-audited events.
    </div>
  </div>
</template>
