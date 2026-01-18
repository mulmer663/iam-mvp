<script setup lang="ts">
import {ref} from 'vue'
import {Info, Plus, Search, Users as UsersIcon} from 'lucide-vue-next'
import {Badge} from '@/components/ui/badge'
import {Button} from '@/components/ui/button'

const groups = ref([
    { id: 'GRP-001', displayName: 'IT Admins', memberCount: 5, active: true },
    { id: 'GRP-002', displayName: 'HR Managers', memberCount: 3, active: true },
    { id: 'GRP-003', displayName: 'Audit Team', memberCount: 8, active: true }
])

interface Props {
  paneIndex?: number
}
defineProps<Props>()

</script>

<template>
  <div class="h-full flex flex-col bg-white overflow-hidden">
    <!-- Toolbar -->
    <div class="h-10 border-b border-neutral-100 flex items-center px-4 justify-between bg-white shrink-0">
        <h2 class="text-sm font-bold text-neutral-800">SCIM Groups</h2>
        <Button size="xs" class="bg-blue-600 text-white hover:bg-blue-700 font-bold h-6 text-[11px]">
            <Plus class="size-3" /> REGISTER
        </Button>
    </div>

    <!-- Search -->
    <div class="p-3 border-b border-neutral-50">
        <div class="relative">
            <Search class="absolute left-2 top-1/2 -translate-y-1/2 size-3 text-neutral-400" />
            <input 
                type="text" 
                placeholder="Search groups..." 
                class="w-full h-8 bg-neutral-50 border border-neutral-100 rounded-sm pl-7 text-[11px] focus:border-blue-300 focus:bg-white"
            />
        </div>
    </div>

    <!-- List -->
    <div class="flex-1 overflow-y-auto p-2 space-y-1 custom-scrollbar">
        <div v-for="group in groups" :key="group.id" 
             class="flex items-center justify-between p-3 border border-neutral-50 rounded-lg hover:bg-neutral-50 cursor-pointer group transition-all"
        >
            <div class="flex items-center gap-3">
                <div class="size-8 bg-neutral-100 rounded flex items-center justify-center text-neutral-500 group-hover:bg-blue-50 group-hover:text-blue-500 transition-colors">
                    <UsersIcon class="size-4" />
                </div>
                <div>
                    <div class="text-xs font-bold text-neutral-800">{{ group.displayName }}</div>
                    <div class="text-[9px] text-neutral-400 font-mono">{{ group.id }}</div>
                </div>
            </div>
            <div class="flex items-center gap-3">
                <div class="text-right">
                    <div class="text-[10px] font-bold text-neutral-600">{{ group.memberCount }} Members</div>
                    <Badge variant="outline" class="text-[8px] h-3.5 px-1 bg-green-50 text-green-600 border-green-100">ACTIVE</Badge>
                </div>
            </div>
        </div>

        <div v-if="groups.length === 0" class="py-20 flex flex-col items-center text-center">
            <Info class="size-8 text-neutral-100 mb-2" />
            <p class="text-xs text-neutral-400 font-medium">No groups found</p>
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
