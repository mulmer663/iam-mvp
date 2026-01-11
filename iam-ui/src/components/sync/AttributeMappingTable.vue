<script setup lang="ts">
import {ref} from 'vue'
import {ArrowRight, ChevronDown, ChevronRight} from 'lucide-vue-next'
import {Badge} from '@/components/ui/badge'
import {Table, TableBody, TableCell, TableHead, TableHeader, TableRow,} from '@/components/ui/table'
import type {AttributeMapping} from '@/types'

const props = defineProps<{
  mappings?: AttributeMapping[]
  appliedRules?: number[]
  loading?: boolean
  fromLabel: string
  toLabel: string
  isHistorical?: boolean
}>()

const isOpen = ref(false)
</script>

<template>
  <section v-if="(mappings && mappings.length > 0) || (appliedRules && appliedRules.length > 0)">
    <button 
      @click="isOpen = !isOpen"
      class="flex items-center justify-between w-full p-2.5 rounded-md border transition-all group"
      :class="isOpen ? 'bg-white border-neutral-400 shadow-sm mb-3 ring-2 ring-neutral-50' : 'bg-neutral-50 border-neutral-200 hover:border-neutral-300'"
    >
      <div class="flex items-center gap-3">
         <div class="size-5 rounded flex items-center justify-center shadow-sm" :class="isOpen ? 'bg-neutral-100' : 'bg-white border'">
            <ArrowRight class="size-3" :class="isOpen ? 'text-neutral-900' : 'text-neutral-400'" />
         </div>
         <div class="flex flex-col items-start leading-none text-left gap-0.5">
            <h3 v-if="mappings && mappings.length > 0" class="text-[10px] font-black uppercase tracking-widest transition-colors flex items-center"
                :class="isOpen ? 'text-neutral-900' : 'text-neutral-600'">
              Attribute Mapping
              <Badge v-if="isHistorical" variant="outline" class="ml-2 h-4 text-[8px] bg-orange-500/10 text-orange-500 border-orange-500/20 uppercase tracking-tighter">Historical</Badge>
            </h3>
            <h3 v-else-if="appliedRules && appliedRules.length > 0" class="text-[10px] font-black uppercase tracking-widest transition-colors"
                :class="isOpen ? 'text-neutral-900' : 'text-neutral-600'">
              Applied Transformation Rules
            </h3>
            <span class="text-[8px] font-bold text-neutral-400 uppercase tracking-tighter">
               <span v-if="mappings && mappings.length > 0">{{ fromLabel }} → {{ toLabel }} ({{ mappings.length }} Fields)</span>
               <span v-else>{{ appliedRules?.length }} Rules Applied</span>
            </span>
         </div>
      </div>
      <component :is="isOpen ? ChevronDown : ChevronRight" class="size-4" :class="isOpen ? 'text-neutral-500' : 'text-neutral-400'" />
    </button>
    
    <div v-if="isOpen" class="border rounded-md overflow-hidden bg-neutral-50/50">
       <!-- Loading State -->
       <div v-if="loading" class="p-8 text-center bg-white flex flex-col items-center gap-2">
          <div class="size-4 border-2 border-blue-500 border-t-transparent rounded-full animate-spin"></div>
          <span class="text-[10px] text-neutral-400">Loading historical mappings...</span>
       </div>

       <!-- Mappings Table --> 
       <Table v-else-if="mappings && mappings.length > 0">
         <TableHeader class="bg-white">
           <TableRow class="h-8 hover:bg-transparent shadow-sm">
             <TableHead class="text-[9px] font-bold uppercase py-0 px-3 w-[35%]">{{ fromLabel }} Field</TableHead>
             <TableHead class="text-[9px] font-bold uppercase py-0 px-1 text-center w-[10%]">Req</TableHead>
             <TableHead class="text-[9px] font-bold uppercase py-0 px-3 text-center w-[20%]">Type</TableHead>
             <TableHead class="text-[9px] font-bold uppercase py-0 px-3 w-[35%]">{{ toLabel }} Field</TableHead>
           </TableRow>
         </TableHeader>
         <TableBody>
            <TableRow v-for="(m, idx) in mappings" :key="idx" class="h-9 hover:bg-white border-b-neutral-100 last:border-0 group/row">
              <TableCell class="py-1 px-3 text-[10px] font-mono" 
                         :class="[
                            fromLabel === 'HR' ? 'text-blue-600' : '',
                            fromLabel === 'IAM' ? 'text-orange-600 font-bold' : ''
                         ]">
                 {{ m.fromField }}
              </TableCell>
              <TableCell class="py-1 px-0 text-center">
                 <div v-if="m.isRequired" class="size-1 bg-red-400 rounded-full mx-auto" title="Required Field"></div>
              </TableCell>
              <TableCell class="py-1 px-2 text-center overflow-hidden">
                 <Badge variant="outline" class="h-4 text-[7px] text-neutral-400 border-neutral-100 bg-white font-mono uppercase tracking-tighter">
                    {{ m.transformType }}
                 </Badge>
              </TableCell>
              <TableCell class="py-1 px-3 text-[10px] font-mono flex items-center gap-2"
                         :class="[
                            toLabel === 'IAM' ? 'text-orange-600 font-bold' : '',
                            toLabel === 'AD' ? 'text-purple-600' : ''
                         ]">
                 <ArrowRight class="size-2 text-neutral-200" />
                 {{ m.toField }}
              </TableCell>
            </TableRow>
         </TableBody>
       </Table>

       <!-- Applied Rules List -->
       <div v-else class="p-3 bg-white">
          <div v-for="ruleId in appliedRules" :key="ruleId" class="flex items-center gap-2 mb-1">
             <div class="px-2 py-0.5 bg-neutral-100 text-[10px] font-mono rounded text-neutral-600">Rule Version: {{ ruleId }}</div>
             <div class="text-[10px] text-neutral-400">Successfully executed</div>
          </div>
       </div>
    </div>
  </section>
</template>
