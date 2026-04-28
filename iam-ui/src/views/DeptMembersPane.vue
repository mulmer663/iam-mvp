<script setup lang="ts">
import { Users } from 'lucide-vue-next'
import { Button } from '@/components/ui/button'
import { useMillerStore } from '@/stores/miller'
import UserTable from '@/components/UserTable.vue'

const props = defineProps<{
    deptId: string
    deptName: string
    paneIndex?: number
}>()

const millerStore = useMillerStore()

function openUserCreate() {
    millerStore.setPane((props.paneIndex ?? 0) + 1, {
        id: `user-create-${Date.now()}`,
        type: 'UserCreatePane',
        title: 'New User',
        data: { paneIndex: (props.paneIndex ?? 0) + 1 },
        width: 'w1'
    })
}
</script>

<template>
    <div class="h-full flex flex-col bg-white">
        <!-- Header -->
        <div class="border-b border-neutral-100 bg-neutral-50/60 px-4 py-3 shrink-0 flex items-center justify-between gap-2">
            <div class="flex items-center gap-2 min-w-0">
                <div class="size-7 rounded-md bg-blue-50 border border-blue-100 flex items-center justify-center shrink-0">
                    <Users class="size-3.5 text-blue-500" />
                </div>
                <div class="min-w-0">
                    <div class="font-bold text-neutral-800 text-[13px] truncate">{{ deptName }}</div>
                    <div class="text-[10px] text-neutral-400">Members</div>
                </div>
            </div>
            <Button size="xs" class="h-7 text-[11px] bg-blue-600 text-white hover:bg-blue-700 shrink-0" @click="openUserCreate">
                + Register
            </Button>
        </div>

        <!-- User Table -->
        <div class="flex-1 overflow-hidden">
            <UserTable :dept-id="deptId" :pane-index="paneIndex" />
        </div>
    </div>
</template>
