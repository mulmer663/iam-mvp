<script setup lang="ts">
import {ChevronRight, Home, User, X} from 'lucide-vue-next'
import {useMillerStore} from '@/stores/miller'
import AppSidebar from '@/components/layout/AppSidebar.vue'
import {Separator} from '@/components/ui/separator'
import {Button} from '@/components/ui/button'
import {SYSTEM_THEMES} from '@/utils/theme'
import {Breadcrumb, BreadcrumbItem, BreadcrumbLink, BreadcrumbList, BreadcrumbPage, BreadcrumbSeparator,} from '@/components/ui/breadcrumb'
import {SidebarInset, SidebarProvider, SidebarTrigger,} from '@/components/ui/sidebar'

// Dynamic View Components
import OrgUserManagement from '@/views/OrgUserManagement.vue'
import DeptManagement from '@/views/DeptManagement.vue'
import SyncDetail from '@/views/SyncDetail.vue'
import SyncHistory from '@/views/SyncHistory.vue'
import UserChangeHistory from '@/views/UserChangeHistory.vue'
import UserProfileViewer from '@/components/common/UserProfileViewer.vue'

import {nextTick, ref, watch} from 'vue'

// ... existing imports

const millerStore = useMillerStore()
const scrollContainer = ref<HTMLElement | null>(null)

// Auto-scroll logic for active pane
watch(
  () => millerStore.activePaneId,
  async (newId) => {
    if (!newId) return
    await nextTick()
    
    // Find the element for the active pane
    const activeEl = document.getElementById(`pane-${newId}`)
    if (activeEl && scrollContainer.value) {
      // Bring the active pane into view
      // Using center alignment makes it clear where the focus is
      activeEl.scrollIntoView({
        behavior: 'smooth',
        inline: 'center',
        block: 'nearest'
      })
    }
  }
)

const VIEW_COMPONENTS: Record<string, any> = {
  OrgUserManagement,
  DeptManagement,
  SyncDetail,
  SyncHistory,
  UserChangeHistory
}

function closePane(index: number) {
  millerStore.removePane(index)
}

function activatePane(id: string) {
  millerStore.activePaneId = id
}

function pushChildPane(parentIndex: number, type: string, title: string, data: any = {}) {
  const nextPane = {
    id: `pane-${Date.now()}`,
    type,
    title,
    data
  }
  millerStore.setPane(parentIndex + 1, nextPane)
}
</script>

<template>
  <SidebarProvider class="h-screen w-full bg-neutral-50 overflow-hidden font-sans text-neutral-900">
    <!-- Sidebar -->
    <AppSidebar />

    <SidebarInset class="flex flex-col min-w-0 overflow-hidden bg-transparent">
      <!-- Top Navigation Bar -->
      <header class="h-10 border-b border-neutral-200 bg-white flex items-center px-4 shrink-0 justify-between">
        <div class="flex items-center gap-2">
           <SidebarTrigger class="-ml-1" />
           <Separator orientation="vertical" class="mr-2 h-4" />
           <Breadcrumb>
            <BreadcrumbList>
              <BreadcrumbItem>
                <BreadcrumbLink href="/" class="flex items-center gap-1">
                  <Home class="size-3" />
                  IAM CORE
                </BreadcrumbLink>
              </BreadcrumbItem>
              <BreadcrumbSeparator />
              <BreadcrumbItem>
                <BreadcrumbPage>Dashboard</BreadcrumbPage>
              </BreadcrumbItem>
              <BreadcrumbSeparator />
              <BreadcrumbItem v-if="millerStore.panes.length > 0">
                 <BreadcrumbPage>{{ millerStore.panes[0]?.title }}</BreadcrumbPage>
              </BreadcrumbItem>
            </BreadcrumbList>
          </Breadcrumb>
        </div>
        
        <!-- Global Search / Actions -->
        <div class="flex items-center gap-2">
           <div class="relative group">
              <div class="flex items-center gap-1 text-[10px] text-neutral-400 bg-neutral-50 px-2 py-1 rounded border border-neutral-100 group-hover:border-neutral-300 transition-colors cursor-text w-48">
                 <span class="font-bold">Q</span>
                 <span>Universal Search (Ctrl+K)</span>
              </div>
           </div>
           <Button variant="ghost" size="icon" class="size-7">
              <div class="size-4 rounded-full bg-neutral-200 text-[9px] flex items-center justify-center font-black text-white">A</div>
           </Button>
           <div class="text-[10px] font-medium text-neutral-600">My Page</div>
        </div>
      </header>

      <!-- Miller Columns container -->
      <main ref="scrollContainer" class="flex-1 overflow-x-auto overflow-y-hidden p-2">
        <div class="h-full flex gap-2 w-max">
           <TransitionGroup name="pane">
             <div 
               v-for="(pane, index) in millerStore.panes" 
               :key="index"
               :id="`pane-${pane.id}`"
               class="h-full shrink-0 transition-all duration-300 ease-out pane-enter-active pane-leave-active"
               :style="{ width: pane.width || '600px', maxWidth: pane.maxWidth || '600px' }"
               @click.capture="activatePane(pane.id)"
             >
               <Transition name="pane-content" mode="out-in">
                 <div 
                   :key="pane.id"
                   class="h-full bg-white border border-neutral-200 rounded-lg shadow-sm flex flex-col"
                   :class="[
                      millerStore.activePaneId === pane.id ? 'ring-2 ring-blue-500/20 border-blue-400 shadow-md z-10' : 'opacity-80 hover:opacity-100',
                      pane.type === 'UserDetail' ? 'w-full' : '' 
                   ]"
                 >
                   <!-- Pane Header -->
                   <div class="h-9 px-3 flex items-center justify-between border-b border-neutral-100 bg-neutral-50/50 rounded-t-lg shrink-0">
                      <div class="flex items-center gap-2 overflow-hidden">
                         <Badge variant="outline" class="h-4 text-[9px] bg-white text-neutral-500 px-1 border-neutral-200 font-mono">LVL.{{ index + 1 }}</Badge>
                         <span class="text-[11px] font-bold text-neutral-600 uppercase tracking-tight truncate max-w-[200px]" :title="pane.title">
                           {{ pane.title }}
                         </span>
                      </div>
                      <div class="flex items-center gap-1">
                         <Button variant="ghost" size="icon" class="size-5 hover:bg-neutral-200 rounded-sm" @click.stop="closePane(index)">
                            <X class="size-3 text-neutral-400" />
                         </Button>
                      </div>
                   </div>

                   <!-- Pane Content -->
                   <div class="flex-1 overflow-hidden relative">
                      <component 
                        v-if="VIEW_COMPONENTS[pane.type]"
                        :is="VIEW_COMPONENTS[pane.type]" 
                        v-bind="pane.data"
                        :paneIndex="index"
                      />
                      
                      <!-- Specialized UserDetail View wrapping UserProfileViewer -->
                      <div v-else-if="pane.type === 'UserDetail' && pane.data.user" class="h-full flex flex-col">
                         <!-- Header -->
                         <div class="p-4 bg-neutral-50/50 border-b border-neutral-100 flex items-center gap-4 shrink-0">
                            <div class="size-10 bg-white border border-neutral-200 rounded-md flex items-center justify-center text-blue-600 shadow-sm">
                               <User class="size-5" />
                            </div>
                            <div class="min-w-0">
                               <div class="text-sm font-black text-neutral-900 leading-tight uppercase tracking-tight">{{ pane.data.user.name.givenName }} {{ pane.data.user.name.familyName }}</div>
                               <div class="text-[11px] text-neutral-400 font-mono mt-0.5 truncate">{{ pane.data.user.emails[0]?.value }}</div>
                            </div>
                         </div>
                         
                         <!-- Body -->
                         <div class="flex-1 p-4 overflow-y-auto">
                           <UserProfileViewer 
                             :data="pane.data.user" 
                             :title="'Core Attributes'"
                           >
                             <template #footer>
                                <!-- Mock Entitlements (since strictly they are not in user object) -->
                                <section class="opacity-50 pointer-events-none mb-4">
                                  <div class="text-[10px] font-black text-neutral-400 uppercase tracking-widest mb-3 flex items-center gap-2">
                                    <div class="size-1 bg-neutral-300 rounded-full"></div> Entitlements & Roles
                                  </div>
                                  <div class="flex flex-wrap gap-2 text-[10px] font-bold">
                                    <div class="px-2 py-1 bg-neutral-100 text-neutral-500 rounded-sm">SYS_ADMIN</div>
                                    <div class="px-2 py-1 bg-neutral-100 text-neutral-500 rounded-sm">DEPT_MANAGER</div>
                                    <div class="px-2 py-1 bg-neutral-100 text-neutral-500 rounded-sm">HR_EDITOR</div>
                                  </div>
                                </section>

                                <!-- Actions -->
                                <section>
                                  <div class="grid grid-cols-1 gap-2">
                                     <Button 
                                      @click="pushChildPane(index, 'SyncHistory', SYSTEM_THEMES.SOURCE.label + ': ' + pane.data.user.name.givenName, { userId: pane.data.user.id, userName: pane.data.user.userName, type: 'SOURCE' })"
                                      variant="outline" size="xs" class="justify-between group/btn text-neutral-600 bg-neutral-50/50"
                                    >
                                      <span class="flex items-center gap-2">
                                        <div class="size-1 rounded-full" :class="SYSTEM_THEMES.SOURCE.indicator"></div> 
                                        {{ SYSTEM_THEMES.SOURCE.label }}
                                      </span>
                                      <ChevronRight class="size-3 opacity-0 group-hover/btn:opacity-100 transition-opacity" />
                                    </Button>
                                    <Button 
                                      @click="pushChildPane(index, 'SyncHistory', SYSTEM_THEMES.INTEGRATION.label + ': ' + pane.data.user.name.givenName, { userId: pane.data.user.id, userName: pane.data.user.userName, type: 'INTEGRATION' })"
                                      variant="outline" size="xs" class="justify-between group/btn text-neutral-600 bg-neutral-50/50"
                                    >
                                      <span class="flex items-center gap-2">
                                        <div class="size-1 rounded-full" :class="SYSTEM_THEMES.INTEGRATION.indicator"></div> 
                                        {{ SYSTEM_THEMES.INTEGRATION.label }}
                                      </span>
                                      <ChevronRight class="size-3 opacity-0 group-hover/btn:opacity-100 transition-opacity" />
                                    </Button>
                                    <Button 
                                      @click="pushChildPane(index, 'UserChangeHistory', SYSTEM_THEMES.AUDIT.label + ': ' + pane.data.user.name.givenName, { userId: pane.data.user.id, userName: pane.data.user.userName })"
                                      variant="outline" size="xs" class="justify-between group/btn text-neutral-600 bg-neutral-50/50"
                                    >
                                      <span class="flex items-center gap-2">
                                        <div class="size-1 rounded-full" :class="SYSTEM_THEMES.AUDIT.indicator"></div> 
                                        {{ SYSTEM_THEMES.AUDIT.label }}
                                      </span>
                                      <ChevronRight class="size-3 opacity-0 group-hover/btn:opacity-100 transition-opacity" />
                                    </Button>
                                  </div>
                                </section>
                             </template>
                           </UserProfileViewer>
                         </div>
                      </div>
                      
                      <div v-else class="flex items-center justify-center h-full text-neutral-300 italic text-[11px] p-10 text-center">
                         MODULE: {{ pane.type }}<br/>
                         (View Implementation Pending)
                      </div>
                   </div>
                 </div>
               </Transition>
             </div>
           </TransitionGroup>
        </div>
      </main>

      <!-- Status Bar -->
      <footer class="h-6 border-t border-neutral-200 bg-white flex items-center px-3 shrink-0 text-[10px] text-neutral-400 font-medium uppercase">
        <div class="flex items-center gap-4">
           <div class="flex items-center gap-1.5"><div class="size-1.5 rounded-full bg-green-500"></div>AGENT ONLINE</div>
           <Separator orientation="vertical" class="h-3 shadow-none bg-neutral-100" />
           <span>THREADS: 24 ACTIVE</span>
           <span>|</span>
           <span>ENV: MVP-DEVELOPMENT</span>
        </div>
        <div class="ml-auto text-[9px] text-neutral-300">v1.0.0-AMIT</div>
      </footer>
    </SidebarInset>
  </SidebarProvider>
</template>

<style scoped>
.pane-enter-active,
.pane-leave-active {
  transition: all 0.3s ease;
}

.pane-enter-from,
.pane-leave-to {
  opacity: 0;
  transform: translateX(20px);
}

/* Internal content transition for same-level replacement */
.pane-content-enter-active,
.pane-content-leave-active {
  transition: all 0.2s ease-in-out;
}

.pane-content-enter-from {
  opacity: 0;
  transform: scale(0.98);
}

.pane-content-leave-to {
  opacity: 0;
  transform: scale(1.02);
}
</style>
