<script setup lang="ts">
import { ChevronRight, User as UserIcon, Edit2, Save, X, Trash2, Lock } from 'lucide-vue-next'
import { Button } from '@/components/ui/button'
import { useMillerStore } from '@/stores/miller'
import { useAttributeStore } from '@/stores/attribute'
import DynamicUserForm from '@/components/user/DynamicUserForm.vue'
import { SYSTEM_THEMES } from '@/utils/theme'
import { UserService, type ScimPatchOp } from '@/api/UserService'
import { toast } from '@/utils/toast'
import type { User } from '@/types'
import type { IamAttributeMeta } from '@/types/attribute'
import { computed, onMounted, ref } from 'vue'

const props = defineProps<{
    user: User
    paneIndex: number
}>()

const millerStore = useMillerStore()
const attrStore = useAttributeStore()

const editing = ref(false)
const saving = ref(false)
const confirmDelete = ref(false)
const formData = ref<Record<string, any>>({})
const currentUser = ref<User>(props.user)

const userAttributes = computed(() => attrStore.userAttributes)

onMounted(async () => {
    if (attrStore.userAttributes.length === 0) {
        await attrStore.fetchAttributes('USER')
    }
})

// ── Attribute-driven view rows ────────────────────────────────────────────────

const topLevelAttrs = computed(() =>
    userAttributes.value.filter(a => !a.parentName)
)

function resolveValue(meta: IamAttributeMeta): any {
    const u = currentUser.value as any
    if (meta.category === 'EXTENSION' && meta.scimSchemaUri) {
        return u[meta.scimSchemaUri]?.[meta.name] ?? null
    }
    return u[meta.name] ?? null
}

function formatValue(val: any, meta: IamAttributeMeta): string {
    if (val === null || val === undefined || val === '') return '—'
    if (typeof val === 'boolean') return val ? 'Active' : 'Inactive'
    if (Array.isArray(val)) {
        // multiValued COMPLEX (emails, phoneNumbers …)
        if (val.length === 0) return '—'
        return val.map(v => v?.value ?? JSON.stringify(v)).join(', ')
    }
    if (typeof val === 'object') {
        // COMPLEX (name): join sub-attr values
        const subAttrs = userAttributes.value.filter(a => a.parentName === meta.name)
        if (subAttrs.length > 0) {
            return subAttrs.map(s => val[s.name]).filter(Boolean).join(' ') || '—'
        }
        return JSON.stringify(val)
    }
    return String(val)
}

const rows = computed(() =>
    topLevelAttrs.value.map(meta => ({
        meta,
        raw: resolveValue(meta),
        display: formatValue(resolveValue(meta), meta)
    }))
)

// ── Pane navigation ───────────────────────────────────────────────────────────

function pushChildPane(type: string, title: string, data: any = {}, width?: string) {
    millerStore.setPane(props.paneIndex + 1, {
        id: `pane-${Date.now()}`,
        type,
        title,
        data,
        width: width || 'w1'
    })
}

// ── Edit ──────────────────────────────────────────────────────────────────────

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

// ── Delete ────────────────────────────────────────────────────────────────────

async function doDelete() {
    saving.value = true
    try {
        await UserService.deleteUser(currentUser.value.id)
        toast.success(`Deleted: ${currentUser.value.userName}`)
        millerStore.removePane(props.paneIndex)
    } catch (e: any) {
        toast.error(e?.message || 'Failed to delete user')
        confirmDelete.value = false
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
                    {{ currentUser.emails?.[0]?.value || currentUser.userName }}
                </div>
            </div>
            <div class="flex items-center gap-1.5 shrink-0">
                <template v-if="!editing && !confirmDelete">
                    <Button size="xs" variant="outline" class="h-7 text-[11px]" @click="startEdit">
                        <Edit2 class="size-3 mr-1" /> Edit
                    </Button>
                    <Button size="xs" variant="outline" class="h-7 text-[11px] border-red-200 text-red-500 hover:bg-red-50"
                        data-testid="user-delete-btn" @click="confirmDelete = true">
                        <Trash2 class="size-3" />
                    </Button>
                </template>
                <template v-else-if="editing">
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

        <!-- Toolbar -->
        <div v-if="!editing && !confirmDelete"
             class="h-9 border-b border-neutral-100 flex items-center px-3 shrink-0 bg-neutral-50/30">
            <span class="text-[10px] font-bold text-neutral-500 uppercase tracking-wider flex-1">
                Attributes
                <span class="ml-1 text-neutral-400 font-normal">({{ rows.length }})</span>
            </span>
        </div>

        <!-- Body — Edit mode -->
        <div v-if="editing" class="flex-1 p-4 overflow-y-auto">
            <DynamicUserForm v-model="formData" :attributes="userAttributes" mode="edit" />
        </div>

        <!-- Delete confirmation overlay -->
        <div v-if="confirmDelete" class="absolute inset-0 bg-white/95 flex flex-col items-center justify-center gap-3 z-10 p-6 text-center">
            <Trash2 class="size-8 text-red-400" />
            <div class="text-sm font-bold text-neutral-800">Delete "{{ currentUser.userName }}"?</div>
            <div class="text-[11px] text-neutral-400">This action cannot be undone.</div>
            <div class="flex gap-2 mt-1">
                <Button variant="outline" size="xs" class="h-7 text-[11px]" @click="confirmDelete = false" :disabled="saving">
                    Cancel
                </Button>
                <Button size="xs" class="h-7 text-[11px] bg-red-600 text-white hover:bg-red-700" @click="doDelete" :disabled="saving">
                    <Trash2 class="size-3 mr-1" /> {{ saving ? 'Deleting…' : 'Delete' }}
                </Button>
            </div>
        </div>

        <!-- Body — View mode -->
        <div v-else-if="!editing" class="flex-1 overflow-y-auto">

            <!-- Attribute list -->
            <div class="divide-y divide-neutral-50">
                <div v-for="row in rows" :key="row.meta.name"
                     class="flex items-center px-3 py-2 hover:bg-neutral-50/60 transition-colors">
                    <div class="w-36 shrink-0 flex items-center gap-1.5">
                        <span class="text-[10px] font-bold text-neutral-400 uppercase tracking-tighter truncate">
                            {{ row.meta.displayName }}
                        </span>
                        <Lock v-if="row.meta.mutability === 'READ_ONLY' || row.meta.mutability === 'IMMUTABLE'"
                            class="size-2.5 text-neutral-300 shrink-0" />
                    </div>
                    <div class="flex-1 min-w-0">
                        <span class="text-[11px] font-medium truncate block"
                            :class="row.display !== '—' ? 'text-neutral-800' : 'text-neutral-300 italic'">
                            {{ row.display }}
                        </span>
                    </div>
                </div>
            </div>

            <!-- Footer: history navigation -->
            <div class="border-t border-neutral-100 mx-3 mt-4 pt-4 space-y-1.5">
                <div class="text-[9px] font-black text-neutral-400 uppercase tracking-widest mb-2">History</div>
                <Button
                    @click="pushChildPane('SyncHistory', SYSTEM_THEMES.SOURCE.label + ': ' + currentUser.name?.givenName, { userId: currentUser.id, userName: currentUser.userName, type: 'SOURCE' }, 'w2')"
                    variant="outline" size="xs"
                    class="w-full justify-between group/btn text-neutral-600 bg-neutral-50/50">
                    <span class="flex items-center gap-2">
                        <div class="size-1 rounded-full" :class="SYSTEM_THEMES.SOURCE.indicator"></div>
                        {{ SYSTEM_THEMES.SOURCE.label }}
                    </span>
                    <ChevronRight class="size-3 opacity-0 group-hover/btn:opacity-100 transition-opacity" />
                </Button>
                <Button
                    @click="pushChildPane('SyncHistory', SYSTEM_THEMES.INTEGRATION.label + ': ' + currentUser.name?.givenName, { userId: currentUser.id, userName: currentUser.userName, type: 'INTEGRATION' }, 'w2')"
                    variant="outline" size="xs"
                    class="w-full justify-between group/btn text-neutral-600 bg-neutral-50/50">
                    <span class="flex items-center gap-2">
                        <div class="size-1 rounded-full" :class="SYSTEM_THEMES.INTEGRATION.indicator"></div>
                        {{ SYSTEM_THEMES.INTEGRATION.label }}
                    </span>
                    <ChevronRight class="size-3 opacity-0 group-hover/btn:opacity-100 transition-opacity" />
                </Button>
                <Button
                    @click="pushChildPane('UserChangeHistory', SYSTEM_THEMES.AUDIT.label + ': ' + currentUser.name?.givenName, { userId: currentUser.id, userName: currentUser.userName }, 'w2')"
                    variant="outline" size="xs"
                    class="w-full justify-between group/btn text-neutral-600 bg-neutral-50/50">
                    <span class="flex items-center gap-2">
                        <div class="size-1 rounded-full" :class="SYSTEM_THEMES.AUDIT.indicator"></div>
                        {{ SYSTEM_THEMES.AUDIT.label }}
                    </span>
                    <ChevronRight class="size-3 opacity-0 group-hover/btn:opacity-100 transition-opacity" />
                </Button>
            </div>
        </div>
    </div>
</template>
