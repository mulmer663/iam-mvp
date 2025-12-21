<script setup lang="ts">
import { useMillerStore } from './stores/miller'
import { onMounted, defineAsyncComponent } from 'vue'
import AppSidebar from './components/AppSidebar.vue'
import { SidebarProvider, SidebarTrigger, SidebarInset } from '@/components/ui/sidebar'
import { Button } from '@/components/ui/button'
import { Search, Bell, HelpCircle, ChevronRight, User } from 'lucide-vue-next'
import { Separator } from '@/components/ui/separator'

// Async views mapping
const VIEW_COMPONENTS: Record<string, any> = {
  OrgUserManagement: defineAsyncComponent(() => import('./views/OrgUserManagement.vue')),
  SourceSyncHistory: defineAsyncComponent(() => import('./views/History.vue')),
  IntegrationSyncHistory: defineAsyncComponent(() => import('./views/History.vue')),
  UserAuditLogs: defineAsyncComponent(() => import('./views/History.vue')),
}

const millerStore = useMillerStore()

onMounted(() => {
  millerStore.pushPane({
    id: 'org-user-main',
    type: 'OrgUserManagement',
    title: 'Users & Org',
    data: {}
  })
})

function getHistoryType(viewType: string) {
  if (viewType === 'SourceSyncHistory') return 'SOURCE'
  if (viewType === 'IntegrationSyncHistory') return 'INTEGRATION'
  return 'AUDIT'
}

function closePane(index: number) {
  const prevPane = millerStore.panes[index - 1]
  if (prevPane) {
    millerStore.popToPane(prevPane.id)
  }
}
</script>

<template>
  <SidebarProvider>
    <AppSidebar />
    <SidebarInset class="h-screen overflow-hidden bg-neutral-50">
      <header class="h-10 bg-white border-b border-neutral-200 flex items-center px-4 justify-between shrink-0">
          <div class="flex items-center gap-4">
            <SidebarTrigger class="hover:bg-neutral-100 p-1 rounded-md" />
            <Separator orientation="vertical" class="h-4" />
            <div class="flex items-center gap-1 text-[11px] text-neutral-400">
              <span class="hover:text-neutral-900 cursor-pointer">Dashboard</span>
              <ChevronRight class="size-3" />
              <span class="text-neutral-900 font-medium">{{ millerStore.panes[0]?.title || 'Loading...' }}</span>
            </div>
          </div>

          <div class="flex items-center gap-3">
             <div class="relative group">
                <Search class="absolute left-2 top-1/2 -translate-y-1/2 size-3 text-neutral-400" />
                <input 
                  type="text" 
                  placeholder="Universal Search (Ctrl+K)" 
                  class="h-7 w-48 bg-neutral-100 border-none rounded-sm pl-7 pr-2 text-[11px] focus:ring-1 focus:ring-blue-500 focus:bg-white transition-all"
                />
             </div>
             <button class="p-1 hover:bg-neutral-100 rounded-md text-neutral-500"><Bell class="size-4" /></button>
             <button class="p-1 hover:bg-neutral-100 rounded-md text-neutral-500"><HelpCircle class="size-4" /></button>
             <Separator orientation="vertical" class="h-4 mx-1" />
             <button class="flex items-center gap-2 hover:bg-neutral-100 p-1 px-2 rounded-md">
                <div class="size-5 bg-blue-100 rounded-full flex items-center justify-center text-blue-700 text-[10px] font-bold">A</div>
                <span class="text-[11px] font-medium text-neutral-700">My Page</span>
             </button>
          </div>
        </header>

        <main class="flex-1 flex overflow-x-auto overflow-y-hidden bg-white/50 scroll-smooth">
          <div 
            v-for="(pane, index) in millerStore.panes" 
            :key="pane.id"
            class="iam-pane border-r border-neutral-200 bg-white flex flex-col shadow-sm last:border-r-0"
            :style="{ minWidth: pane.width || '450px' }"
            :class="[index === millerStore.panes.length - 1 ? 'flex-1 w-full' : '']"
          >
            <div class="h-9 border-b border-neutral-100 bg-neutral-50/30 flex items-center px-3 justify-between shrink-0">
              <div class="flex items-center gap-2">
                <span class="text-[11px] font-bold text-neutral-700 uppercase tracking-tight">{{ pane.title }}</span>
              </div>
              <div class="flex gap-1" v-if="index > 0">
                <Button 
                  @click="closePane(index)"
                  variant="ghost" 
                  size="xs"
                  class="bg-white text-neutral-400 hover:text-red-500 font-bold h-6 px-2 text-[10px]"
                >
                  CLOSE
                </Button>
              </div>
            </div>

            <div class="flex-1 overflow-y-auto">
               <!-- Dynamic view injection -->
               <component 
                 v-if="VIEW_COMPONENTS[pane.type]"
                 :is="VIEW_COMPONENTS[pane.type]" 
                 :type="pane.type.includes('History') || pane.type.includes('Audit') ? getHistoryType(pane.type) : undefined"
                 :pane-index="index"
                 v-bind="pane.data"
               />
               
               <!-- Placeholder for specific detail types if any -->
               <div v-else-if="pane.type === 'UserDetail' && pane.data.user" class="p-4 space-y-4">
                  <div class="flex items-center gap-3">
                     <div class="size-12 bg-neutral-100 rounded-full flex items-center justify-center text-neutral-400">
                        <User class="size-6" />
                     </div>
                     <div>
                        <div class="text-sm font-bold text-neutral-900">{{ pane.data.user.name }}</div>
                        <div class="text-xs text-neutral-500">{{ pane.data.user.email }}</div>
                     </div>
                  </div>
                  <Separator />
                  <div class="grid grid-cols-2 gap-4">
                     <div v-for="(val, key) in pane.data.user" :key="key" class="space-y-1">
                        <div class="text-[10px] uppercase font-bold text-neutral-400">{{ key }}</div>
                        <div class="text-xs text-neutral-800">{{ val }}</div>
                     </div>
                  </div>
               </div>
               
               <div v-else class="flex items-center justify-center h-full text-neutral-300 italic text-[11px] p-10 text-center">
                  MODULE: {{ pane.type }}<br/>
                  (View Implementation Pending)
               </div>
            </div>
          </div>
        </main>

      <footer class="h-6 border-t border-neutral-200 bg-white flex items-center px-3 shrink-0 text-[10px] text-neutral-400 font-medium uppercase">
        <div class="flex items-center gap-4">
           <div class="flex items-center gap-1.5"><div class="size-1.5 rounded-full bg-green-500"></div>AGENT ONLINE</div>
           <Separator orientation="vertical" class="h-3 shadow-none bg-neutral-100" />
           <span>THREADS: 24 ACTIVE</span>
           <span>|</span>
           <span>ENV: MVP-DEVELOPMENT</span>
        </div>
        <div class="ml-auto flex items-center gap-2">
           <span>v1.0.0-MVP</span>
           <Separator orientation="vertical" class="h-2 bg-neutral-200" />
           <span>{{ new Date().toLocaleTimeString() }}</span>
        </div>
      </footer>
    </SidebarInset>
  </SidebarProvider>
</template>

<style>
@reference "tailwindcss";

#app {
  height: 100vh;
  width: 100vw;
}

::-webkit-scrollbar {
  width: 5px;
  height: 5px;
}
::-webkit-scrollbar-track {
  background: transparent;
}
::-webkit-scrollbar-thumb {
  background: var(--color-neutral-200);
  border-radius: 9999px;
}
::-webkit-scrollbar-thumb:hover {
  background: var(--color-neutral-300);
}
</style>
