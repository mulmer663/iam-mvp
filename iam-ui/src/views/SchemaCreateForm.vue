<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Button } from '@/components/ui/button'
import { Lock } from 'lucide-vue-next'
import { useResourceTypeStore } from '@/stores/resourceType'
import { useMillerStore } from '@/stores/miller'
import { toast } from '@/utils/toast'
import { isStandardSchema } from '@/utils/scim-permissions'

const props = defineProps<{ paneIndex?: number }>()

const rtStore = useResourceTypeStore()
const millerStore = useMillerStore()

// ── Step state ───────────────────────────────────────────────────────────────
const targetRtId = ref<string>('')
const mode = ref<'core' | 'extension' | ''>('')
const id = ref('urn:')
const name = ref('')
const description = ref('')
const saving = ref(false)
const error = ref('')

onMounted(async () => {
    if (rtStore.resourceTypes.length === 0) await rtStore.fetchResourceTypes()
    if (rtStore.schemas.length === 0) await rtStore.fetchSchemas()
})

const selectedRt = computed(() => rtStore.resourceTypes.find(r => r.id === targetRtId.value))

/** RFC 표준 RT의 CORE 스키마는 RFC가 잠금 → "Add to CORE" 비허용 */
const canAddToCore = computed(() => {
    if (!selectedRt.value) return false
    return !isStandardSchema(selectedRt.value.schema)
})

// ── Submit ───────────────────────────────────────────────────────────────────

async function onSubmit() {
    error.value = ''
    if (!targetRtId.value) { error.value = 'Select a Resource Type first.'; return }
    if (!mode.value) { error.value = 'Choose extend mode (CORE or Extension).'; return }

    if (mode.value === 'core') {
        // Just open the RT's core schema detail pane; user adds attrs there.
        const coreSchemaId = selectedRt.value!.schema
        millerStore.setPane(props.paneIndex ?? 0, {
            id: `schema-${coreSchemaId}`,
            type: 'SchemaDetailPane',
            title: selectedRt.value!.name,
            width: 'w1',
            data: { schemaId: coreSchemaId, paneIndex: props.paneIndex ?? 0 }
        })
        toast.success(`Editing core schema of ${selectedRt.value!.name}`)
        return
    }

    // mode === 'extension'
    if (!id.value || id.value === 'urn:' || !name.value) {
        error.value = 'Schema URN and name are required.'
        return
    }
    if (!id.value.startsWith('urn:')) {
        error.value = 'Schema id must be a valid URN starting with "urn:".'
        return
    }

    saving.value = true
    try {
        // 1. Create the new extension schema
        await rtStore.createSchema({ id: id.value, name: name.value, description: description.value })

        // 2. Auto-attach to selected RT's schemaExtensions
        const rt = selectedRt.value!
        const existing = rt.schemaExtensions ?? []
        if (!existing.some(e => e.schema === id.value)) {
            await rtStore.updateResourceType({
                ...rt,
                schemaExtensions: [...existing, { schema: id.value, required: false }]
            })
        }

        toast.success(`Extension "${name.value}" created and attached to ${rt.name}`)

        // 3. Open the new schema's detail pane
        millerStore.setPane(props.paneIndex ?? 0, {
            id: `schema-${id.value}`,
            type: 'SchemaDetailPane',
            title: name.value,
            width: 'w1',
            data: { schemaId: id.value, paneIndex: props.paneIndex ?? 0 }
        })
    } catch (e: any) {
        error.value = e?.response?.data?.message ?? 'Failed to create schema'
    } finally {
        saving.value = false
    }
}
</script>

<template>
    <div class="h-full flex flex-col bg-white">
        <div class="h-9 border-b border-neutral-100 flex items-center px-3 bg-neutral-50/50 shrink-0">
            <span class="text-[11px] font-bold text-neutral-600 uppercase tracking-wider">New Schema</span>
        </div>

        <form @submit.prevent="onSubmit" class="flex-1 overflow-y-auto p-4 space-y-5 custom-scrollbar">

            <!-- Step 1: Target ResourceType -->
            <div class="space-y-1.5">
                <label class="text-[11px] font-bold text-neutral-600 uppercase tracking-wider">
                    1. Target Resource Type *
                </label>
                <p class="text-[10px] text-neutral-400">Which resource type will this schema describe?</p>
                <select v-model="targetRtId" @change="mode = ''"
                    class="w-full h-8 px-2 text-xs bg-white border border-neutral-200 rounded focus:border-blue-400 outline-none transition-all">
                    <option value="">— Select Resource Type —</option>
                    <option v-for="rt in rtStore.resourceTypes" :key="rt.id" :value="rt.id">
                        {{ rt.name }} ({{ rt.endpoint }})
                    </option>
                </select>
            </div>

            <!-- Step 2: Mode selector -->
            <div v-if="targetRtId" class="space-y-1.5">
                <label class="text-[11px] font-bold text-neutral-600 uppercase tracking-wider">
                    2. Extend Mode *
                </label>
                <div class="grid grid-cols-2 gap-2">
                    <!-- Add to CORE -->
                    <label
                        :class="[
                            'flex flex-col gap-1 p-3 border rounded cursor-pointer transition-all',
                            mode === 'core' ? 'border-blue-400 bg-blue-50/50 ring-1 ring-blue-100' : 'border-neutral-200 hover:bg-neutral-50',
                            !canAddToCore ? 'opacity-50 cursor-not-allowed' : ''
                        ]">
                        <input type="radio" value="core" v-model="mode" :disabled="!canAddToCore" class="hidden" />
                        <div class="flex items-center justify-between">
                            <span class="text-[11px] font-bold text-neutral-700">Add to CORE</span>
                            <Lock v-if="!canAddToCore" class="size-3 text-neutral-300" />
                        </div>
                        <span class="text-[10px] text-neutral-400 leading-relaxed">
                            Add attributes directly to the resource type's base schema.
                        </span>
                        <span v-if="!canAddToCore" class="text-[9px] text-amber-500 font-medium">
                            RFC standard — locked
                        </span>
                    </label>

                    <!-- Create Extension -->
                    <label
                        :class="[
                            'flex flex-col gap-1 p-3 border rounded cursor-pointer transition-all',
                            mode === 'extension' ? 'border-blue-400 bg-blue-50/50 ring-1 ring-blue-100' : 'border-neutral-200 hover:bg-neutral-50'
                        ]">
                        <input type="radio" value="extension" v-model="mode" class="hidden" />
                        <span class="text-[11px] font-bold text-neutral-700">Create Extension</span>
                        <span class="text-[10px] text-neutral-400 leading-relaxed">
                            Define a new extension schema and attach it to this resource type.
                        </span>
                    </label>
                </div>
            </div>

            <!-- Step 3: Extension URN/name/desc (only for extension mode) -->
            <template v-if="mode === 'extension'">
                <div class="space-y-1.5">
                    <label class="text-[11px] font-bold text-neutral-600 uppercase tracking-wider">3. Extension URN *</label>
                    <input v-model="id"
                        placeholder="urn:com:example:scim:schemas:extension:1.0:User"
                        class="w-full text-xs font-mono border border-neutral-200 rounded px-2 py-1.5 outline-none focus:border-blue-400" />
                    <p class="text-[10px] text-neutral-400">
                        Must be a unique URN. Example: <code class="font-mono">urn:acme:scim:schemas:extension:1.0:{{ targetRtId }}</code>
                    </p>
                </div>

                <div class="space-y-1.5">
                    <label class="text-[11px] font-bold text-neutral-600 uppercase tracking-wider">Name *</label>
                    <input v-model="name"
                        placeholder="AcmeUser"
                        class="w-full text-sm border border-neutral-200 rounded px-2 py-1.5 outline-none focus:border-blue-400" />
                </div>

                <div class="space-y-1.5">
                    <label class="text-[11px] font-bold text-neutral-600 uppercase tracking-wider">Description</label>
                    <textarea v-model="description" rows="3"
                        placeholder="Acme Corp extension schema"
                        class="w-full text-xs border border-neutral-200 rounded px-2 py-1.5 outline-none focus:border-blue-400 resize-none" />
                </div>
            </template>

            <div v-if="error" class="text-xs text-red-500 bg-red-50 border border-red-200 rounded px-2 py-1.5">
                {{ error }}
            </div>

            <div class="flex gap-2 pt-2">
                <Button type="submit" class="h-8 text-xs px-4 bg-blue-600 text-white hover:bg-blue-700"
                    :disabled="saving || !targetRtId || !mode">
                    {{ saving ? 'Creating…' : (mode === 'core' ? 'Open CORE' : 'Create Extension') }}
                </Button>
                <Button type="button" variant="outline" class="h-8 text-xs px-3"
                    @click="millerStore.removePane(paneIndex ?? 0)">
                    Cancel
                </Button>
            </div>
        </form>
    </div>
</template>

<style scoped>
.custom-scrollbar::-webkit-scrollbar { width: 4px; }
.custom-scrollbar::-webkit-scrollbar-track { background: transparent; }
.custom-scrollbar::-webkit-scrollbar-thumb { background: #f1f1f1; border-radius: 10px; }
.custom-scrollbar::-webkit-scrollbar-thumb:hover { background: #e5e5e5; }
</style>
