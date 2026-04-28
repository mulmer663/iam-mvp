<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { DepartmentService } from '@/api/DepartmentService'
import type { Department } from '@/types'
import { Network } from 'lucide-vue-next'
import { useMillerStore } from '@/stores/miller'
import DeptTree from '@/components/DeptTree.vue'

const props = defineProps<{
    paneIndex?: number
}>()

const millerStore = useMillerStore()
const departments = ref<Department[]>([])
const selectedDeptId = ref<string>('')
const loading = ref(false)

onMounted(async () => {
    loading.value = true
    try {
        departments.value = await DepartmentService.getDepartments()
    } finally {
        loading.value = false
    }
})

function onDeptSelect(id: string) {
    selectedDeptId.value = id
    const dept = departments.value.find(d => d.id === id)
    millerStore.setPane((props.paneIndex ?? 0) + 1, {
        id: `dept-detail-${id}`,
        type: 'DeptDetailPane',
        title: dept?.displayName ?? id,
        data: { deptId: id, paneIndex: (props.paneIndex ?? 0) + 1 }
    })
}
</script>

<template>
    <div class="h-full flex flex-col bg-white overflow-hidden">
        <!-- Header -->
        <div class="h-9 border-b border-neutral-100 flex items-center px-3 gap-2 shrink-0 bg-neutral-50/30">
            <Network class="size-3.5 text-neutral-400" />
            <span class="text-[10px] font-bold text-neutral-500 uppercase tracking-wider">
                Departments
                <span class="ml-1 text-neutral-400 font-normal">({{ departments.length }})</span>
            </span>
        </div>

        <!-- Tree -->
        <div class="flex-1 overflow-y-auto p-2 custom-scrollbar">
            <div v-if="loading" class="py-10 flex justify-center">
                <div class="size-4 border-2 border-blue-600/20 border-t-blue-600 rounded-full animate-spin"></div>
            </div>
            <DeptTree
                v-else
                :departments="departments"
                :parent-id="null"
                :selected-id="selectedDeptId"
                @select="onDeptSelect"
            />
        </div>
    </div>
</template>

<style scoped>
.custom-scrollbar::-webkit-scrollbar { width: 4px; }
.custom-scrollbar::-webkit-scrollbar-track { background: transparent; }
.custom-scrollbar::-webkit-scrollbar-thumb { background: #f1f1f1; border-radius: 10px; }
.custom-scrollbar::-webkit-scrollbar-thumb:hover { background: #e5e5e5; }
</style>
