<script setup lang="ts">
import { onMounted, defineAsyncComponent, computed } from 'vue'
import { useMillerStore } from './stores/miller'
import AppSidebar from './components/layout/AppSidebar.vue'
import { SidebarProvider, SidebarTrigger, SidebarInset } from '@/components/ui/sidebar'
import { Button } from '@/components/ui/button'
import { Search, Bell, HelpCircle, ChevronRight, User } from 'lucide-vue-next'
import { Separator } from '@/components/ui/separator'
import { SYSTEM_THEMES } from '@/utils/theme'

// Async views mapping
const VIEW_COMPONENTS: Record<string, any> = {
  OrgUserManagement: defineAsyncComponent(() => import('./views/OrgUserManagement.vue')),
  SourceSyncHistory: defineAsyncComponent(() => import('./views/SyncHistory.vue')),
  IntegrationSyncHistory: defineAsyncComponent(() => import('./views/SyncHistory.vue')),
  UserAuditLogs: defineAsyncComponent(() => import('./views/SyncHistory.vue')),
  UserChangeHistory: defineAsyncComponent(() => import('./views/SyncHistory.vue')),
  SyncDetail: defineAsyncComponent(() => import('./views/SyncDetail.vue')),
}

const millerStore = useMillerStore()
// Computed
const currentRootTitle = computed(() => millerStore.panes[0]?.title || 'Loading...')

const panes = computed(() => millerStore.panes.map((pane, index) => ({
  ...pane,
  isLast: index === millerStore.panes.length - 1,
  showClose: index > 0
})))

onMounted(() => {
  millerStore.pushPane({
    id: 'org-user-main',
    type: 'OrgUserManagement',
    title: 'Users & Org',
    data: {},
    width: '800px',
    maxWidth: '800px'
  })
})

// Event-driven scroll: Triggered by Vue's animation hooks
const handleScrollToElement = (el: Element, isSmooth = true) => {
  el.scrollIntoView({ 
    behavior: isSmooth ? 'smooth' : 'auto', 
    block: 'nearest', 
    inline: 'end' 
  })
}

// Start scrolling as soon as the transition begins
function onEnter(el: Element) {
  handleScrollToElement(el, true)
}

// Final adjustment when transition is 100% complete
function onAfterEnter(el: Element) {
  handleScrollToElement(el, true)
}

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

function pushChildPane(parentIndex: number, type: string, title: string, data: any = {}) {
  millerStore.setPane(parentIndex + 1, {
    id: `${type.toLowerCase()}-${Date.now()}`,
    type,
    title,
    data
  })
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
              <span class="text-neutral-900 font-medium">{{ currentRootTitle }}</span>
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

        <TransitionGroup 
          name="miller-pane" 
          tag="main"
          @enter="onEnter"
          @after-enter="onAfterEnter"
          class="flex-1 flex overflow-x-auto overflow-y-hidden bg-neutral-100/30 items-stretch relative"
        >
          <div 
            v-for="(pane, index) in panes" 
            :key="pane.id"
            :id="'pane-' + pane.id"
            class="iam-pane border-r border-neutral-200 bg-white flex flex-col shadow-xl last:border-r-0 shrink-0 first:shadow-none transition-all duration-300 relative"
            :style="{ 
              width: pane.width || '450px',
              minWidth: pane.width || '450px',
            }"
            :class="[
              pane.isLast && !pane.maxWidth ? 'flex-1 min-w-[600px]' : '',
              millerStore.highlightedPaneId === pane.id ? 'highlight-flash' : ''
            ]"
          >
            <!-- Compact Header -->
            <div class="h-8 border-b border-neutral-100 bg-neutral-50/50 flex items-center px-3 justify-between shrink-0 group/header">
              <div class="flex items-center gap-2 overflow-hidden">
                <span class="text-[10px] font-bold text-neutral-400 uppercase tracking-widest whitespace-nowrap">LVL.{{ index + 1 }}</span>
                <Separator orientation="vertical" class="h-2 bg-neutral-200" />
                <span class="text-[11px] font-extrabold text-neutral-700 uppercase truncate">{{ pane.title }}</span>
              </div>
              <div class="flex gap-1">
                <Button 
                  v-if="pane.showClose"
                  @click="closePane(index)"
                  variant="ghost" 
                  size="xs"
                  class="h-5 px-1.5 text-[9px] text-neutral-400 hover:text-red-500 hover:bg-red-50 font-bold transition-colors"
                >
                  ESC
                </Button>
              </div>
            </div>

            <div class="flex-1 overflow-y-auto bg-white">
               <!-- Dynamic view injection -->
               <component 
                 v-if="VIEW_COMPONENTS[pane.type]"
                 :is="VIEW_COMPONENTS[pane.type]" 
                 :type="pane.type.includes('History') || pane.type.includes('Audit') ? getHistoryType(pane.type) : undefined"
                 :pane-index="index"
                 v-bind="pane.data"
               />
               
               <!-- High-Density Detail View -->
               <div v-else-if="pane.type === 'UserDetail' && pane.data.user" class="h-full flex flex-col">
                  <div class="p-4 bg-neutral-50/50 border-b border-neutral-100 flex items-center gap-4">
                     <div class="size-10 bg-white border border-neutral-200 rounded-md flex items-center justify-center text-blue-600 shadow-sm">
                        <User class="size-5" />
                     </div>
                     <div class="min-w-0">
                        <div class="text-sm font-black text-neutral-900 leading-tight uppercase tracking-tight">{{ pane.data.user.name.givenName }} {{ pane.data.user.name.familyName }}</div>
                        <div class="text-[11px] text-neutral-400 font-mono mt-0.5 truncate">{{ pane.data.user.emails[0]?.value }}</div>
                     </div>
                  </div>
                  
                  <div class="flex-1 p-4 space-y-6">
                    <section>
                      <h3 class="text-[10px] font-black text-neutral-400 uppercase tracking-widest mb-3 flex items-center gap-2">
                        <div class="size-1 bg-neutral-300 rounded-full"></div> Core Identity
                      </h3>
                      <div class="grid grid-cols-3 gap-y-4 gap-x-2">
                         <div class="space-y-1 border-l border-neutral-100 pl-2">
                            <div class="text-[9px] uppercase font-bold text-neutral-400 tracking-tighter">Given Name</div>
                            <div class="text-[11px] text-neutral-800 font-semibold truncate">{{ pane.data.user.name.givenName }}</div>
                         </div>
                         <div class="space-y-1 border-l border-neutral-100 pl-2">
                            <div class="text-[9px] uppercase font-bold text-neutral-400 tracking-tighter">Family Name</div>
                            <div class="text-[11px] text-neutral-800 font-semibold truncate">{{ pane.data.user.name.familyName }}</div>
                         </div>
                         <div class="space-y-1 border-l border-neutral-100 pl-2">
                            <div class="text-[9px] uppercase font-bold text-neutral-400 tracking-tighter">User Name</div>
                            <div class="text-[11px] text-neutral-800 font-semibold truncate">{{ pane.data.user.userName }}</div>
                         </div>
                         <div class="space-y-1 border-l border-neutral-100 pl-2">
                            <div class="text-[9px] uppercase font-bold text-neutral-400 tracking-tighter">Title</div>
                            <div class="text-[11px] text-neutral-800 font-semibold truncate">{{ pane.data.user.title }}</div>
                         </div>
                         <div class="space-y-1 border-l border-neutral-100 pl-2">
                            <div class="text-[9px] uppercase font-bold text-neutral-400 tracking-tighter">Active</div>
                            <div class="text-[11px] text-neutral-800 font-semibold truncate">{{ pane.data.user.active ? 'Yes' : 'No' }}</div>
                         </div>
                         <div class="space-y-1 border-l border-neutral-100 pl-2">
                            <div class="text-[9px] uppercase font-bold text-neutral-400 tracking-tighter">Primary Email</div>
                            <div class="text-[11px] text-neutral-800 font-semibold truncate">{{ pane.data.user.emails[0]?.value }}</div>
                         </div>
                      </div>
                    </section>

                    <Separator class="bg-neutral-50" />

                    <section v-if="pane.data.user['urn:ietf:params:scim:schemas:extension:enterprise:2.0:User']">
                      <h3 class="text-[10px] font-black text-neutral-400 uppercase tracking-widest mb-3 flex items-center gap-2">
                        <div class="size-1 bg-neutral-300 rounded-full"></div> Enterprise Extension
                      </h3>
                      <div class="grid grid-cols-2 gap-2">
                         <div v-for="(val, key) in pane.data.user['urn:ietf:params:scim:schemas:extension:enterprise:2.0:User']" :key="key" 
                              class="bg-neutral-50 p-2 rounded border border-neutral-100 group hover:border-blue-200 transition-colors">
                            <div class="text-[8px] uppercase font-bold text-neutral-400 mb-0.5">{{ key }}</div>
                            <div class="text-[11px] text-neutral-800 font-medium">{{ val }}</div>
                         </div>
                      </div>
                    </section>

                    <Separator class="bg-neutral-50" />

                    <section class="opacity-50 pointer-events-none">
                      <h3 class="text-[10px] font-black text-neutral-400 uppercase tracking-widest mb-3 flex items-center gap-2">
                        <div class="size-1 bg-neutral-300 rounded-full"></div> Entitlements & Roles
                      </h3>
                      <div class="flex flex-wrap gap-2 text-[10px] font-bold">
                        <div class="px-2 py-1 bg-neutral-100 text-neutral-500 rounded-sm">SYS_ADMIN</div>
                        <div class="px-2 py-1 bg-neutral-100 text-neutral-500 rounded-sm">DEPT_MANAGER</div>
                        <div class="px-2 py-1 bg-neutral-100 text-neutral-500 rounded-sm">HR_EDITOR</div>
                      </div>
                    </section>

                    <Separator class="bg-neutral-50" />

                     <section>
                      <h3 class="text-[10px] font-black text-neutral-400 uppercase tracking-widest mb-3 flex items-center gap-2">
                        <div class="size-1 bg-neutral-300 rounded-full"></div> Continuous Exploration
                      </h3>
                      <div class="grid grid-cols-1 gap-2">
                         <Button 
                          @click="pushChildPane(index, 'SourceSyncHistory', SYSTEM_THEMES.SOURCE.label + ': ' + pane.data.user.name.givenName, { userId: pane.data.user.id, userName: pane.data.user.userName })"
                          variant="outline" size="xs" class="justify-between group/btn text-neutral-600 bg-neutral-50/50"
                        >
                          <span class="flex items-center gap-2">
                            <div class="size-1 rounded-full" :class="SYSTEM_THEMES.SOURCE.indicator"></div> 
                            {{ SYSTEM_THEMES.SOURCE.label }}
                          </span>
                          <ChevronRight class="size-3 opacity-0 group-hover/btn:opacity-100 transition-opacity" />
                        </Button>
                        <Button 
                          @click="pushChildPane(index, 'IntegrationSyncHistory', SYSTEM_THEMES.INTEGRATION.label + ': ' + pane.data.user.name.givenName, { userId: pane.data.user.id, userName: pane.data.user.userName })"
                          variant="outline" size="xs" class="justify-between group/btn text-neutral-600 bg-neutral-50/50"
                        >
                          <span class="flex items-center gap-2">
                            <div class="size-1 rounded-full" :class="SYSTEM_THEMES.INTEGRATION.indicator"></div> 
                            {{ SYSTEM_THEMES.INTEGRATION.label }}
                          </span>
                          <ChevronRight class="size-3 opacity-0 group-hover/btn:opacity-100 transition-opacity" />
                        </Button>
                        <Button 
                          @click="pushChildPane(index, 'UserAuditLogs', SYSTEM_THEMES.AUDIT.label + ': ' + pane.data.user.name.givenName, { userId: pane.data.user.id, userName: pane.data.user.userName })"
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
                  </div>
               </div>
               
               <div v-else class="flex items-center justify-center h-full text-neutral-300 italic text-[11px] p-10 text-center">
                  MODULE: {{ pane.type }}<br/>
                  (View Implementation Pending)
               </div>
            </div>
          </div>
        </TransitionGroup>

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

<style scoped>
#app {
  height: 100vh;
  width: 100vw;
}

@keyframes flash {
  0% { background-color: white; }
  20% { 
    background-color: #eff6ff; 
    box-shadow: 0 0 40px rgba(59, 130, 246, 0.3); 
  }
  100% { background-color: white; }
}

.highlight-flash {
  animation: flash 2s cubic-bezier(0.22, 1, 0.36, 1);
  z-index: 50;
}

/* Miller Pane Transitions */
.miller-pane-enter-active,
.miller-pane-leave-active {
  transition: all 0.4s cubic-bezier(0.16, 1, 0.3, 1);
  overflow: hidden;
  white-space: nowrap;
}

.miller-pane-enter-from,
.miller-pane-leave-to {
  opacity: 0;
  transform: translateX(30px);
  width: 0 !important;
  min-width: 0 !important;
  max-width: 0 !important;
}

.miller-pane-move {
  transition: transform 0.4s cubic-bezier(0.16, 1, 0.3, 1);
}
</style>
