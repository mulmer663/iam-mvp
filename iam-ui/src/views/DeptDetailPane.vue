<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Network, Lock, Users as UsersIcon, ChevronRight, Edit2, Save, X, Trash2 } from 'lucide-vue-next'
import { Button } from '@/components/ui/button'
import { useMillerStore } from '@/stores/miller'
import { useAttributeStore } from '@/stores/attribute'
import { useDeptStore } from '@/stores/department'
import { DepartmentService } from '@/api/DepartmentService'
import { toast } from '@/utils/toast'
import type { Department } from '@/types'
import type { IamAttributeMeta } from '@/types/attribute'

const props = defineProps<{
    deptId: string
    paneIndex?: number
}>()

const millerStore = useMillerStore()
const attrStore = useAttributeStore()
const deptStore = useDeptStore()

const dept = ref<Department | null>(null)
const loading = ref(false)
const editing = ref(false)
const saving = ref(false)
const confirmDelete = ref(false)
const formData = ref<Record<string, any>>({})

onMounted(async () => {
    loading.value = true
    try {
        const [deptData] = await Promise.all([
            DepartmentService.getDepartment(props.deptId),
            attrStore.deptAttributes.length === 0 ? attrStore.fetchAttributes() : Promise.resolve()
        ])
        dept.value = deptData ?? null
    } finally {
        loading.value = false
    }
})

const deptAttrs = computed(() =>
    attrStore.deptAttributes.filter(a => !a.parentName)
)

const rows = computed(() =>
    deptAttrs.value.map(meta => ({
        meta,
        value: dept.value?.[meta.name] ?? null
    }))
)

// Edit mode: only READ_WRITE attrs are editable inputs; IMMUTABLE shown as locked
const editableAttrs = computed(() =>
    deptAttrs.value.filter(a => a.mutability !== 'READ_ONLY')
)

function startEdit() {
    if (!dept.value) return
    formData.value = { ...dept.value }
    editing.value = true
}

function cancelEdit() {
    editing.value = false
    formData.value = {}
}

async function saveEdit() {
    if (!dept.value) return
    saving.value = true
    try {
        const updated = await deptStore.update(dept.value.id, formData.value)
        dept.value = updated
        editing.value = false
        toast.success('Department updated')
    } catch (e: any) {
        toast.error(e?.message || 'Failed to update')
    } finally {
        saving.value = false
    }
}

async function doDelete() {
    if (!dept.value) return
    saving.value = true
    try {
        await deptStore.remove(dept.value.id)
        toast.success(`Deleted: ${dept.value.displayName}`)
        millerStore.removePane(props.paneIndex ?? 1)
    } catch (e: any) {
        toast.error(e?.message || 'Failed to delete')
        confirmDelete.value = false
    } finally {
        saving.value = false
    }
}

function openMembers() {
    if (!dept.value) return
    millerStore.setPane((props.paneIndex ?? 0) + 1, {
        id: `dept-users-${dept.value.id}`,
        type: 'OrgUserManagement',
        title: `${dept.value.displayName} Members`,
        data: { initialDeptId: dept.value.id },
        width: 'w3'
    })
}

function inputType(meta: IamAttributeMeta): string {
    if (meta.type === 'BOOLEAN') return 'checkbox'
    if (meta.type === 'INTEGER' || meta.type === 'NUMBER') return 'number'
    return 'text'
}

function formatValue(value: any): string {
    if (value === null || value === undefined || value === '') return '—'
    if (typeof value === 'boolean') return value ? 'Active' : 'Inactive'
    return String(value)
}
</script>

<template>
    <div class="h-full flex flex-col bg-white overflow-hidden">
        <div v-if="loading" class="flex-1 flex items-center justify-center">
            <div class="size-5 border-2 border-blue-600/20 border-t-blue-600 rounded-full animate-spin"></div>
        </div>

        <template v-else-if="dept">
            <!-- Header -->
            <div class="p-4 border-b border-neutral-100 bg-neutral-50/30 shrink-0">
                <div class="flex items-start justify-between">
                    <div class="flex items-center gap-3">
                        <div class="size-9 bg-white border border-neutral-200 rounded-lg flex items-center justify-center text-blue-600 shadow-sm">
                            <Network class="size-4" />
                        </div>
                        <div>
                            <h2 class="text-sm font-black text-neutral-900 leading-tight uppercase tracking-tight">
                                {{ dept.displayName }}
                            </h2>
                            <div class="flex items-center gap-2 mt-0.5">
                                <span class="text-[9px] text-neutral-400 font-mono">{{ dept.id }}</span>
                                <span v-if="dept.externalId" class="text-[9px] text-blue-400 font-mono bg-blue-50 px-1 rounded">
                                    EXT: {{ dept.externalId }}
                                </span>
                            </div>
                        </div>
                    </div>
                    <div class="flex items-center gap-1.5">
                        <span
                            class="text-[9px] font-bold px-2 py-0.5 rounded border"
                            :class="dept.active ? 'bg-green-50 text-green-600 border-green-200' : 'bg-neutral-100 text-neutral-400 border-neutral-200'"
                        >
                            {{ dept.active ? 'ACTIVE' : 'INACTIVE' }}
                        </span>
                        <Button v-if="!editing && !confirmDelete" size="xs" variant="outline" class="h-6 text-[10px]" @click="startEdit">
                            <Edit2 class="size-3 mr-1" /> Edit
                        </Button>
                    </div>
                </div>
            </div>

            <!-- Toolbar -->
            <div class="h-9 border-b border-neutral-100 flex items-center px-3 shrink-0 bg-neutral-50/30" data-testid="dept-attr-toolbar">
                <span class="text-[10px] font-bold text-neutral-500 uppercase tracking-wider flex-1">
                    Attributes
                    <span class="ml-1 text-neutral-400 font-normal" data-testid="dept-attr-count">({{ deptAttrs.length }})</span>
                </span>
                <template v-if="editing">
                    <Button size="xs" variant="ghost" class="h-6 text-[10px]" @click="cancelEdit" :disabled="saving">
                        <X class="size-3 mr-1" /> Cancel
                    </Button>
                    <Button size="xs" class="h-6 text-[10px] bg-blue-600 text-white hover:bg-blue-700 ml-1" @click="saveEdit" :disabled="saving">
                        <Save class="size-3 mr-1" /> {{ saving ? 'Saving…' : 'Save' }}
                    </Button>
                </template>
            </div>

            <!-- View mode -->
            <div v-if="!editing" class="flex-1 overflow-y-auto custom-scrollbar">
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
                                :class="row.value !== null && row.value !== '' ? 'text-neutral-800' : 'text-neutral-300 italic'">
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
                    <!-- Boolean toggle -->
                    <label v-if="attr.type === 'BOOLEAN'" class="flex items-center gap-2 cursor-pointer">
                        <input type="checkbox" v-model="formData[attr.name]"
                            :disabled="attr.mutability === 'IMMUTABLE'"
                            class="rounded border-neutral-300 text-blue-600" />
                        <span class="text-[11px] text-neutral-600">{{ formData[attr.name] ? 'Active' : 'Inactive' }}</span>
                    </label>
                    <!-- Text / Number -->
                    <input v-else
                        :type="inputType(attr)"
                        v-model="formData[attr.name]"
                        :disabled="attr.mutability === 'IMMUTABLE'"
                        :placeholder="attr.description ?? attr.displayName"
                        class="w-full h-7 px-2 text-[11px] bg-white border border-neutral-200 rounded focus:border-blue-400 focus:ring-1 focus:ring-blue-100 outline-none transition-all disabled:bg-neutral-50 disabled:text-neutral-400 disabled:cursor-not-allowed"
                    />
                </div>
            </div>

            <!-- Delete confirmation overlay -->
            <div v-if="confirmDelete" class="absolute inset-0 bg-white/95 flex flex-col items-center justify-center gap-3 z-10 p-6 text-center">
                <Trash2 class="size-8 text-red-400" />
                <div class="text-sm font-bold text-neutral-800">Delete "{{ dept.displayName }}"?</div>
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

            <!-- Actions -->
            <div v-if="!editing" class="shrink-0 p-3 border-t border-neutral-100 bg-neutral-50/30 flex gap-2">
                <button @click="openMembers"
                    class="flex-1 py-1.5 bg-neutral-900 text-white rounded text-[10px] font-bold hover:bg-neutral-800 transition-colors flex items-center justify-center gap-2">
                    <UsersIcon class="size-3" /> VIEW MEMBERS
                    <ChevronRight class="size-3 ml-auto" />
                </button>
                <button @click="confirmDelete = true"
                    class="py-1.5 px-3 bg-white border border-red-200 text-red-500 rounded text-[10px] font-bold hover:bg-red-50 transition-colors">
                    <Trash2 class="size-3" />
                </button>
            </div>
        </template>

        <div v-else class="flex-1 flex items-center justify-center text-[11px] text-neutral-300 italic">
            Department not found
        </div>
    </div>
</template>

<style scoped>
.custom-scrollbar::-webkit-scrollbar { width: 4px; }
.custom-scrollbar::-webkit-scrollbar-track { background: transparent; }
.custom-scrollbar::-webkit-scrollbar-thumb { background: #f1f1f1; border-radius: 10px; }
.custom-scrollbar::-webkit-scrollbar-thumb:hover { background: #e5e5e5; }
</style>
