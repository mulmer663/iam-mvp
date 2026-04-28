<script setup lang="ts">
import { onMounted } from 'vue'
import { Info, Plus, Users as UsersIcon } from 'lucide-vue-next'
import { useMillerStore } from '@/stores/miller'
import { useGroupStore } from '@/stores/group'

const props = defineProps<{ paneIndex?: number }>()

const millerStore = useMillerStore()
const groupStore = useGroupStore()

onMounted(async () => {
    if (groupStore.groups.length === 0) await groupStore.fetchAll()
})

function openDetail(group: any) {
    const paneId = `group-detail-${group.id}`
    if (millerStore.panes.find(p => p.id === paneId)) {
        millerStore.activePaneId = paneId
        return
    }
    millerStore.setPane((props.paneIndex ?? 0) + 1, {
        id: paneId,
        type: 'GroupDetailPane',
        title: group.displayName,
        data: { groupId: group.id },
        width: 'w1'
    })
}

function openCreate() {
    millerStore.setPane((props.paneIndex ?? 0) + 1, {
        id: `group-create-${Date.now()}`,
        type: 'GroupCreatePane',
        title: 'New Group',
        data: { paneIndex: (props.paneIndex ?? 0) + 1 },
        width: 'w1'
    })
}
</script>

<template>
    <div class="h-full flex flex-col bg-white overflow-hidden">
        <!-- Toolbar -->
        <div class="h-9 border-b border-neutral-100 flex items-center px-3 gap-2 shrink-0 bg-neutral-50/30">
            <UsersIcon class="size-3.5 text-neutral-400" />
            <span class="text-[10px] font-bold text-neutral-500 uppercase tracking-wider flex-1">
                Groups
                <span class="ml-1 text-neutral-400 font-normal">({{ groupStore.groups.length }})</span>
            </span>
            <button @click="openCreate"
                class="flex items-center gap-1 h-6 px-2 text-[10px] font-bold bg-neutral-900 text-white rounded hover:bg-neutral-700 transition-colors">
                <Plus class="size-3" /> New
            </button>
        </div>

        <!-- List -->
        <div class="flex-1 overflow-y-auto custom-scrollbar">
            <div v-if="groupStore.loading" class="py-10 flex justify-center">
                <div class="size-4 border-2 border-blue-600/20 border-t-blue-600 rounded-full animate-spin"></div>
            </div>

            <div v-else-if="groupStore.groups.length === 0" class="py-20 flex flex-col items-center text-center">
                <Info class="size-8 text-neutral-100 mb-2" />
                <p class="text-[11px] text-neutral-400 font-medium">No groups yet</p>
            </div>

            <div v-else class="divide-y divide-neutral-50">
                <div v-for="group in groupStore.groups" :key="group.id"
                     @click="openDetail(group)"
                     class="flex items-center gap-3 px-3 py-2.5 hover:bg-neutral-50 cursor-pointer transition-colors group">
                    <div class="size-7 bg-neutral-100 rounded flex items-center justify-center text-neutral-400 group-hover:bg-blue-50 group-hover:text-blue-500 transition-colors shrink-0">
                        <UsersIcon class="size-3.5" />
                    </div>
                    <div class="min-w-0 flex-1">
                        <div class="text-[12px] font-bold text-neutral-800 truncate">{{ group.displayName }}</div>
                        <div class="text-[10px] text-neutral-400 font-mono truncate">{{ group.id }}</div>
                    </div>
                    <div class="text-[10px] text-neutral-400 shrink-0">
                        {{ group.members?.length ?? 0 }} members
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>

<style scoped>
.custom-scrollbar::-webkit-scrollbar { width: 4px; }
.custom-scrollbar::-webkit-scrollbar-track { background: transparent; }
.custom-scrollbar::-webkit-scrollbar-thumb { background: #f1f1f1; border-radius: 10px; }
.custom-scrollbar::-webkit-scrollbar-thumb:hover { background: #e5e5e5; }
</style>
