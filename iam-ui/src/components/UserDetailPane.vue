<script setup lang="ts">
import { ChevronRight, User as UserIcon, Edit2, Save, X } from 'lucide-vue-next'
import { Button } from '@/components/ui/button'
import { useMillerStore } from '@/stores/miller'
import { useAttributeStore } from '@/stores/attribute'
import UserProfileViewer from '@/components/common/UserProfileViewer.vue'
import DynamicUserForm from '@/components/user/DynamicUserForm.vue'
import { SYSTEM_THEMES } from '@/utils/theme'
import { UserService, type ScimPatchOp } from '@/api/UserService'
import { toast } from '@/utils/toast'
import type { User } from '@/types'
import { computed, onMounted, ref } from 'vue'

const props = defineProps<{
    user: User
    paneIndex: number
}>()

const millerStore = useMillerStore()
const attrStore = useAttributeStore()

const editing = ref(false)
const saving = ref(false)
const formData = ref<Record<string, any>>({})
const currentUser = ref<User>(props.user)

const userAttributes = computed(() => attrStore.userAttributes)

onMounted(async () => {
    if (attrStore.userAttributes.length === 0) {
        await attrStore.fetchAttributes('USER')
    }
})

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

function startEdit() {
    formData.value = JSON.parse(JSON.stringify(currentUser.value))
    editing.value = true
}

function cancelEdit() {
    editing.value = false
    formData.value = {}
}

function buildPatchOps(original: Record<string, any>, current: Record<string, any>): ScimPatchOp[] {
    const ops: ScimPatchOp[] = []
    const keys = new Set([...Object.keys(original), ...Object.keys(current)])
    for (const key of keys) {
        if (key === 'id' || key === 'meta' || key === 'schemas') continue
        const before = original[key]
        const after = current[key]
        if (JSON.stringify(before) === JSON.stringify(after)) continue
        if (after === undefined || after === '' || (Array.isArray(after) && after.length === 0)) {
            ops.push({ op: 'remove', path: key })
        } else {
            ops.push({ op: 'replace', path: key, value: after })
        }
    }
    return ops
}

async function saveEdit() {
    saving.value = true
    try {
        const ops = buildPatchOps(currentUser.value, formData.value)
        if (ops.length === 0) {
            toast.success('No changes')
            editing.value = false
            return
        }
        const updated = await UserService.patchUser(currentUser.value.id, ops)
        currentUser.value = updated
        editing.value = false
        toast.success('User updated')
    } catch (e: any) {
        console.error(e)
        toast.error(e?.message || 'Failed to update user')
    } finally {
        saving.value = false
    }
}
</script>

<template>
    <div class="h-full flex flex-col">
        <!-- Header -->
        <div class="p-4 bg-neutral-50/50 border-b border-neutral-100 flex items-center gap-4 shrink-0">
            <div class="size-10 bg-white border border-neutral-200 rounded-md flex items-center justify-center text-blue-600 shadow-sm">
                <UserIcon class="size-5" />
            </div>
            <div class="min-w-0 flex-1">
                <div class="text-sm font-black text-neutral-900 leading-tight uppercase tracking-tight">
                    {{ currentUser.name?.givenName }} {{ currentUser.name?.familyName }}
                </div>
                <div class="text-[11px] text-neutral-400 font-mono mt-0.5 truncate">
                    {{ currentUser.emails?.[0]?.value || '-' }}
                </div>
            </div>
            <div class="flex items-center gap-1.5 shrink-0">
                <Button v-if="!editing" size="xs" variant="outline" class="h-7 text-[11px]" @click="startEdit">
                    <Edit2 class="size-3 mr-1" /> Edit
                </Button>
                <template v-else>
                    <Button size="xs" variant="ghost" class="h-7 text-[11px]" @click="cancelEdit" :disabled="saving">
                        <X class="size-3 mr-1" /> Cancel
                    </Button>
                    <Button size="xs" class="h-7 text-[11px] bg-blue-600 text-white hover:bg-blue-700"
                        @click="saveEdit" :disabled="saving">
                        <Save class="size-3 mr-1" /> {{ saving ? 'Saving…' : 'Save' }}
                    </Button>
                </template>
            </div>
        </div>

        <!-- Body — Edit mode -->
        <div v-if="editing" class="flex-1 p-4 overflow-y-auto">
            <DynamicUserForm v-model="formData" :attributes="userAttributes" mode="edit" />
        </div>

        <!-- Body — View mode -->
        <div v-else class="flex-1 p-4 overflow-y-auto">
            <UserProfileViewer :data="currentUser" :title="'Core Attributes'">
                <template #footer>
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

                    <section>
                        <div class="grid grid-cols-1 gap-2">
                            <Button
                                @click="pushChildPane('SyncHistory', SYSTEM_THEMES.SOURCE.label + ': ' + currentUser.name?.givenName, { userId: currentUser.id, userName: currentUser.userName, type: 'SOURCE' }, '800px')"
                                variant="outline" size="xs"
                                class="justify-between group/btn text-neutral-600 bg-neutral-50/50">
                                <span class="flex items-center gap-2">
                                    <div class="size-1 rounded-full" :class="SYSTEM_THEMES.SOURCE.indicator"></div>
                                    {{ SYSTEM_THEMES.SOURCE.label }}
                                </span>
                                <ChevronRight class="size-3 opacity-0 group-hover/btn:opacity-100 transition-opacity" />
                            </Button>
                            <Button
                                @click="pushChildPane('SyncHistory', SYSTEM_THEMES.INTEGRATION.label + ': ' + currentUser.name?.givenName, { userId: currentUser.id, userName: currentUser.userName, type: 'INTEGRATION' }, '800px')"
                                variant="outline" size="xs"
                                class="justify-between group/btn text-neutral-600 bg-neutral-50/50">
                                <span class="flex items-center gap-2">
                                    <div class="size-1 rounded-full" :class="SYSTEM_THEMES.INTEGRATION.indicator"></div>
                                    {{ SYSTEM_THEMES.INTEGRATION.label }}
                                </span>
                                <ChevronRight class="size-3 opacity-0 group-hover/btn:opacity-100 transition-opacity" />
                            </Button>
                            <Button
                                @click="pushChildPane('UserChangeHistory', SYSTEM_THEMES.AUDIT.label + ': ' + currentUser.name?.givenName, { userId: currentUser.id, userName: currentUser.userName }, '800px')"
                                variant="outline" size="xs"
                                class="justify-between group/btn text-neutral-600 bg-neutral-50/50">
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
