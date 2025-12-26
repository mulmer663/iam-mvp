<script setup lang="ts">
import { UserService } from '@/api/UserService'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { MoreHorizontal, Mail, User as LucideUser } from 'lucide-vue-next'
import { useMillerStore } from '@/stores/miller'
import { computed, onMounted, ref } from 'vue'
import type { User } from '@/types'

const props = defineProps<{
  deptId?: string
  paneIndex?: number
}>()

const millerStore = useMillerStore()
const users = ref<User[]>([])

onMounted(async () => {
    try {
        users.value = await UserService.getUsers()
    } catch (e) {
        console.error('Failed to load users', e)
    }
})

const filteredUsers = computed((): User[] => {
  if (!props.deptId) return users.value
  return users.value.filter(u => u['urn:ietf:params:scim:schemas:extension:enterprise:2.0:User']?.department === props.deptId)
})

// Check if a user is "selected" (has an open detail pane in the stack)
const selectedUserId = computed(() => {
  const detailPane = millerStore.panes.find(p => p.type === 'UserDetail' && p.data.user)
  return detailPane?.data.user.id
})

function openUserDetail(user: User) {
  const detailPane = {
    id: `user-detail-${user.id}`,
    type: 'UserDetail',
    title: `User: ${user.name.givenName} ${user.name.familyName}`,
    data: { user }
  }

  // If this component is in a Miller Pane (paneIndex defined), replace the NEXT pane
  if (typeof props.paneIndex === 'number') {
     millerStore.setPane(props.paneIndex + 1, detailPane)
  } else {
    // Fallback if used outside of strict Miller context (shouldn't happen in App.vue)
    millerStore.pushPane(detailPane)
  }
}
</script>

<template>
  <div class="h-full flex flex-col">
    <div class="flex-1 overflow-auto border rounded-md">
      <Table class="iam-table">
        <TableHeader class="bg-neutral-50/80 sticky top-0 z-10">
          <TableRow class="hover:bg-transparent h-8">
            <TableHead class="w-[30px] p-0 text-center"><MoreHorizontal class="size-3 inline-block" /></TableHead>
            <TableHead class="text-[10px] font-extrabold uppercase p-2">Name</TableHead>
            <TableHead class="text-[10px] font-extrabold uppercase p-2">Position</TableHead>
            <TableHead class="text-[10px] font-extrabold uppercase p-2 text-center">Status</TableHead>
            <TableHead class="text-[10px] font-extrabold uppercase p-2">Email</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          <TableRow 
            v-for="user in filteredUsers" 
            :key="user.id" 
            @click="openUserDetail(user)"
            class="h-8 cursor-pointer group border-b border-neutral-50 transition-colors"
            :class="[selectedUserId === user.id ? 'bg-blue-50/50' : 'hover:bg-neutral-50']"
          >
            <TableCell class="p-0 text-center">
               <LucideUser class="size-3 inline-block text-neutral-300 group-hover:text-blue-500" />
            </TableCell>
            <TableCell class="p-2 py-1">
              <div class="flex flex-col">
                 <span class="text-[12px] font-semibold text-neutral-800 leading-tight">{{ user.name.givenName }} {{ user.name.familyName }}</span>
                 <span class="text-[10px] text-neutral-400 font-mono">{{ user.userName }}</span>
              </div>
            </TableCell>
            <TableCell class="p-2 py-1">
              <span class="text-[11px] text-neutral-600 font-medium">{{ user.title }}</span>
            </TableCell>
            <TableCell class="p-2 py-1 text-center">
              <span class="px-1.5 py-0 h-4 text-[9px] uppercase font-bold tracking-wider rounded-sm"
                    :class="user.active ? 'bg-blue-100 text-blue-700' : 'bg-neutral-100 text-neutral-400'">
                {{ user.active ? 'ACTIVE' : 'INACTIVE' }}
              </span>
            </TableCell>
            <TableCell class="p-2 py-1 relative">
              <div class="flex items-center gap-1.5">
                 <Mail class="size-3 text-neutral-300" />
                 <span class="text-[11px] text-neutral-500">{{ user.emails[0]?.value }}</span>
              </div>
              <!-- Connection Indicator -->
              <div v-if="selectedUserId === user.id" class="absolute right-0 top-0 bottom-0 w-0.5 bg-blue-600"></div>
            </TableCell>
          </TableRow>
        </TableBody>
      </Table>
    </div>
  </div>
</template>

<style>
.iam-table {
  border-collapse: separate;
  border-spacing: 0;
}
.iam-table th, .iam-table td {
  border-bottom-width: 1px;
}
</style>
