<script setup lang="ts">
import DeptTree from '@/components/DeptTree.vue'
import UserTable from '@/components/UserTable.vue'
import { Button } from '@/components/ui/button'
import { ref, onMounted } from 'vue'
import { Search, Filter, Plus } from 'lucide-vue-next'
import { useMillerStore } from '@/stores/miller'
import { DepartmentService } from '@/api/DepartmentService'
import type { Department } from '@/types'

const props = defineProps<{
  paneIndex?: number
  initialDeptId?: string
}>()

const millerStore = useMillerStore()
const departments = ref<Department[]>([])
const selectedDeptId = ref<string>(props.initialDeptId || '')

onMounted(async () => {
  departments.value = await DepartmentService.getDepartments()
  if (!selectedDeptId.value && departments.value.length > 0) {
    const root = departments.value.find(d => !d.parentId)
    if (root) selectedDeptId.value = root.id
  }
})

function onDeptSelect(id: string) {
  selectedDeptId.value = id
}

function openUserCreate() {
  const paneId = `user-create-${Date.now()}`
  const pane = {
    id: paneId,
    type: 'UserCreatePane',
    title: 'New User',
    data: { paneIndex: (props.paneIndex ?? 0) + 1 },
    width: 'w1'
  }
  if (typeof props.paneIndex === 'number') {
    millerStore.setPane(props.paneIndex + 1, pane)
  } else {
    millerStore.pushPane(pane)
  }
}
</script>

<template>
  <div class="h-full flex flex-col">
    <!-- Sub Header Controls -->
    <div class="h-10 border-b border-neutral-100 flex items-center px-4 justify-between bg-white shrink-0">
       <div class="flex items-center gap-3">
          <h2 class="text-sm font-bold text-neutral-800">Account Explorer</h2>
          <div class="flex items-center gap-1 p-0.5 bg-neutral-100 rounded-sm">
             <button class="px-2 py-1 text-[10px] font-bold bg-white shadow-sm rounded-sm text-neutral-700">LIST</button>
             <button class="px-2 py-1 text-[10px] font-bold text-neutral-400 hover:text-neutral-600 transition-colors rounded-sm">GRAPH</button>
          </div>
       </div>
       <div class="flex gap-1.5">
          <Button variant="outline" size="xs" class="bg-white border-neutral-200 hover:bg-neutral-50 flex items-center gap-1.5 h-6 text-[11px]">
             <Filter class="size-3 text-neutral-400" /> Filter
          </Button>
          <Button size="xs" class="bg-blue-600 text-white hover:bg-blue-700 font-bold flex items-center gap-1 h-6 text-[11px]" @click="openUserCreate">
             <Plus class="size-3" /> REGISTER
          </Button>
       </div>
    </div>

    <!-- Master-Detail Content -->
    <div class="flex-1 flex overflow-hidden">
      <!-- Left: Dept Tree (Fixed Width) -->
      <div class="w-[240px] border-r border-neutral-100 flex flex-col shrink-0">
        <div class="p-3">
           <div class="relative mb-3">
              <Search class="absolute left-2 top-1/2 -translate-y-1/2 size-3 text-neutral-400" />
              <input 
                type="text" 
                placeholder="Find department..." 
                class="w-full h-8 bg-neutral-50 border border-neutral-100 rounded-sm pl-7 text-[11px] focus:border-blue-300 focus:bg-white"
              />
           </div>
           <DeptTree :departments="departments" :parent-id="null" :selected-id="selectedDeptId" @select="onDeptSelect" />
        </div>
      </div>

      <!-- Right: User Table (Fluid) -->
      <div class="flex-1 flex flex-col bg-white">
        <div class="p-3 py-2 border-b border-neutral-50 flex items-center justify-between text-[11px]">
           <div class="flex items-center gap-2 text-neutral-400 font-medium">
              <span>Users in</span>
              <span class="text-blue-600 font-bold bg-blue-50 px-1.5 rounded-sm">
                 {{ selectedDeptId }}
              </span>
           </div>
           <span class="text-neutral-400">Total: 128 entries</span>
        </div>
        <div class="flex-1 p-3">
           <UserTable :dept-id="selectedDeptId" :pane-index="props.paneIndex" />
        </div>
      </div>
    </div>
  </div>
</template>
