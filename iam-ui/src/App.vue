<script setup lang="ts">
import {Home, X} from 'lucide-vue-next'
import {useMillerStore} from '@/stores/miller'
import AppSidebar from '@/components/layout/AppSidebar.vue'
import {Separator} from '@/components/ui/separator'
import {Button} from '@/components/ui/button'

import {Breadcrumb, BreadcrumbItem, BreadcrumbLink, BreadcrumbList, BreadcrumbPage, BreadcrumbSeparator,} from '@/components/ui/breadcrumb'
import {SidebarInset, SidebarProvider, SidebarTrigger,} from '@/components/ui/sidebar'
import Toaster from '@/components/ui/sonner/Sonner.vue'
import 'vue-sonner/style.css'

// Dynamic View Components
import OrgUserManagement from '@/views/OrgUserManagement.vue'
import DeptManagement from '@/views/DeptManagement.vue'
import DeptDetailPane from '@/views/DeptDetailPane.vue'
import DeptCreatePane from '@/views/DeptCreatePane.vue'
import SyncHistory from '@/views/SyncHistory.vue'
import UserChangeHistory from '@/views/UserChangeHistory.vue'
import AttributeManagement from '@/views/AttributeManagement.vue'
import ResourceManagement from '@/views/ResourceManagement.vue'
import SchemaDetailPane from '@/views/SchemaDetailPane.vue'
import SchemaCreateForm from '@/views/SchemaCreateForm.vue'
import AttributeForm from '@/components/attribute/AttributeForm.vue'
import ResourceTypeForm from '@/components/ResourceTypeForm.vue'
import ResourceTypeDetailPane from '@/views/ResourceTypeDetailPane.vue'
import ResourceTypeCreateForm from '@/views/ResourceTypeCreateForm.vue'
import UserDetailPane from '@/components/UserDetailPane.vue'
import UserCreatePane from '@/views/UserCreatePane.vue'
import GroupManagement from '@/views/GroupManagement.vue'

import {nextTick, ref, watch} from 'vue'
import { useMillerSizes } from '@/composables/useMillerSizes'

const millerStore = useMillerStore()
const scrollContainer = ref<HTMLElement | null>(null)
const { resolveWidth } = useMillerSizes(scrollContainer)

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
  DeptDetailPane,
  DeptCreatePane,
  SyncHistory,
  UserChangeHistory,
  AttributeManagement,
  AttributeManagementPane: AttributeManagement, // Alias for Miller
  AttributeListPane: AttributeManagement, // NEW: Alias for recursive sub-attribute list
  ResourceManagement,
  GroupManagement,
  SchemaDetailPane,
  SchemaCreatePane: SchemaCreateForm,
  AttributeFormPane: AttributeForm,
  ResourceTypeFormPane: ResourceTypeForm,
  ResourceTypeDetailPane,
  ResourceTypeCreatePane: ResourceTypeCreateForm,
  UserDetail: UserDetailPane,
  UserCreatePane
}

function closePane(index: number) {
  millerStore.removePane(index)
}

function activatePane(id: string) {
  millerStore.activePaneId = id
}

</script>

<template>
  <div class="h-screen w-full flex flex-col">
    <SidebarProvider class="flex-1 bg-neutral-50 overflow-hidden font-sans text-neutral-900">
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
                 :style="{ width: resolveWidth(pane.width), maxWidth: resolveWidth(pane.width) }"
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
    <Toaster position="top-center" />
  </div>
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
