<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Database, Layers, Lock, Plus, Trash2, ChevronRight } from 'lucide-vue-next'
import { useMillerStore } from '@/stores/miller'
import { useAttributeStore } from '@/stores/attribute'
import { useResourceTypeStore } from '@/stores/resourceType'
import { toast } from '@/utils/toast'
import { getSchemaCategory, isStandardSchema, shortenUrn } from '@/types/scim'

const props = defineProps<{ paneIndex?: number }>()

const millerStore = useMillerStore()
const attrStore = useAttributeStore()
const rtStore = useResourceTypeStore()

onMounted(async () => {
    await Promise.all([
        rtStore.fetchSchemas(),
        rtStore.fetchResourceTypes(),
        attrStore.fetchAttributes(),
        attrStore.fetchGroupAttributes(),
    ])
})

// ── Enriched schema list ──────────────────────────────────────────────────────
const allAttrs = computed(() => [...attrStore.userAttributes, ...attrStore.groupAttributes])

function attrCount(schemaId: string): number {
    const cat = getSchemaCategory(schemaId)
    if (cat === 'extension') {
        return allAttrs.value.filter(a => a.scimSchemaUri === schemaId).length
    }
    // Core: count CORE-category attrs for the right domain
    const rt = rtStore.resourceTypes.find(r => r.schema === schemaId)
    const domain = rt?.id === 'Group' ? 'GROUP' : 'USER'
    return allAttrs.value.filter(a => a.category === 'CORE' && a.targetDomain === domain).length
}

const coreSchemas = computed(() =>
    rtStore.schemas.filter(s => getSchemaCategory(s.id) === 'core')
)

const extensionSchemas = computed(() =>
    rtStore.schemas.filter(s => getSchemaCategory(s.id) === 'extension')
)

// ── Navigation ────────────────────────────────────────────────────────────────
function openDetail(schemaId: string, schemaName: string) {
    const paneId = `schema-${schemaId}`
    if (millerStore.panes.find(p => p.id === paneId)) {
        millerStore.activePaneId = paneId
        return
    }
    millerStore.setPane((props.paneIndex ?? 0) + 1, {
        id: paneId,
        type: 'SchemaDetailPane',
        title: schemaName,
        width: '560px',
        data: { schemaId, paneIndex: (props.paneIndex ?? 0) + 1 }
    })
}

function openCreate() {
    const paneId = `schema-create-${Date.now()}`
    millerStore.setPane((props.paneIndex ?? 0) + 1, {
        id: paneId,
        type: 'SchemaCreatePane',
        title: 'New Schema',
        width: '440px',
        data: { paneIndex: (props.paneIndex ?? 0) + 1 }
    })
}

async function onDelete(schemaId: string, schemaName: string) {
    if (isStandardSchema(schemaId)) return
    if (!confirm(`Delete schema "${schemaName}"?\nAll extension attributes under this schema will also be removed.`)) return
    try {
        await rtStore.deleteSchema(schemaId)
        toast.success('Schema deleted')
        // Close detail pane if open
        const idx = millerStore.panes.findIndex(p => p.id === `schema-${schemaId}`)
        if (idx !== -1) millerStore.removePane(idx)
    } catch {
        toast.error('Failed to delete schema')
    }
}
</script>

<template>
    <div class="h-full flex flex-col bg-white text-[13px]">

        <!-- Toolbar ─────────────────────────────────────────────────────────── -->
        <div class="h-10 border-b border-neutral-100 flex items-center px-3 justify-between shrink-0 bg-neutral-50/50">
            <span class="font-bold text-sm text-neutral-700">Schema Registry</span>
            <Button size="xs" variant="outline" class="h-7 text-xs flex gap-1" @click="openCreate">
                <Plus class="size-3" /> New Schema
            </Button>
        </div>

        <div class="flex-1 overflow-y-auto p-3 space-y-5">

            <!-- Loading -->
            <div v-if="rtStore.loading" class="flex items-center justify-center h-20 text-neutral-300 text-xs">
                Loading schemas…
            </div>

            <template v-else>

                <!-- Core Schemas ───────────────────────────────────────────── -->
                <section v-if="coreSchemas.length">
                    <div class="px-1 mb-2 text-[10px] font-bold text-neutral-400 uppercase tracking-widest flex items-center gap-1.5">
                        <Database class="size-3" /> Core Schemas
                    </div>
                    <div class="space-y-1.5">
                        <div v-for="s in coreSchemas" :key="s.id"
                            class="flex items-center gap-3 px-3 py-2.5 border border-neutral-100 rounded-lg hover:bg-blue-50/40 hover:border-blue-200 cursor-pointer group transition-all"
                            @click="openDetail(s.id, s.name)">

                            <div class="size-8 rounded-md bg-blue-50 border border-blue-100 flex items-center justify-center shrink-0">
                                <Database class="size-4 text-blue-500" />
                            </div>

                            <div class="flex-1 min-w-0">
                                <div class="flex items-center gap-1.5 flex-wrap">
                                    <span class="font-semibold text-neutral-800">{{ s.name }}</span>
                                    <Badge variant="outline"
                                        class="text-[8px] h-4 px-1.5 font-bold uppercase bg-blue-50 text-blue-600 border-blue-200">
                                        Core
                                    </Badge>
                                    <Lock class="size-3 text-neutral-300" title="RFC standard" />
                                </div>
                                <div class="text-[10px] text-neutral-400 font-mono truncate mt-0.5" :title="s.id">
                                    {{ shortenUrn(s.id) }}
                                </div>
                                <div v-if="s.description" class="text-[10px] text-neutral-500 mt-0.5 truncate">
                                    {{ s.description }}
                                </div>
                            </div>

                            <div class="flex items-center gap-2 shrink-0">
                                <span class="text-[10px] bg-neutral-100 px-1.5 py-0.5 rounded text-neutral-500 font-medium">
                                    {{ attrCount(s.id) }} attrs
                                </span>
                                <ChevronRight class="size-4 text-neutral-300 opacity-0 group-hover:opacity-100 transition-opacity" />
                            </div>
                        </div>
                    </div>
                </section>

                <!-- Extension Schemas ──────────────────────────────────────── -->
                <section v-if="extensionSchemas.length || true">
                    <div class="px-1 mb-2 text-[10px] font-bold text-neutral-400 uppercase tracking-widest flex items-center gap-1.5">
                        <Layers class="size-3" /> Extension Schemas
                    </div>

                    <div v-if="extensionSchemas.length === 0"
                        class="px-3 py-4 text-[11px] text-neutral-300 italic text-center border border-dashed border-neutral-200 rounded-lg">
                        No custom extensions yet. Click "New Schema" to add one.
                    </div>

                    <div v-else class="space-y-1.5">
                        <div v-for="s in extensionSchemas" :key="s.id"
                            class="flex items-center gap-3 px-3 py-2.5 border border-neutral-100 rounded-lg hover:bg-purple-50/40 hover:border-purple-200 cursor-pointer group transition-all"
                            @click="openDetail(s.id, s.name)">

                            <div class="size-8 rounded-md bg-purple-50 border border-purple-100 flex items-center justify-center shrink-0">
                                <Layers class="size-4 text-purple-500" />
                            </div>

                            <div class="flex-1 min-w-0">
                                <div class="flex items-center gap-1.5 flex-wrap">
                                    <span class="font-semibold text-neutral-800">{{ s.name }}</span>
                                    <Badge variant="outline"
                                        class="text-[8px] h-4 px-1.5 font-bold uppercase bg-purple-50 text-purple-600 border-purple-200">
                                        Extension
                                    </Badge>
                                    <Lock v-if="isStandardSchema(s.id)" class="size-3 text-neutral-300" title="RFC standard" />
                                </div>
                                <div class="text-[10px] text-neutral-400 font-mono truncate mt-0.5" :title="s.id">
                                    {{ shortenUrn(s.id) }}
                                </div>
                                <div v-if="s.description" class="text-[10px] text-neutral-500 mt-0.5 truncate">
                                    {{ s.description }}
                                </div>
                            </div>

                            <div class="flex items-center gap-2 shrink-0">
                                <span class="text-[10px] bg-neutral-100 px-1.5 py-0.5 rounded text-neutral-500 font-medium">
                                    {{ attrCount(s.id) }} attrs
                                </span>
                                <Button v-if="!isStandardSchema(s.id)"
                                    variant="ghost" size="icon"
                                    class="size-6 hover:bg-red-50 hover:text-red-500 opacity-0 group-hover:opacity-100 transition-opacity"
                                    @click.stop="onDelete(s.id, s.name)">
                                    <Trash2 class="size-3" />
                                </Button>
                                <ChevronRight class="size-4 text-neutral-300 opacity-0 group-hover:opacity-100 transition-opacity" />
                            </div>
                        </div>
                    </div>
                </section>

            </template>
        </div>
    </div>
</template>
