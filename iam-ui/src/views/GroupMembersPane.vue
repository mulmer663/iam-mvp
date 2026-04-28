<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Users as UsersIcon, UserPlus, X, Search, Loader } from 'lucide-vue-next'
import { GroupService, type Group } from '@/api/GroupService'
import { UserService } from '@/api/UserService'
import { toast } from '@/utils/toast'
import type { User } from '@/types'

const props = defineProps<{
    groupId: string
    groupName: string
    paneIndex?: number
}>()

const group = ref<Group | null>(null)
const allUsers = ref<User[]>([])
const loading = ref(false)
const saving = ref(false)
const searchQuery = ref('')

type Member = { value: string; display?: string; type?: string }

const members = computed<Member[]>(() => group.value?.members ?? [])

const memberIds = computed(() => new Set(members.value.map(m => m.value)))

const filteredUsers = computed(() => {
    const q = searchQuery.value.trim().toLowerCase()
    return allUsers.value.filter(u => {
        if (memberIds.value.has(u.id)) return false
        if (!q) return true
        const name = `${u.name?.givenName ?? ''} ${u.name?.familyName ?? ''}`.toLowerCase()
        return name.includes(q) || u.userName.toLowerCase().includes(q)
    })
})

onMounted(async () => {
    loading.value = true
    try {
        const [g, users] = await Promise.all([
            GroupService.getGroup(props.groupId),
            UserService.getUsers()
        ])
        group.value = g
        allUsers.value = users
    } finally {
        loading.value = false
    }
})

async function addMember(user: User) {
    if (!group.value) return
    saving.value = true
    try {
        const updated = await GroupService.updateGroup(group.value.id, {
            ...group.value,
            members: [
                ...members.value,
                { value: user.id, display: `${user.name?.givenName ?? ''} ${user.name?.familyName ?? ''}`.trim() || user.userName, type: 'User' }
            ]
        })
        group.value = updated
        toast.success(`Added: ${user.userName}`)
    } catch (e: any) {
        toast.error(e?.message || 'Failed to add member')
    } finally {
        saving.value = false
    }
}

async function removeMember(memberId: string) {
    if (!group.value) return
    saving.value = true
    try {
        const updated = await GroupService.updateGroup(group.value.id, {
            ...group.value,
            members: members.value.filter(m => m.value !== memberId)
        })
        group.value = updated
        toast.success('Member removed')
    } catch (e: any) {
        toast.error(e?.message || 'Failed to remove member')
    } finally {
        saving.value = false
    }
}
</script>

<template>
    <div class="h-full flex flex-col bg-white">
        <!-- Header -->
        <div class="border-b border-neutral-100 bg-neutral-50/60 px-4 py-3 shrink-0 flex items-center gap-2">
            <div class="size-7 rounded-md bg-blue-50 border border-blue-100 flex items-center justify-center shrink-0">
                <UsersIcon class="size-3.5 text-blue-500" />
            </div>
            <div class="min-w-0">
                <div class="font-bold text-neutral-800 text-[13px] truncate">{{ groupName }}</div>
                <div class="text-[10px] text-neutral-400">Members</div>
            </div>
        </div>

        <div v-if="loading" class="flex-1 flex items-center justify-center">
            <div class="size-5 border-2 border-blue-600/20 border-t-blue-600 rounded-full animate-spin"></div>
        </div>

        <template v-else>
            <!-- Current Members -->
            <div class="border-b border-neutral-100 shrink-0">
                <div class="h-8 flex items-center px-3 bg-neutral-50/30">
                    <span class="text-[10px] font-bold text-neutral-500 uppercase tracking-wider flex-1">
                        Current Members
                        <span class="ml-1 text-neutral-400 font-normal">({{ members.length }})</span>
                    </span>
                </div>

                <div v-if="members.length === 0" class="px-3 py-4 text-[11px] text-neutral-300 italic text-center">
                    No members yet
                </div>

                <div v-else class="max-h-52 overflow-y-auto divide-y divide-neutral-50">
                    <div v-for="m in members" :key="m.value"
                         class="flex items-center gap-2 px-3 py-2 hover:bg-neutral-50 group">
                        <div class="size-6 rounded bg-neutral-100 flex items-center justify-center shrink-0">
                            <UsersIcon class="size-3 text-neutral-400" />
                        </div>
                        <div class="flex-1 min-w-0">
                            <div class="text-[11px] font-medium text-neutral-800 truncate">{{ m.display }}</div>
                            <div class="text-[9px] text-neutral-400 font-mono truncate">{{ m.value }}</div>
                        </div>
                        <button @click="removeMember(m.value)" :disabled="saving"
                            class="size-5 flex items-center justify-center rounded opacity-0 group-hover:opacity-100 hover:bg-red-50 hover:text-red-500 text-neutral-300 transition-all disabled:pointer-events-none">
                            <X class="size-3" />
                        </button>
                    </div>
                </div>
            </div>

            <!-- Add Members (user search) -->
            <div class="flex flex-col flex-1 min-h-0">
                <div class="h-8 flex items-center px-3 bg-neutral-50/30 border-b border-neutral-100 shrink-0">
                    <UserPlus class="size-3 text-neutral-400 mr-1.5" />
                    <span class="text-[10px] font-bold text-neutral-500 uppercase tracking-wider">Add Member</span>
                </div>

                <!-- Search box -->
                <div class="px-3 py-2 border-b border-neutral-50 shrink-0">
                    <div class="relative">
                        <Search class="absolute left-2 top-1/2 -translate-y-1/2 size-3 text-neutral-300" />
                        <input v-model="searchQuery" type="text" placeholder="Search users…"
                            class="w-full h-7 pl-7 pr-2 text-[11px] bg-neutral-50 border border-neutral-100 rounded focus:bg-white focus:border-blue-300 outline-none transition-all" />
                    </div>
                </div>

                <!-- User list -->
                <div class="flex-1 overflow-y-auto divide-y divide-neutral-50">
                    <div v-if="filteredUsers.length === 0" class="py-8 text-center text-[11px] text-neutral-300 italic">
                        {{ searchQuery ? 'No matching users' : 'All users already members' }}
                    </div>

                    <div v-for="user in filteredUsers" :key="user.id"
                         class="flex items-center gap-2 px-3 py-2 hover:bg-blue-50/40 cursor-pointer group transition-colors"
                         @click="addMember(user)">
                        <div class="size-6 rounded bg-blue-50 flex items-center justify-center shrink-0">
                            <UsersIcon class="size-3 text-blue-400" />
                        </div>
                        <div class="flex-1 min-w-0">
                            <div class="text-[11px] font-medium text-neutral-800 truncate">
                                {{ user.name?.givenName }} {{ user.name?.familyName }}
                            </div>
                            <div class="text-[9px] text-neutral-400 font-mono truncate">{{ user.userName }}</div>
                        </div>
                        <div class="shrink-0 opacity-0 group-hover:opacity-100 transition-opacity">
                            <Loader v-if="saving" class="size-3 text-blue-400 animate-spin" />
                            <UserPlus v-else class="size-3 text-blue-400" />
                        </div>
                    </div>
                </div>
            </div>
        </template>
    </div>
</template>
