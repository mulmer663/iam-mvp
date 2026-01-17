<script setup lang="ts">
import {Activity, Clock, Database, Network, UserCircle, Users} from 'lucide-vue-next'
import {Sidebar, SidebarContent, SidebarGroup, SidebarGroupContent, SidebarGroupLabel, SidebarHeader, SidebarMenu, SidebarMenuButton, SidebarMenuItem, SidebarRail,} from '@/components/ui/sidebar'
import {useMillerStore} from '@/stores/miller'
import type {Component} from 'vue'

// Types
interface SidebarSubItem {
  title: string
  icon: Component
  view: string
}

interface SidebarGroupItem {
  title: string
  items: (SidebarSubItem & { data?: any })[]
}

const millerStore = useMillerStore()

const items: SidebarGroupItem[] = [
  {
    title: 'Management',
    items: [
      { title: 'Users & Org', icon: Users, view: 'OrgUserManagement' },
      { title: 'Departments', icon: Network, view: 'DeptManagement' },
    ],
  },
  {
    title: 'History & Logs',
    items: [
      { title: 'Source Sync', icon: Database, view: 'SyncHistory', data: { type: 'SOURCE' } },
      { title: 'Integration Sync', icon: Activity, view: 'SyncHistory', data: { type: 'INTEGRATION' } },
      { title: 'Audit Logs', icon: Clock, view: 'UserChangeHistory' },
    ],
  },
]

function handleItemClick(item: any) {
  // Reset stack with new root view
  millerStore.panes = []
    millerStore.pushPane({
      id: `root-${item.view}-${item.data?.type || 'main'}`,
      type: item.view,
      title: item.title,
      data: item.data || {},
      width: item.view === 'OrgUserManagement' ? '800px' : (item.view === 'DeptManagement' ? '350px' : undefined),
      maxWidth: item.view === 'OrgUserManagement' ? '800px' : (item.view === 'DeptManagement' ? '350px' : undefined)
    })
}
</script>

<template>
  <Sidebar collapsible="icon" class="border-r border-neutral-200">
    <SidebarHeader class="h-10 px-4 flex items-center border-b border-neutral-100 bg-white">
      <div class="flex items-center gap-2 overflow-hidden">
        <div class="size-6 bg-blue-600 rounded-sm flex items-center justify-center text-white shrink-0 font-bold text-xs">I</div>
        <span class="font-bold text-neutral-800 tracking-tight whitespace-nowrap group-data-[collapsible=icon]:hidden">IAM CORE</span>
      </div>
    </SidebarHeader>

    <SidebarContent class="bg-white">
      <SidebarGroup v-for="group in items" :key="group.title">
        <SidebarGroupLabel class="text-[10px] uppercase text-neutral-400 font-bold px-4 mt-2">
          {{ group.title }}
        </SidebarGroupLabel>
        <SidebarGroupContent>
          <SidebarMenu>
            <SidebarMenuItem v-for="item in group.items" :key="item.title">
              <SidebarMenuButton 
                @click="handleItemClick(item)"
                class="hover:bg-neutral-50 px-4 h-8"
              >
                <component :is="item.icon" class="size-4" />
                <span class="text-xs font-medium text-neutral-700">{{ item.title }}</span>
              </SidebarMenuButton>
            </SidebarMenuItem>
          </SidebarMenu>
        </SidebarGroupContent>
      </SidebarGroup>
    </SidebarContent>

    <div class="mt-auto p-4 border-t border-neutral-100 group-data-[collapsible=icon]:p-2">
       <div class="flex items-center gap-3 overflow-hidden">
          <UserCircle class="size-8 text-neutral-400 shrink-0" />
          <div class="flex flex-col min-w-0 group-data-[collapsible=icon]:hidden">
            <span class="text-xs font-semibold text-neutral-800 truncate">Administrator</span>
            <span class="text-[10px] text-neutral-400 truncate">admin@iam.corp</span>
          </div>
       </div>
    </div>
    <SidebarRail />
  </Sidebar>
</template>
