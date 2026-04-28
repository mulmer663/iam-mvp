<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Users as UsersIcon, Lock, Edit2, Save, X, Trash2 } from 'lucide-vue-next'
import { Button } from '@/components/ui/button'
import { useMillerStore } from '@/stores/miller'
import { useAttributeStore } from '@/stores/attribute'
import { useGroupStore } from '@/stores/group'
import { GroupService, type Group } from '@/api/GroupService'
import { toast } from '@/utils/toast'
import type { IamAttributeMeta } from '@/types/attribute'

const props = defineProps<{
    groupId: string
    paneIndex?: number
}>()

const millerStore = useMillerStore()
const attrStore = useAttributeStore()
const groupStore = useGroupStore()

const group = ref<Group | null>(null)
const loading = ref(false)
const editing = ref(false)
const saving = ref(false)
const confirmDelete = ref(false)
const formData = ref<Record<string, any>>({})

onMounted(async () => {
    loading.value = true
    try {
        const [data] = await Promise.all([
            GroupService.getGroup(props.groupId),
            attrStore.groupAttributes.length === 0 ? attrStore.fetchAttributes('GROUP') : Promise.resolve()
        ])
        group.value = data
    } finally {
        loading.value = false
    }
})

// ── Attribute rows ────────────────────────────────────────────────────────────

const groupAttrs = computed(() =>
    attrStore.groupAttributes.filter(a => !a.parentName)
)

const rows = computed(() =>
    groupAttrs.value.map(meta => ({
        meta,
        value: group.value?.[meta.name] ?? null
    }))
)

const editableAttrs = computed(() =>
    groupAttrs.value.filter(a => a.mutability !== 'READ_ONLY')
)

function formatValue(val: any): string {
    if (val === null || val === undefined || val === '') return '—'
    if (typeof val === 'boolean') return val ? 'Active' : 'Inactive'
    if (Array.isArray(val)) {
        if (val.length === 0) return '—'
        return val.map(v => v?.display ?? v?.value ?? JSON.stringify(v)).join(', ')
    }
    return String(val)
}

// ── Edit ──────────────────────────────────────────────────────────────────────

function startEdit() {
    if (!group.value) return
    formData.value = { ...group.value }
    editing.value = true
}

function cancelEdit() {
    editing.value = false
    formData.value = {}
}

async function saveEdit() {
    if (!group.value) return
    saving.value = true
    try {
        const updated = await groupStore.update(group.value.id, formData.value)
        group.value = updated
        editing.value = false
        toast.success('Group updated')
    } catch (e: any) {
        toast.error(e?.message || 'Failed to update')
    } finally {
        saving.value = false
    }
}

// ── Delete ────────────────────────────────────────────────────────────────────

async function doDelete() {
    if (!group.value) return
    saving.value = true
    try {
        await groupStore.remove(group.value.id)
        toast.success(`Deleted: ${group.value.displayName}`)
        millerStore.removePane(props.paneIndex ?? 1)
    } catch (e: any) {
        toast.error(e?.message || 'Failed to delete')
        confirmDelete.value = false
    } finally {
        saving.value = false
    }
}

function inputType(meta: IamAttributeMeta): string {
    if (meta.type === 'INTEGER' || meta.type === 'NUMBER') return 'number'
    return 'text'
}
</script>

<template>
    <div class="h-full flex flex-col bg-white overflow-hidden">
        <div v-if="loading" class="flex-1 flex items-center justify-center">
            <div class="size-5 border-2 border-blue-600/20 border-t-blue-600 rounded-full animate-spin"></div>
        </div>

        <template v-else-if="group">
            <!-- Header -->
            <div class="p-4 border-b border-neutral-100 bg-neutral-50/30 shrink-0">
                <div class="flex items-start justify-between">
                    <div class="flex items-center gap-3">
                        <div class="size-9 bg-white border border-neutral-200 rounded-lg flex items-center justify-center text-blue-600 shadow-sm">
                            <UsersIcon class="size-4" />
                        </div>
                        <div>
                            <h2 class="text-sm font-black text-neutral-900 leading-tight uppercase tracking-tight">
                                {{ group.displayName }}
                            </h2>
                            <div class="text-[9px] text-neutral-400 font-mono mt-0.5">{{ group.id }}</div>
                        </div>
                    </div>
                    <div class="flex items-center gap-1.5">
                        <template v-if="!editing && !confirmDelete">
                            <Button size="xs" variant="outline" class="h-6 text-[10px]" @click="startEdit">
                                <Edit2 class="size-3 mr-1" /> Edit
                            </Button>
                            <Button size="xs" variant="outline" class="h-6 text-[10px] border-red-200 text-red-500 hover:bg-red-50"
                                @click="confirmDelete = true">
                                <Trash2 class="size-3" />
                            </Button>
                        </template>
                        <template v-else-if="editing">
                            <Button size="xs" variant="ghost" class="h-6 text-[10px]" @click="cancelEdit" :disabled="saving">
                                <X class="size-3 mr-1" /> Cancel
                            </Button>
                            <Button size="xs" class="h-6 text-[10px] bg-blue-600 text-white hover:bg-blue-700" @click="saveEdit" :disabled="saving">
                                <Save class="size-3 mr-1" /> {{ saving ? 'Saving…' : 'Save' }}
                            </Button>
                        </template>
                    </div>
                </div>
            </div>

            <!-- Toolbar -->
            <div class="h-9 border-b border-neutral-100 flex items-center px-3 shrink-0 bg-neutral-50/30">
                <span class="text-[10px] font-bold text-neutral-500 uppercase tracking-wider flex-1">
                    Attributes
                    <span class="ml-1 text-neutral-400 font-normal">({{ groupAttrs.length }})</span>
                </span>
            </div>

            <!-- Delete overlay -->
            <div v-if="confirmDelete" class="absolute inset-0 bg-white/95 flex flex-col items-center justify-center gap-3 z-10 p-6 text-center">
                <Trash2 class="size-8 text-red-400" />
                <div class="text-sm font-bold text-neutral-800">Delete "{{ group.displayName }}"?</div>
                <div class="text-[11px] text-neutral-400">This action cannot be undone.</div>
                <div class="flex gap-2 mt-1">
                    <Button variant="outline" size="xs" class="h-7 text-[11px]" @click="confirmDelete = false" :disabled="saving">Cancel</Button>
                    <Button size="xs" class="h-7 text-[11px] bg-red-600 text-white hover:bg-red-700" @click="doDelete" :disabled="saving">
                        <Trash2 class="size-3 mr-1" /> {{ saving ? 'Deleting…' : 'Delete' }}
                    </Button>
                </div>
            </div>

            <!-- View mode -->
            <div v-else-if="!editing" class="flex-1 overflow-y-auto custom-scrollbar">
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
                                :class="formatValue(row.value) !== '—' ? 'text-neutral-800' : 'text-neutral-300 italic'">
                                {{ formatValue(row.value) }}
                            </span>
                        </div>
                    </div>
                </div>
            </div>

            <!-- Edit mode -->
            <div v-else class="flex-1 overflow-y-auto custom-scrollbar p-3 space-y-2">
                <div v-for="attr in editableAttrs" :key="attr.name">
                    <label class="block text-[9px] font-bold text-neutral-400 uppercase tracking-tighter mb-0.5">
                        {{ attr.displayName }}
                        <span v-if="attr.required" class="text-red-400 ml-0.5">*</span>
                        <span v-if="attr.mutability === 'IMMUTABLE'" class="text-neutral-300 ml-1 normal-case font-normal">(immutable)</span>
                    </label>
                    <label v-if="attr.type === 'BOOLEAN'" class="flex items-center gap-2 cursor-pointer">
                        <input type="checkbox" v-model="formData[attr.name]"
                            :disabled="attr.mutability === 'IMMUTABLE'"
                            class="rounded border-neutral-300 text-blue-600" />
                        <span class="text-[11px] text-neutral-600">{{ formData[attr.name] ? 'Active' : 'Inactive' }}</span>
                    </label>
                    <textarea v-else-if="attr.uiComponent === 'textarea'"
                        v-model="formData[attr.name]"
                        rows="2"
                        class="w-full px-2 py-1.5 text-[11px] bg-white border border-neutral-200 rounded focus:border-blue-400 outline-none transition-all resize-none"
                    />
                    <input v-else-if="attr.type !== 'COMPLEX'"
                        :type="inputType(attr)"
                        v-model="formData[attr.name]"
                        :disabled="attr.mutability === 'IMMUTABLE'"
                        :placeholder="attr.description ?? attr.displayName"
                        class="w-full h-7 px-2 text-[11px] bg-white border border-neutral-200 rounded focus:border-blue-400 focus:ring-1 focus:ring-blue-100 outline-none transition-all disabled:bg-neutral-50 disabled:text-neutral-400"
                    />
                    <div v-else class="text-[10px] text-neutral-300 italic px-2 py-1.5 border border-neutral-100 rounded">
                        Complex field — edit via API
                    </div>
                </div>
            </div>
        </template>

        <div v-else class="flex-1 flex items-center justify-center text-[11px] text-neutral-300 italic">
            Group not found
        </div>
    </div>
</template>

<style scoped>
.custom-scrollbar::-webkit-scrollbar { width: 4px; }
.custom-scrollbar::-webkit-scrollbar-track { background: transparent; }
.custom-scrollbar::-webkit-scrollbar-thumb { background: #f1f1f1; border-radius: 10px; }
.custom-scrollbar::-webkit-scrollbar-thumb:hover { background: #e5e5e5; }
</style>
