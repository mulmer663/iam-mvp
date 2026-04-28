<script setup lang="ts">
import { onMounted } from 'vue'
import { Network, Plus } from 'lucide-vue-next'
import { useMillerStore } from '@/stores/miller'
import { useDeptStore } from '@/stores/department'
import DeptTree from '@/components/DeptTree.vue'

const props = defineProps<{ paneIndex?: number }>()

const millerStore = useMillerStore()
const deptStore = useDeptStore()

onMounted(() => {
    if (deptStore.departments.length === 0) deptStore.fetchAll()
})

function onDeptSelect(id: string) {
    const dept = deptStore.departments.find(d => d.id === id)
    millerStore.setPane((props.paneIndex ?? 0) + 1, {
        id: `dept-detail-${id}`,
        type: 'DeptDetailPane',
        title: dept?.displayName ?? id,
        data: { deptId: id, paneIndex: (props.paneIndex ?? 0) + 1 }
    })
}

function openCreate() {
    millerStore.setPane((props.paneIndex ?? 0) + 1, {
        id: `dept-create-${Date.now()}`,
        type: 'DeptCreatePane',
        title: 'New Department',
        data: { paneIndex: (props.paneIndex ?? 0) + 1 },
        width: 'w1'
    })
}
</script>

<template>
    <div class="h-full flex flex-col bg-white overflow-hidden">
        <!-- Header -->
        <div class="h-9 border-b border-neutral-100 flex items-center px-3 gap-2 shrink-0 bg-neutral-50/30">
            <Network class="size-3.5 text-neutral-400" />
            <span class="text-[10px] font-bold text-neutral-500 uppercase tracking-wider flex-1">
                Departments
                <span class="ml-1 text-neutral-400 font-normal">({{ deptStore.departments.length }})</span>
            </span>
            <button
                @click="openCreate"
                class="flex items-center gap-1 h-6 px-2 text-[10px] font-bold bg-neutral-900 text-white rounded hover:bg-neutral-700 transition-colors"
            >
                <Plus class="size-3" /> New
            </button>
        </div>

        <!-- Tree -->
        <div class="flex-1 overflow-y-auto p-2 custom-scrollbar">
            <div v-if="deptStore.loading" class="py-10 flex justify-center">
                <div class="size-4 border-2 border-blue-600/20 border-t-blue-600 rounded-full animate-spin"></div>
            </div>
            <DeptTree
                v-else
                :departments="deptStore.departments"
                :parent-id="null"
                :selected-id="''"
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
