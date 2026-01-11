<script setup lang="ts">
import {computed, ref} from 'vue'
import {Search} from 'lucide-vue-next'
import {Separator} from '@/components/ui/separator'
import {Button} from '@/components/ui/button'

const props = defineProps<{
  data: any
  changes?: Array<{ field: string, old: string, new: string }>
  title?: string
  loading?: boolean
}>()

const searchQuery = ref('')
const viewMode = ref<'all' | 'changes'>(props.changes && props.changes.length > 0 ? 'changes' : 'all')

// --- Helpers ---
const isExtension = (key: string) => {
  return key.startsWith('urn:ietf:params:scim:schemas:extension') || key.includes('extension')
}

const getExtensionLabel = (key: string) => {
  const parts = key.split(':')
  const extIndex = parts.indexOf('extension')
  if (extIndex !== -1 && extIndex < parts.length - 1) {
    return parts.slice(extIndex + 1).join(':')
  }
  return parts[parts.length - 1]
}

function flattenObject(obj: any, prefix = ''): [string, any][] {
  let entries: [string, any][] = []
  if (!obj || typeof obj !== 'object' || Array.isArray(obj)) return []

  for (const [key, value] of Object.entries(obj)) {
    if (key === 'schemas') continue
    const k = prefix ? `${prefix}.${key}` : key
    if (value && typeof value === 'object' && !Array.isArray(value) && Object.keys(value).length > 0) {
      entries = entries.concat(flattenObject(value, k))
    } else {
      entries.push([k, value])
    }
  }
  return entries
}

const getChange = (key: string) => {
  return props.changes?.find(c => c.field === key)
}

// --- Computed Data ---
const allSnapshotEntries = computed(() => {
  let d = props.data
  // Unwrap common wrappers if present
  if (!d) return []
  if (d.event && typeof d.event === 'object' && d.event.payload) d = d.event.payload
  else if (d.command && typeof d.command === 'object' && d.command.payload) d = d.command.payload
  else if (d.payload && typeof d.payload === 'object' && !Array.isArray(d.payload)) d = d.payload

  return flattenObject(d).filter(([key]) => !isExtension(key))
})

const extensions = computed(() => {
  let d = props.data
  // Same unwrapping
  if (!d) return {}
  if (d.event && typeof d.event === 'object' && d.event.payload) d = d.event.payload
  else if (d.command && typeof d.command === 'object' && d.command.payload) d = d.command.payload
  else if (d.payload && typeof d.payload === 'object' && !Array.isArray(d.payload)) d = d.payload

  const exts: Record<string, any> = {}
  if (d) {
    for (const [key, val] of Object.entries(d)) {
      if (isExtension(key)) {
        exts[key] = val
      }
    }
  }
  return exts
})

const filteredEntries = computed(() => {
  let entries = allSnapshotEntries.value
  
  if (searchQuery.value) {
    const q = searchQuery.value.toLowerCase()
    entries = entries.filter(([key, val]) => 
      key.toLowerCase().includes(q) || 
      String(val).toLowerCase().includes(q)
    )
  }

  if (viewMode.value === 'changes' && !searchQuery.value) {
    entries = entries.filter(([key]) => !!getChange(key))
  }

  // Sort changed first
  return entries.sort(([keyA], [keyB]) => {
    const changeA = getChange(keyA) ? 1 : 0
    const changeB = getChange(keyB) ? 1 : 0
    return changeB - changeA
  })
})
</script>

<template>
  <div class="space-y-6">
    <!-- Header / Meta / Search -->
    <div class="flex flex-col gap-3">
       <div class="flex items-center justify-between">
          <div class="flex items-center gap-2">
             <h3 class="text-[10px] font-black text-neutral-400 uppercase tracking-widest">
               {{ title || 'Profile Snapshot' }}
             </h3>
             <span class="px-1.5 py-0.5 bg-neutral-100 text-[8px] font-bold text-neutral-500 rounded-sm">
               {{ filteredEntries.length }} / {{ allSnapshotEntries.length }}
             </span>
          </div>
          
          <!-- View Mode Toggle -->
          <div v-if="changes && changes.length > 0" class="flex items-center gap-1 p-0.5 bg-neutral-100 rounded-sm">
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
       
       <!-- Search -->
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

    <!-- Data Grid -->
    <div class="space-y-4">
       <!-- No Results -->
       <div v-if="filteredEntries.length === 0" class="py-10 text-center border border-dashed border-neutral-200 rounded-md">
          <div class="text-[10px] text-neutral-400 italic">No matching fields found.</div>
          <Button v-if="searchQuery" variant="ghost" size="xs" @click="searchQuery = ''" class="mt-2 text-[9px] h-6">Clear Search</Button>
       </div>

       <!-- Standard Attributes Grid -->
       <!-- Responsive Grid: Auto-fill with min-width -->
       <div v-else class="grid grid-cols-[repeat(auto-fill,minmax(240px,1fr))] gap-2">
          <div v-for="[key, val] in filteredEntries" :key="key">
             <div class="flex items-center justify-between border rounded p-2 transition-colors relative transition-all duration-300 h-full"
                  :class="[
                    getChange(key) 
                      ? 'bg-blue-50/30 border-blue-200' 
                      : 'bg-neutral-50/30 border-neutral-100 group hover:bg-white'
                  ]">
                <!-- Change Indicator -->
                <div v-if="getChange(key)" class="absolute left-0 top-0 bottom-0 w-0.5 bg-blue-500"></div>

                <div class="min-w-0 flex-1">
                   <div class="flex items-center gap-2 mb-0.5">
                      <div class="text-[9px] uppercase font-bold tracking-tighter truncate"
                           :class="getChange(key) ? 'text-blue-600' : 'text-neutral-400'"
                           :title="key">
                        {{ key }}
                      </div>
                      <div v-if="getChange(key)" 
                           class="text-[7px] font-black px-1 text-white bg-blue-500 rounded-[2px] tracking-widest uppercase">
                        Updated
                      </div>
                   </div>
                   
                   <div class="flex flex-wrap items-baseline gap-x-2">
                      <div class="text-[11px] font-semibold truncate max-w-full"
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
                      
                      <div v-if="getChange(key)" class="text-[9px] text-neutral-400 font-medium italic">
                         (was: <span class="decoration-neutral-300 line-through">{{ getChange(key)?.old }}</span>)
                      </div>
                   </div>
                </div>
             </div>
          </div>
       </div>

       <!-- Extensions -->
       <div v-if="Object.keys(extensions).length > 0">
           <div v-for="(val, key) in extensions" :key="key">
              <div class="space-y-2 mt-4">
                 <div class="text-[9px] font-black uppercase tracking-widest flex items-center gap-2 text-neutral-400">
                    <Separator class="flex-1" />
                    <span>Extension: {{ getExtensionLabel(key as string) }}</span>
                    <Separator class="flex-1" />
                 </div>
                 <div class="grid grid-cols-[repeat(auto-fill,minmax(200px,1fr))] gap-2">
                    <div v-for="(extVal, extKey) in (val as any)" :key="extKey" 
                         class="border rounded p-2 bg-neutral-50/50 border-neutral-100">
                       <div class="text-[9px] uppercase font-bold mb-0.5 text-neutral-400">{{ extKey }}</div>
                       <div class="text-[11px] font-medium text-neutral-800 truncate" :title="String(extVal)">{{ extVal }}</div>
                    </div>
                 </div>
              </div>
           </div>
       </div>

       <div v-if="$slots.footer" class="mt-6 border-t border-neutral-100 pt-4">
          <slot name="footer" />
       </div>
    </div>
  </div>
</template>
