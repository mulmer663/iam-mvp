<script setup lang="ts">
import {ChevronRight, User as UserIcon} from 'lucide-vue-next'
import {Button} from '@/components/ui/button'
import {useMillerStore} from '@/stores/miller'
import UserProfileViewer from '@/components/common/UserProfileViewer.vue'
import {SYSTEM_THEMES} from '@/utils/theme'
import type {User} from '@/types'

const props = defineProps<{
  user: User
  paneIndex: number
}>()

const millerStore = useMillerStore()

function pushChildPane(type: string, title: string, data: any = {}, width?: string) {
  const nextPane = {
    id: `pane-${Date.now()}`,
    type,
    title,
    data,
    width: width || '500px',
    maxWidth: width || '500px'
  }
  millerStore.setPane(props.paneIndex + 1, nextPane)
}
</script>

<template>
  <div class="h-full flex flex-col">
    <!-- Header -->
    <div class="p-4 bg-neutral-50/50 border-b border-neutral-100 flex items-center gap-4 shrink-0">
      <div class="size-10 bg-white border border-neutral-200 rounded-md flex items-center justify-center text-blue-600 shadow-sm">
        <UserIcon class="size-5" />
      </div>
      <div class="min-w-0">
        <div class="text-sm font-black text-neutral-900 leading-tight uppercase tracking-tight">
          {{ user.name.givenName }} {{ user.name.familyName }}
        </div>
        <div class="text-[11px] text-neutral-400 font-mono mt-0.5 truncate">
          {{ user.emails?.[0]?.value || '-' }}
        </div>
      </div>
    </div>
    
    <!-- Body -->
    <div class="flex-1 p-4 overflow-y-auto">
      <UserProfileViewer 
        :data="user" 
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
                @click="pushChildPane('SyncHistory', SYSTEM_THEMES.SOURCE.label + ': ' + user.name.givenName, { userId: user.id, userName: user.userName, type: 'SOURCE' }, '800px')"
                variant="outline" size="xs" class="justify-between group/btn text-neutral-600 bg-neutral-50/50"
              >
                <span class="flex items-center gap-2">
                  <div class="size-1 rounded-full" :class="SYSTEM_THEMES.SOURCE.indicator"></div> 
                  {{ SYSTEM_THEMES.SOURCE.label }}
                </span>
                <ChevronRight class="size-3 opacity-0 group-hover/btn:opacity-100 transition-opacity" />
              </Button>
              <Button 
                @click="pushChildPane('SyncHistory', SYSTEM_THEMES.INTEGRATION.label + ': ' + user.name.givenName, { userId: user.id, userName: user.userName, type: 'INTEGRATION' }, '800px')"
                variant="outline" size="xs" class="justify-between group/btn text-neutral-600 bg-neutral-50/50"
              >
                <span class="flex items-center gap-2">
                  <div class="size-1 rounded-full" :class="SYSTEM_THEMES.INTEGRATION.indicator"></div> 
                  {{ SYSTEM_THEMES.INTEGRATION.label }}
                </span>
                <ChevronRight class="size-3 opacity-0 group-hover/btn:opacity-100 transition-opacity" />
              </Button>
              <Button 
                @click="pushChildPane('UserChangeHistory', SYSTEM_THEMES.AUDIT.label + ': ' + user.name.givenName, { userId: user.id, userName: user.userName }, '800px')"
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
</template>
