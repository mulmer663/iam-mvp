<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Edit2, Check, X, Plus, Trash2, Lock, ChevronRight } from 'lucide-vue-next'
import { useAttributeStore } from '@/stores/attribute'
import { useResourceTypeStore } from '@/stores/resourceType'
import { useMillerStore } from '@/stores/miller'
import { toast } from '@/utils/toast'
import { getSchemaCategory, isStandardSchema } from '@/types/scim'
import type { AttributeTargetDomain } from '@/types/attribute'

const props = defineProps<{
    schemaId: string
    paneIndex?: number
}>()

const attrStore = useAttributeStore()
const rtStore = useResourceTypeStore()
const millerStore = useMillerStore()

// ── Schema meta ─────────────────────────────────────────────────────────────
const schema = computed(() => rtStore.schemas.find(s => s.id === props.schemaId))
const category = computed(() => getSchemaCategory(props.schemaId))
const isStandard = computed(() => isStandardSchema(props.schemaId))
const isExtension = computed(() => category.value === 'extension')

// Infer target domain from ResourceType linkage
const targetDomain = computed<AttributeTargetDomain>(() => {
    const rt = rtStore.resourceTypes.find(
        r => r.schema === props.schemaId ||
             r.schemaExtensions?.some(e => e.schema === props.schemaId)
    )
    if (!rt) return 'USER'
    return rt.schema === props.schemaId || rt.id === 'User' ? 'USER' : 'GROUP'
})

// ── Attribute list for this schema ──────────────────────────────────────────
const schemaAttributes = computed(() => {
    const allAttrs = [...attrStore.userAttributes, ...attrStore.groupAttributes]
    if (isExtension.value) {
        return allAttrs.filter(a => a.scimSchemaUri === props.schemaId)
    }
    // Core schema: show CORE category attrs for the right domain
    const domain = targetDomain.value
    return allAttrs.filter(a => a.category === 'CORE' && a.targetDomain === domain)
})

// ── Inline schema name/description edit ─────────────────────────────────────
const editing = ref(false)
const editName = ref('')
const editDesc = ref('')
const saving = ref(false)

function startEdit() {
    editName.value = schema.value?.name ?? ''
    editDesc.value = schema.value?.description ?? ''
    editing.value = true
}

async function saveEdit() {
    if (!schema.value) return
    saving.value = true
    try {
        await rtStore.updateSchema(props.schemaId, { name: editName.value, description: editDesc.value })
        toast.success('Schema updated')
        editing.value = false
    } catch {
        toast.error('Failed to update schema')
    } finally {
        saving.value = false
    }
}

// ── Attribute actions ────────────────────────────────────────────────────────
function openAddAttribute() {
    const paneId = `attr-create-${Date.now()}`
    millerStore.setPane((props.paneIndex ?? 0) + 1, {
        id: paneId,
        type: 'AttributeFormPane',
        title: 'New Attribute',
        width: '420px',
        data: {
            initialData: {
                name: '',
                targetDomain: targetDomain.value,
                category: 'EXTENSION',
                scimSchemaUri: props.schemaId,
                type: 'STRING',
            },
            paneIndex: (props.paneIndex ?? 0) + 1
        }
    })
}

function openEditAttribute(attr: any) {
    const paneId = `attr-edit-${attr.name}-${Date.now()}`
    millerStore.setPane((props.paneIndex ?? 0) + 1, {
        id: paneId,
        type: 'AttributeFormPane',
        title: `Edit: ${attr.name}`,
        width: '420px',
        data: { initialData: attr, paneIndex: (props.paneIndex ?? 0) + 1 }
    })
}

async function deleteAttribute(name: string) {
    if (!confirm(`Delete attribute "${name}"?`)) return
    try {
        await attrStore.deleteAttribute(name, targetDomain.value)
        toast.success('Attribute deleted')
    } catch {
        toast.error('Failed to delete attribute')
    }
}

onMounted(async () => {
    if (rtStore.schemas.length === 0) await rtStore.fetchSchemas()
    if (rtStore.resourceTypes.length === 0) await rtStore.fetchResourceTypes()
    await attrStore.fetchAttributes()
    await attrStore.fetchGroupAttributes()
})

// ── Helpers ──────────────────────────────────────────────────────────────────
function mutabilityColor(m: string) {
    if (!m) return 'text-neutral-400'
    const v = m.toLowerCase()
    if (v === 'read_only' || v === 'readonly') return 'text-amber-600'
    if (v === 'immutable') return 'text-red-500'
    if (v === 'write_only' || v === 'writeonly') return 'text-purple-500'
    return 'text-neutral-500'
}

function typeColor(t: string) {
    const m: Record<string, string> = {
        STRING: 'bg-blue-50 text-blue-600 border-blue-200',
        BOOLEAN: 'bg-amber-50 text-amber-600 border-amber-200',
        INTEGER: 'bg-green-50 text-green-600 border-green-200',
        COMPLEX: 'bg-purple-50 text-purple-600 border-purple-200',
        REFERENCE: 'bg-rose-50 text-rose-600 border-rose-200',
        DATETIME: 'bg-teal-50 text-teal-600 border-teal-200',
    }
    return m[t?.toUpperCase()] ?? 'bg-neutral-50 text-neutral-500 border-neutral-200'
}
</script>

<template>
    <div class="h-full flex flex-col bg-white text-[13px]">

        <!-- Schema Header ──────────────────────────────────────────────────── -->
        <div class="border-b border-neutral-100 bg-neutral-50/60 px-4 py-3 shrink-0">
            <div v-if="!editing" class="flex items-start justify-between gap-2">
                <div class="min-w-0">
                    <div class="flex items-center gap-2 flex-wrap">
                        <span class="font-bold text-neutral-800">{{ schema?.name ?? schemaId }}</span>
                        <Badge v-if="isExtension" variant="outline"
                            class="text-[9px] h-4 px-1.5 font-bold uppercase bg-purple-50 text-purple-600 border-purple-200">
                            Extension
                        </Badge>
                        <Badge v-else variant="outline"
                            class="text-[9px] h-4 px-1.5 font-bold uppercase bg-blue-50 text-blue-600 border-blue-200">
                            Core
                        </Badge>
                        <Lock v-if="isStandard" class="size-3 text-neutral-300" title="RFC standard — read-only" />
                    </div>
                    <div class="text-[10px] text-neutral-400 font-mono mt-0.5 break-all">{{ schemaId }}</div>
                    <div v-if="schema?.description" class="text-[11px] text-neutral-500 mt-1">{{ schema.description }}</div>
                </div>
                <Button v-if="isExtension && !isStandard"
                    variant="ghost" size="icon" class="size-7 shrink-0 hover:bg-neutral-100" @click="startEdit">
                    <Edit2 class="size-3.5 text-neutral-400" />
                </Button>
            </div>

            <!-- Inline edit mode -->
            <div v-else class="space-y-2">
                <input v-model="editName" placeholder="Schema name"
                    class="w-full text-sm font-bold border border-neutral-200 rounded px-2 py-1 outline-none focus:border-blue-400" />
                <input v-model="editDesc" placeholder="Description"
                    class="w-full text-xs border border-neutral-200 rounded px-2 py-1 outline-none focus:border-blue-400" />
                <div class="flex gap-1.5">
                    <Button size="xs" class="h-6 text-xs px-2" @click="saveEdit" :disabled="saving">
                        <Check class="size-3 mr-1" /> Save
                    </Button>
                    <Button size="xs" variant="ghost" class="h-6 text-xs px-2" @click="editing = false">
                        <X class="size-3 mr-1" /> Cancel
                    </Button>
                </div>
            </div>
        </div>

        <!-- Toolbar ─────────────────────────────────────────────────────────── -->
        <div class="h-9 border-b border-neutral-100 flex items-center px-3 justify-between shrink-0 bg-neutral-50/30">
            <span class="text-[11px] font-bold text-neutral-500 uppercase tracking-wider">
                Attributes
                <span class="ml-1 text-neutral-400 font-normal">({{ schemaAttributes.length }})</span>
            </span>
            <Button v-if="isExtension" size="xs" variant="outline"
                class="h-6 text-xs flex gap-1" @click="openAddAttribute">
                <Plus class="size-3" /> Add Attribute
            </Button>
        </div>

        <!-- Attribute Table ─────────────────────────────────────────────────── -->
        <div class="flex-1 overflow-y-auto">
            <!-- Table header -->
            <div class="grid grid-cols-[1fr_80px_60px_90px_32px] gap-1 px-3 py-1.5 bg-neutral-50 border-b border-neutral-100 sticky top-0 z-10">
                <span class="text-[10px] font-bold text-neutral-400 uppercase tracking-wider">Name</span>
                <span class="text-[10px] font-bold text-neutral-400 uppercase tracking-wider">Type</span>
                <span class="text-[10px] font-bold text-neutral-400 uppercase tracking-wider">Req.</span>
                <span class="text-[10px] font-bold text-neutral-400 uppercase tracking-wider">Mutability</span>
                <span></span>
            </div>

            <!-- Empty state -->
            <div v-if="schemaAttributes.length === 0"
                class="flex flex-col items-center justify-center h-40 text-neutral-300 text-xs italic">
                No attributes defined for this schema.
            </div>

            <!-- Attribute rows -->
            <div v-for="attr in schemaAttributes" :key="`${attr.targetDomain}-${attr.name}`"
                class="grid grid-cols-[1fr_80px_60px_90px_32px] gap-1 px-3 py-2 border-b border-neutral-50 hover:bg-neutral-50/70 group items-center transition-colors"
                :class="{ 'cursor-pointer': isExtension }"
                @click="isExtension ? openEditAttribute(attr) : undefined">

                <!-- Name + description -->
                <div class="min-w-0">
                    <div class="flex items-center gap-1.5">
                        <span class="font-medium text-neutral-800 truncate" :title="attr.name">{{ attr.name }}</span>
                        <ChevronRight v-if="isExtension"
                            class="size-3 text-neutral-300 opacity-0 group-hover:opacity-100 transition-opacity shrink-0" />
                    </div>
                    <div v-if="attr.description" class="text-[10px] text-neutral-400 truncate">{{ attr.description }}</div>
                </div>

                <!-- Type badge -->
                <div>
                    <span :class="['text-[9px] font-bold px-1.5 py-0.5 rounded border uppercase tracking-tighter', typeColor(attr.type)]">
                        {{ attr.type }}
                    </span>
                </div>

                <!-- Required -->
                <div>
                    <span v-if="attr.required"
                        class="text-[9px] font-bold px-1.5 py-0.5 rounded border uppercase tracking-tighter bg-red-50 text-red-500 border-red-200">
                        Yes
                    </span>
                    <span v-else class="text-[10px] text-neutral-300">—</span>
                </div>

                <!-- Mutability -->
                <div :class="['text-[10px] font-medium truncate', mutabilityColor(attr.mutability)]">
                    {{ attr.mutability?.replace('_', ' ') ?? '—' }}
                </div>

                <!-- Actions (extension only) -->
                <div class="flex items-center justify-end">
                    <Button v-if="isExtension && attr.category !== 'CORE'"
                        variant="ghost" size="icon"
                        class="size-5 hover:bg-red-50 hover:text-red-500 opacity-0 group-hover:opacity-100 transition-opacity"
                        @click.stop="deleteAttribute(attr.name)">
                        <Trash2 class="size-3" />
                    </Button>
                </div>
            </div>
        </div>
    </div>
</template>
