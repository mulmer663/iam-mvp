<script setup lang="ts">
import { MOCK_USERS } from '@/mocks/data'
import { Badge } from '@/components/ui/badge'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { MoreHorizontal, Mail, User } from 'lucide-vue-next'
import { useMillerStore } from '@/stores/miller'
import { computed } from 'vue'

const props = defineProps<{
  deptId?: string
  paneIndex?: number
}>()

const millerStore = useMillerStore()

const filteredUsers = computed(() => {
  if (!props.deptId) return MOCK_USERS
  return MOCK_USERS.filter(u => u.deptCode === props.deptId)
})

const getStatusVariant = (status: string) => {
  switch (status) {
    case 'ACTIVE': return 'default'
    case 'INACTIVE': return 'secondary'
    default: return 'outline'
  }
}

function openUserDetail(user: any) {
  // If this component is in a Miller Pane (paneIndex defined), replace the NEXT pane
  if (typeof props.paneIndex === 'number') {
     millerStore.setPane(props.paneIndex + 1, {
        id: `user-detail-${user.id}`,
        type: 'UserDetail',
        title: `User: ${user.name}`,
        data: { user }
     })
  } else {
    // Fallback if used outside of strict Miller context (shouldn't happen in App.vue)
    millerStore.pushPane({
        id: `user-detail-${user.id}`,
        type: 'UserDetail',
        title: `User: ${user.name}`,
        data: { user }
    })
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
            class="h-8 hover:bg-neutral-50 cursor-pointer group border-b border-neutral-50"
          >
            <TableCell class="p-0 text-center">
               <User class="size-3 inline-block text-neutral-300 group-hover:text-blue-500" />
            </TableCell>
            <TableCell class="p-2 py-1">
              <div class="flex flex-col">
                 <span class="text-[12px] font-semibold text-neutral-800 leading-tight">{{ user.name }}</span>
                 <span class="text-[10px] text-neutral-400 font-mono">{{ user.loginId }}</span>
              </div>
            </TableCell>
            <TableCell class="p-2 py-1">
              <span class="text-[11px] text-neutral-600 font-medium">{{ user.position }}</span>
            </TableCell>
            <TableCell class="p-2 py-1 text-center">
              <Badge 
                :variant="getStatusVariant(user.status)"
                class="px-1.5 py-0 h-4 text-[9px] uppercase font-bold tracking-wider rounded-sm"
              >
                {{ user.status }}
              </Badge>
            </TableCell>
            <TableCell class="p-2 py-1">
              <div class="flex items-center gap-1.5">
                 <Mail class="size-3 text-neutral-300" />
                 <span class="text-[11px] text-neutral-500">{{ user.email }}</span>
              </div>
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
