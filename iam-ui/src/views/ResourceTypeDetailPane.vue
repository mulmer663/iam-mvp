<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Edit2, Check, X, Plus, Trash2, Lock, ChevronRight, Link } from 'lucide-vue-next'
import { useResourceTypeStore } from '@/stores/resourceType'
import { useMillerStore } from '@/stores/miller'
import { toast } from '@/utils/toast'
import { shortenUrn } from '@/types/scim'
import { isStandardResourceType, resourceTypeCapabilities } from '@/utils/scim-permissions'

const props = defineProps<{
    resourceTypeId: string
    paneIndex?: number
}>()

const rtStore = useResourceTypeStore()
const millerStore = useMillerStore()

const rt = computed(() => rtStore.resourceTypes.find(r => r.id === props.resourceTypeId))
const isStandardRt = computed(() => isStandardResourceType(props.resourceTypeId))
const caps = computed(() => resourceTypeCapabilities(props.resourceTypeId))

// ── Inline edit ──────────────────────────────────────────────────────────────
const editing = ref(false)
const editName = ref('')
const editDesc = ref('')
const editEndpoint = ref('')
const editExtensions = ref<Array<{ schema: string; required: boolean }>>([])
const saving = ref(false)

function startEdit() {
    editName.value = rt.value?.name ?? ''
    editDesc.value = rt.value?.description ?? ''
    editEndpoint.value = rt.value?.endpoint ?? ''
    editExtensions.value = (rt.value?.schemaExtensions ?? []).map(e => ({ schema: e.schema, required: e.required }))
    editing.value = true
}

function cancelEdit() {
    editing.value = false
}

async function saveEdit() {
    if (!rt.value) return
    saving.value = true
    try {
        await rtStore.updateResourceType({
            ...rt.value,
            name: editName.value,
            description: editDesc.value,
            endpoint: editEndpoint.value,
            schemaExtensions: editExtensions.value
        })
        toast.success('Resource Type updated')
        editing.value = false
    } catch {
        toast.error('Failed to update Resource Type')
    } finally {
        saving.value = false
    }
}

function addExtension() {
    editExtensions.value.push({ schema: '', required: false })
}

function removeExtension(idx: number) {
    editExtensions.value.splice(idx, 1)
}

// ── Open schema detail ───────────────────────────────────────────────────────
function openSchema(schemaId: string) {
    const paneId = `schema-${schemaId}`
    millerStore.setPane((props.paneIndex ?? 0) + 1, {
        id: paneId,
        type: 'SchemaDetailPane',
        title: schemaId.split(':').slice(-1)[0] ?? schemaId,
        width: 'w1',
        data: { schemaId, paneIndex: (props.paneIndex ?? 0) + 1 }
    })
}

// Reset editing when RT id changes
watch(() => props.resourceTypeId, () => { editing.value = false })

onMounted(async () => {
    if (rtStore.resourceTypes.length === 0) await rtStore.fetchResourceTypes()
    if (rtStore.schemas.length === 0) await rtStore.fetchSchemas()
})
</script>

<template>
    <div class="h-full flex flex-col bg-white text-[13px]">

        <!-- Header ──────────────────────────────────────────────────────────── -->
        <div class="border-b border-neutral-100 bg-neutral-50/60 px-4 py-3 shrink-0">
            <div v-if="!editing" class="flex items-start justify-between gap-2">
                <div class="min-w-0">
                    <div class="flex items-center gap-2 flex-wrap">
                        <span class="font-bold text-neutral-800">{{ rt?.name ?? resourceTypeId }}</span>
                        <Badge variant="outline"
                            class="text-[9px] h-4 px-1.5 font-bold uppercase bg-blue-50 text-blue-600 border-blue-200">
                            ResourceType
                        </Badge>
                        <Lock v-if="isStandardRt" class="size-3 text-neutral-300" title="RFC standard — read-only" />
                    </div>
                    <div class="flex items-center gap-1 mt-0.5 text-[10px] text-neutral-400">
                        <span class="font-mono">{{ rt?.endpoint }}</span>
                    </div>
                    <div v-if="rt?.description" class="text-[11px] text-neutral-500 mt-1">{{ rt.description }}</div>
                </div>
                <Button variant="ghost" size="icon" class="size-7 shrink-0 hover:bg-neutral-100" @click="startEdit"
                    :title="isStandardRt ? 'Edit extensions only (RFC fields are locked)' : 'Edit'">
                    <Edit2 class="size-3.5 text-neutral-400" />
                </Button>
            </div>

            <!-- Inline edit mode -->
            <div v-else class="space-y-2">
                <div v-if="isStandardRt" class="flex items-center gap-1.5 text-[10px] text-amber-600 bg-amber-50 border border-amber-100 rounded px-2 py-1">
                    <Lock class="size-2.5" />
                    <span>RFC standard — only schema extensions are editable</span>
                </div>
                <input v-model="editName" placeholder="Name" :disabled="!caps.canEditMeta"
                    class="w-full text-sm font-bold border border-neutral-200 rounded px-2 py-1 outline-none focus:border-blue-400 disabled:bg-neutral-50 disabled:text-neutral-400 disabled:cursor-not-allowed" />
                <input v-model="editDesc" placeholder="Description" :disabled="!caps.canEditMeta"
                    class="w-full text-xs border border-neutral-200 rounded px-2 py-1 outline-none focus:border-blue-400 disabled:bg-neutral-50 disabled:text-neutral-400 disabled:cursor-not-allowed" />
                <input v-model="editEndpoint" placeholder="Endpoint (e.g. /Users)" :disabled="!caps.canEditMeta"
                    class="w-full text-xs font-mono border border-neutral-200 rounded px-2 py-1 outline-none focus:border-blue-400 disabled:bg-neutral-50 disabled:text-neutral-400 disabled:cursor-not-allowed" />
                <div class="flex gap-1.5 pt-1">
                    <Button size="xs" class="h-6 text-xs px-2" @click="saveEdit" :disabled="saving">
                        <Check class="size-3 mr-1" /> Save
                    </Button>
                    <Button size="xs" variant="ghost" class="h-6 text-xs px-2" @click="cancelEdit">
                        <X class="size-3 mr-1" /> Cancel
                    </Button>
                </div>
            </div>
        </div>

        <!-- Base Schema row ─────────────────────────────────────────────────── -->
        <div class="px-4 py-2.5 border-b border-neutral-100 shrink-0">
            <div class="text-[10px] font-bold text-neutral-400 uppercase tracking-wider mb-1.5">Base Schema</div>
            <div v-if="rt?.schema"
                class="flex items-center gap-2 px-2 py-1.5 bg-blue-50/50 border border-blue-100 rounded cursor-pointer hover:bg-blue-50 transition-colors group"
                @click="openSchema(rt.schema)">
                <Link class="size-3 text-blue-400 shrink-0" />
                <span class="text-[11px] font-mono text-blue-700 truncate flex-1">{{ shortenUrn(rt.schema) }}</span>
                <ChevronRight class="size-3 text-blue-400 opacity-0 group-hover:opacity-100 transition-opacity shrink-0" />
            </div>
            <div v-else class="text-[11px] text-neutral-300 italic">No base schema</div>
        </div>

        <!-- Schema Extensions ───────────────────────────────────────────────── -->
        <div class="flex-1 overflow-y-auto">
            <div class="h-9 border-b border-neutral-100 flex items-center px-3 justify-between shrink-0 bg-neutral-50/30">
                <span class="text-[11px] font-bold text-neutral-500 uppercase tracking-wider">
                    Schema Extensions
                    <span class="ml-1 text-neutral-400 font-normal">
                        ({{ editing ? editExtensions.length : (rt?.schemaExtensions?.length ?? 0) }})
                    </span>
                </span>
                <Button v-if="editing" size="xs" variant="outline"
                    class="h-6 text-xs flex gap-1" @click="addExtension">
                    <Plus class="size-3" /> Add
                </Button>
            </div>

            <!-- Read view -->
            <template v-if="!editing">
                <div v-if="!rt?.schemaExtensions?.length"
                    class="flex items-center justify-center h-24 text-neutral-300 text-xs italic">
                    No schema extensions defined.
                </div>
                <div v-for="ext in rt?.schemaExtensions" :key="ext.schema"
                    class="flex items-center gap-2 px-3 py-2.5 border-b border-neutral-50 hover:bg-neutral-50/70 group transition-colors cursor-pointer"
                    @click="openSchema(ext.schema)">
                    <Link class="size-3 text-purple-400 shrink-0" />
                    <div class="flex-1 min-w-0">
                        <div class="text-[11px] font-mono text-purple-700 truncate">{{ shortenUrn(ext.schema) }}</div>
                        <div v-if="ext.name" class="text-[10px] text-neutral-400 truncate">{{ ext.name }}</div>
                    </div>
                    <div class="flex items-center gap-1.5 shrink-0">
                        <span v-if="ext.required"
                            class="text-[9px] font-bold px-1.5 py-0.5 rounded border uppercase tracking-tighter bg-red-50 text-red-500 border-red-200">
                            Required
                        </span>
                        <span v-else class="text-[9px] text-neutral-300 font-medium">Optional</span>
                        <ChevronRight class="size-3 text-neutral-300 opacity-0 group-hover:opacity-100 transition-opacity" />
                    </div>
                </div>
            </template>

            <!-- Edit view -->
            <template v-else>
                <div v-if="!editExtensions.length"
                    class="flex items-center justify-center h-16 text-neutral-300 text-xs italic">
                    No extensions. Click "Add" to attach a schema.
                </div>
                <div v-for="(ext, idx) in editExtensions" :key="idx"
                    class="flex items-start gap-2 px-3 py-2 border-b border-neutral-50 group">
                    <div class="flex-1 space-y-1.5">
                        <input v-model="ext.schema" placeholder="urn:ietf:params:scim:schemas:extension:..."
                            class="w-full text-[11px] font-mono border border-neutral-200 rounded px-2 py-1 outline-none focus:border-blue-400" />
                        <label class="flex items-center gap-1.5 cursor-pointer">
                            <input type="checkbox" v-model="ext.required" class="size-3" />
                            <span class="text-[11px] text-neutral-500">Required</span>
                        </label>
                    </div>
                    <Button variant="ghost" size="icon"
                        class="size-6 mt-1 hover:bg-red-50 hover:text-red-500 opacity-0 group-hover:opacity-100 transition-opacity shrink-0"
                        @click="removeExtension(idx)">
                        <Trash2 class="size-3" />
                    </Button>
                </div>
            </template>
        </div>
    </div>
</template>
