<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue'
import { DepartmentService } from '@/api/DepartmentService'
import type { Department } from '@/types'
import { Folder, ChevronRight, Network, Info, Users as UsersIcon } from 'lucide-vue-next'
import { Badge } from '@/components/ui/badge'
import { useMillerStore } from '@/stores/miller'
import UserProfileViewer from '@/components/common/UserProfileViewer.vue'
import { Separator } from '@/components/ui/separator'

const props = defineProps<{
  paneIndex?: number
  deptId?: string // If null, show roots
}>()

const millerStore = useMillerStore()
const subDepts = ref<Department[]>([])
const currentDept = ref<Department | null>(null)
const loading = ref(false)

// Check if this specific department is "selected" in the next column
const selectedSubDeptId = computed(() => {
  if (typeof props.paneIndex !== 'number') return null
  const nextPane = millerStore.panes[props.paneIndex + 1]
  if (nextPane?.type === 'DeptManagement') {
    return nextPane.data.deptId
  }
  return null
})

async function loadData() {
  loading.value = true
  try {
    subDepts.value = await DepartmentService.getSubDepartments(props.deptId || null)
    if (props.deptId) {
      currentDept.value = await DepartmentService.getDepartment(props.deptId) || null
    }
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
watch(() => props.deptId, loadData)

// id/meta는 헤더에 이미 표시 — UserProfileViewer에는 비즈니스 필드만 전달
const deptDisplayData = computed(() => {
  if (!currentDept.value) return {}
  const { displayName, description, active, parentId, externalId } = currentDept.value
  return { displayName, description, active, parentId, externalId }
})

function openSubDept(dept: Department) {
  const detailPaneId = `dept-${dept.id}`
  const detailPane = {
    id: detailPaneId,
    type: 'DeptManagement',
    title: dept.displayName,
    data: { deptId: dept.id }
  }
  
  if (typeof props.paneIndex === 'number') {
    millerStore.setPane(props.paneIndex + 1, detailPane)
  } else {
    millerStore.pushPane(detailPane)
  }
}

function openMemberSync() {
    if (!currentDept.value) return
    
    const userListPaneId = `dept-users-${currentDept.value.id}`
    const userListPane = {
        id: userListPaneId,
        type: 'OrgUserManagement',
        title: `${currentDept.value.displayName} Members`,
        data: { initialDeptId: currentDept.value.id },
        width: '800px',
        maxWidth: '800px'
    }

    if (typeof props.paneIndex === 'number') {
        millerStore.setPane(props.paneIndex + 1, userListPane)
    } else {
        millerStore.pushPane(userListPane)
    }
}
</script>

<template>
  <div class="h-full flex flex-col bg-white overflow-hidden">
    <!-- Department Detail Header (Only if deptId exists) -->
    <div v-if="currentDept" class="p-4 bg-neutral-50/50 border-b border-neutral-100 overflow-y-auto max-h-[60vh] shrink-0">
        <div class="flex items-start justify-between">
            <div class="flex items-center gap-3">
                <div class="size-10 bg-white border border-neutral-200 rounded-lg flex items-center justify-center text-blue-600 shadow-sm">
                    <Network class="size-5" />
                </div>
                <div>
                    <h2 class="text-sm font-black text-neutral-900 leading-tight uppercase tracking-tight">{{ currentDept.displayName }}</h2>
                    <div class="flex items-center gap-2 mt-0.5">
                        <div class="text-[9px] text-neutral-400 font-mono">ID: {{ currentDept.id }}</div>
                        <div v-if="currentDept.externalId" class="text-[9px] text-blue-400 font-mono bg-blue-50 px-1 rounded">EXT: {{ currentDept.externalId }}</div>
                    </div>
                </div>
            </div>
            <Badge variant="outline" class="text-[9px] font-bold bg-white">DEPT_UNIT</Badge>
        </div>

        <div class="mt-4 grid grid-cols-2 gap-2">
            <div class="bg-white p-2 border border-neutral-100 rounded-md">
                <div class="text-[9px] text-neutral-400 font-black uppercase tracking-wider mb-1">Total Members</div>
                <div class="flex items-center gap-1.5">
                    <UsersIcon class="size-3 text-blue-500" />
                    <span class="text-xs font-bold text-neutral-700">12</span>
                </div>
            </div>
            <div class="bg-white p-2 border border-neutral-100 rounded-md">
                <div class="text-[9px] text-neutral-400 font-black uppercase tracking-wider mb-1">Status</div>
                <div class="flex items-center gap-1.5">
                    <div class="size-1.5 rounded-full" :class="currentDept.active ? 'bg-green-500' : 'bg-neutral-300'"></div>
                    <span class="text-xs font-bold text-neutral-700">{{ currentDept.active ? 'ACTIVE' : 'INACTIVE' }}</span>
                </div>
            </div>
        </div>

        <Separator class="my-4 bg-neutral-100" />

        <!-- SCIM Attributes Section -->
        <div class="mt-4">
            <UserProfileViewer
                :data="deptDisplayData"
                title="Department Attributes (SCIM)"
            />
        </div>

        <button 
            @click="openMemberSync"
            class="w-full mt-3 py-1.5 bg-neutral-900 text-white rounded text-[10px] font-bold hover:bg-neutral-800 transition-colors flex items-center justify-center gap-2"
        >
            <UsersIcon class="size-3" /> VIEW ALL MEMBERS
        </button>
    </div>

    <!-- Sub-Departments List -->
    <div class="flex-1 overflow-y-auto p-2 custom-scrollbar">
      <div class="px-2 py-1.5 mb-1 flex items-center justify-between">
        <h3 class="text-[10px] font-black text-neutral-400 uppercase tracking-widest flex items-center gap-2">
          <div class="size-1 bg-neutral-300 rounded-full"></div> 
          {{ currentDept ? 'Sub-Units' : 'Root Departments' }} ({{ subDepts.length }})
        </h3>
      </div>

      <div class="flex flex-col gap-1">
        <div 
          v-for="dept in subDepts" 
          :key="dept.id"
          @click="openSubDept(dept)"
          class="flex items-center justify-between p-2 rounded-md cursor-pointer transition-all group"
          :class="[
            selectedSubDeptId === dept.id 
              ? 'bg-blue-600 text-white shadow-md z-10' 
              : 'hover:bg-neutral-50 text-neutral-700'
          ]"
        >
          <div class="flex items-center gap-3 overflow-hidden">
             <div 
                class="size-7 rounded flex items-center justify-center transition-colors shrink-0"
                :class="selectedSubDeptId === dept.id ? 'bg-blue-500' : 'bg-neutral-100 text-neutral-400 group-hover:bg-blue-50 group-hover:text-blue-500'"
             >
                <Folder class="size-3.5" />
             </div>
             <div class="min-w-0">
                <div class="text-[11px] font-bold truncate leading-tight">{{ dept.displayName }}</div>
                <div class="text-[9px] font-mono opacity-50">{{ dept.id }}</div>
             </div>
          </div>
          
          <div class="flex items-center gap-2 shrink-0">
             <div v-if="selectedSubDeptId !== dept.id" class="text-[9px] font-bold px-1.5 py-0.5 bg-neutral-100 text-neutral-400 rounded-full group-hover:bg-neutral-200">8</div>
             <ChevronRight class="size-3.5 opacity-30 group-hover:opacity-100 group-hover:translate-x-0.5 transition-all" />
          </div>
        </div>

        <!-- Empty State -->
        <div v-if="!loading && subDepts.length === 0" class="py-10 flex flex-col items-center text-center">
            <Info class="size-6 text-neutral-200 mb-2" />
            <p class="text-[10px] text-neutral-400 font-medium">No sub-units found</p>
        </div>

        <!-- Loading -->
        <div v-if="loading" class="py-10 flex justify-center">
            <div class="size-4 border-2 border-blue-600/20 border-t-blue-600 rounded-full animate-spin"></div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.custom-scrollbar::-webkit-scrollbar {
  width: 4px;
}
.custom-scrollbar::-webkit-scrollbar-track {
  background: transparent;
}
.custom-scrollbar::-webkit-scrollbar-thumb {
  background: #f1f1f1;
  border-radius: 10px;
}
.custom-scrollbar::-webkit-scrollbar-thumb:hover {
  background: #e5e5e5;
}
</style>
