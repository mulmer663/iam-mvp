<script setup lang="ts">
import { ChevronRight, ChevronDown, Folder, Home } from 'lucide-vue-next'
import { MOCK_DEPARTMENTS } from '@/mocks/data'
import { ref, computed } from 'vue'
import type { Department } from '@/types'

defineOptions({
  name: 'DeptTree'
})

const props = defineProps<{
  parentId: string | null
  selectedId: string // Received from parent
}>()

const emit = defineEmits<{
  (e: 'select', deptId: string): void
}>()

const openGroups = ref<Set<string>>(new Set(['DEPT01', 'DEPT02']))

const currentDepartments = computed((): Department[] => 
  MOCK_DEPARTMENTS.filter(d => d.parentId === props.parentId) as Department[]
)

function toggleGroup(id: string) {
  if (openGroups.value.has(id)) {
    openGroups.value.delete(id)
  } else {
    openGroups.value.add(id)
  }
}

function selectDept(dept: Department) {
  emit('select', dept.id)
}
</script>

<template>
  <div class="flex flex-col">
    <div v-for="dept in currentDepartments" :key="dept.id" class="flex flex-col">
      <div 
        @click="selectDept(dept)"
        class="flex items-center gap-1.5 p-1 px-2 rounded-sm cursor-pointer group transition-all relative overflow-hidden"
        :class="[props.selectedId === dept.id ? 'bg-blue-600 text-white shadow-sm font-bold' : 'hover:bg-neutral-100 text-neutral-600']"
      >
        <div v-if="props.selectedId === dept.id" class="absolute left-0 top-0 bottom-0 w-0.5 bg-blue-300"></div>
        <button 
          v-if="MOCK_DEPARTMENTS.some(d => d.parentId === dept.id)"
          @click.stop="toggleGroup(dept.id)"
          class="p-0.5 hover:bg-neutral-200 rounded-sm shrink-0"
        >
          <ChevronDown v-if="openGroups.has(dept.id)" class="size-3" />
          <ChevronRight v-else class="size-3" />
        </button>
        <div v-else class="size-4 w-4 shrink-0"></div>
        
        <Home v-if="!dept.parentId" class="size-3 opacity-50" />
        <Folder v-else class="size-3 opacity-50" />
        
        <span class="text-[12px] font-medium truncate">{{ dept.name }}</span>
      </div>
      
      <!-- Recursive nesting -->
      <div 
        v-if="openGroups.has(dept.id) && MOCK_DEPARTMENTS.some(d => d.parentId === dept.id)"
        class="ml-3 border-l border-neutral-100 pl-1"
      >
        <DeptTree 
          :parent-id="dept.id" 
          :selected-id="props.selectedId"
          @select="id => emit('select', id)" 
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
/* Recursive component requires name for self-reference */
</style>
