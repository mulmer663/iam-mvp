<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Database, Layers, Lock, Plus, Trash2, ChevronRight, Box } from 'lucide-vue-next'
import { useMillerStore } from '@/stores/miller'
import { useAttributeStore } from '@/stores/attribute'
import { useResourceTypeStore } from '@/stores/resourceType'
import { toast } from '@/utils/toast'
import { getSchemaCategory, shortenUrn } from '@/types/scim'
import { isStandardSchema, isStandardResourceType } from '@/utils/scim-permissions'
import type { ScimResourceTypeDto } from '@/types/scim'

const props = defineProps<{ paneIndex?: number }>()

const millerStore = useMillerStore()
const attrStore = useAttributeStore()
const rtStore = useResourceTypeStore()

onMounted(async () => {
    await Promise.all([
        rtStore.fetchSchemas(),
        rtStore.fetchResourceTypes(),
        attrStore.fetchAttributes(),
    ])
})

// ── Enriched schema list ──────────────────────────────────────────────────────
const allAttrs = computed(() => attrStore.attributes)

const RT_DOMAIN_MAP: Record<string, string> = {
    User: 'USER',
    Group: 'GROUP',
    Department: 'DEPARTMENT',
}

function attrCount(schemaId: string): number {
    const cat = getSchemaCategory(schemaId)
    if (cat === 'extension') {
        return allAttrs.value.filter(a => a.scimSchemaUri === schemaId).length
    }
    const rt = rtStore.resourceTypes.find(r => r.schema === schemaId)
    const domain = RT_DOMAIN_MAP[rt?.id ?? ''] ?? 'USER'
    return allAttrs.value.filter(a => a.category === 'CORE' && a.targetDomain === domain).length
}

const coreSchemas = computed(() =>
    rtStore.schemas.filter(s => getSchemaCategory(s.id) === 'core')
)

const extensionSchemas = computed(() =>
    rtStore.schemas.filter(s => getSchemaCategory(s.id) === 'extension')
)

// ── ResourceType helpers ──────────────────────────────────────────────────────
function isStandardRt(rt: ScimResourceTypeDto): boolean {
    return isStandardResourceType(rt.id)
}

// ── Navigation — Schemas ─────────────────────────────────────────────────────
function openSchemaDetail(schemaId: string, schemaName: string) {
    const paneId = `schema-${schemaId}`
    if (millerStore.panes.find(p => p.id === paneId)) {
        millerStore.activePaneId = paneId
        return
    }
    millerStore.setPane((props.paneIndex ?? 0) + 1, {
        id: paneId,
        type: 'SchemaDetailPane',
        title: schemaName,
        width: 'w1',
        data: { schemaId, paneIndex: (props.paneIndex ?? 0) + 1 }
    })
}

function openSchemaCreate() {
    const paneId = `schema-create-${Date.now()}`
    millerStore.setPane((props.paneIndex ?? 0) + 1, {
        id: paneId,
        type: 'SchemaCreatePane',
        title: 'New Schema',
        width: 'w1',
        data: { paneIndex: (props.paneIndex ?? 0) + 1 }
    })
}

async function deleteSchema(schemaId: string, schemaName: string) {
    if (isStandardSchema(schemaId)) return
    if (!confirm(`Delete schema "${schemaName}"?\nAll extension attributes under this schema will also be removed.`)) return
    try {
        await rtStore.deleteSchema(schemaId)
        toast.success('Schema deleted')
        const idx = millerStore.panes.findIndex(p => p.id === `schema-${schemaId}`)
        if (idx !== -1) millerStore.removePane(idx)
    } catch {
        toast.error('Failed to delete schema')
    }
}

// ── Navigation — ResourceTypes ───────────────────────────────────────────────
function openRtDetail(rt: ScimResourceTypeDto) {
    const paneId = `rt-${rt.id}`
    if (millerStore.panes.find(p => p.id === paneId)) {
        millerStore.activePaneId = paneId
        return
    }
    millerStore.setPane((props.paneIndex ?? 0) + 1, {
        id: paneId,
        type: 'ResourceTypeDetailPane',
        title: rt.name,
        width: 'w1',
        data: { resourceTypeId: rt.id, paneIndex: (props.paneIndex ?? 0) + 1 }
    })
}

function openRtCreate() {
    const paneId = `rt-create-${Date.now()}`
    millerStore.setPane((props.paneIndex ?? 0) + 1, {
        id: paneId,
        type: 'ResourceTypeCreatePane',
        title: 'New Resource Type',
        width: 'w1',
        data: { paneIndex: (props.paneIndex ?? 0) + 1 }
    })
}

async function deleteRt(rt: ScimResourceTypeDto) {
    if (isStandardRt(rt)) return
    if (!confirm(`Delete Resource Type "${rt.name}"?`)) return
    try {
        await rtStore.deleteResourceType(rt.id)
        toast.success('Resource Type deleted')
        const idx = millerStore.panes.findIndex(p => p.id === `rt-${rt.id}`)
        if (idx !== -1) millerStore.removePane(idx)
    } catch {
        toast.error('Failed to delete Resource Type')
    }
}
</script>

<template>
    <div class="h-full flex flex-col bg-white text-[13px]">

        <!-- Loading -->
        <div v-if="rtStore.loading" class="flex items-center justify-center h-20 text-neutral-300 text-xs">
            Loading…
        </div>

        <div v-else class="flex-1 overflow-y-auto p-3 space-y-5">

            <!-- ── Schemas ──────────────────────────────────────────────────── -->
            <section>
                <div class="px-1 mb-2 flex items-center justify-between">
                    <span class="text-[10px] font-bold text-neutral-400 uppercase tracking-widest flex items-center gap-1.5">
                        <Database class="size-3" /> Schemas
                    </span>
                    <Button size="xs" variant="outline" class="h-6 text-[11px] flex gap-1" @click="openSchemaCreate">
                        <Plus class="size-3" /> New
                    </Button>
                </div>

                <!-- Core Schemas -->
                <div v-if="coreSchemas.length" class="space-y-1.5 mb-2">
                    <div v-for="s in coreSchemas" :key="s.id"
                        class="flex items-center gap-3 px-3 py-2.5 border border-neutral-100 rounded-lg hover:bg-blue-50/40 hover:border-blue-200 cursor-pointer group transition-all"
                        @click="openSchemaDetail(s.id, s.name)">

                        <div class="size-7 rounded-md bg-blue-50 border border-blue-100 flex items-center justify-center shrink-0">
                            <Database class="size-3.5 text-blue-500" />
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
                        </div>

                        <div class="flex items-center gap-2 shrink-0">
                            <span class="text-[10px] bg-neutral-100 px-1.5 py-0.5 rounded text-neutral-500 font-medium">
                                {{ attrCount(s.id) }} attrs
                            </span>
                            <ChevronRight class="size-4 text-neutral-300 opacity-0 group-hover:opacity-100 transition-opacity" />
                        </div>
                    </div>
                </div>

                <!-- Extension Schemas -->
                <div v-if="extensionSchemas.length === 0"
                    class="px-3 py-3 text-[11px] text-neutral-300 italic text-center border border-dashed border-neutral-200 rounded-lg">
                    No custom extensions yet. Click "New" to add one.
                </div>

                <div v-else class="space-y-1.5">
                    <div v-for="s in extensionSchemas" :key="s.id"
                        class="flex items-center gap-3 px-3 py-2.5 border border-neutral-100 rounded-lg hover:bg-purple-50/40 hover:border-purple-200 cursor-pointer group transition-all"
                        @click="openSchemaDetail(s.id, s.name)">

                        <div class="size-7 rounded-md bg-purple-50 border border-purple-100 flex items-center justify-center shrink-0">
                            <Layers class="size-3.5 text-purple-500" />
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
                        </div>

                        <div class="flex items-center gap-2 shrink-0">
                            <span class="text-[10px] bg-neutral-100 px-1.5 py-0.5 rounded text-neutral-500 font-medium">
                                {{ attrCount(s.id) }} attrs
                            </span>
                            <Button v-if="!isStandardSchema(s.id)"
                                variant="ghost" size="icon"
                                class="size-6 hover:bg-red-50 hover:text-red-500 opacity-0 group-hover:opacity-100 transition-opacity"
                                @click.stop="deleteSchema(s.id, s.name)">
                                <Trash2 class="size-3" />
                            </Button>
                            <ChevronRight class="size-4 text-neutral-300 opacity-0 group-hover:opacity-100 transition-opacity" />
                        </div>
                    </div>
                </div>
            </section>

            <!-- ── Resource Types ───────────────────────────────────────────── -->
            <section>
                <div class="px-1 mb-2 flex items-center justify-between">
                    <span class="text-[10px] font-bold text-neutral-400 uppercase tracking-widest flex items-center gap-1.5">
                        <Box class="size-3" /> Resource Types
                    </span>
                    <Button size="xs" variant="outline" class="h-6 text-[11px] flex gap-1" @click="openRtCreate">
                        <Plus class="size-3" /> New
                    </Button>
                </div>

                <div v-if="rtStore.resourceTypes.length === 0"
                    class="px-3 py-3 text-[11px] text-neutral-300 italic text-center border border-dashed border-neutral-200 rounded-lg">
                    No resource types found.
                </div>

                <div v-else class="space-y-1.5">
                    <div v-for="rt in rtStore.resourceTypes" :key="rt.id"
                        class="flex items-center gap-3 px-3 py-2.5 border border-neutral-100 rounded-lg hover:bg-neutral-50 hover:border-neutral-200 cursor-pointer group transition-all"
                        @click="openRtDetail(rt)">

                        <div class="size-7 rounded-md bg-neutral-100 border border-neutral-200 flex items-center justify-center shrink-0">
                            <Box class="size-3.5 text-neutral-500" />
                        </div>

                        <div class="flex-1 min-w-0">
                            <div class="flex items-center gap-1.5 flex-wrap">
                                <span class="font-semibold text-neutral-800">{{ rt.name }}</span>
                                <Badge variant="outline"
                                    class="text-[8px] h-4 px-1.5 font-bold uppercase bg-neutral-100 text-neutral-500 border-neutral-200">
                                    {{ rt.endpoint }}
                                </Badge>
                                <Lock v-if="isStandardRt(rt)" class="size-3 text-neutral-300" title="RFC standard" />
                            </div>
                            <div class="text-[10px] text-neutral-400 font-mono truncate mt-0.5" :title="rt.schema">
                                {{ shortenUrn(rt.schema) }}
                            </div>
                        </div>

                        <div class="flex items-center gap-2 shrink-0">
                            <span v-if="rt.schemaExtensions?.length"
                                class="text-[10px] bg-purple-50 text-purple-600 border border-purple-100 px-1.5 py-0.5 rounded font-medium">
                                +{{ rt.schemaExtensions.length }} ext
                            </span>
                            <Button v-if="!isStandardRt(rt)"
                                variant="ghost" size="icon"
                                class="size-6 hover:bg-red-50 hover:text-red-500 opacity-0 group-hover:opacity-100 transition-opacity"
                                @click.stop="deleteRt(rt)">
                                <Trash2 class="size-3" />
                            </Button>
                            <ChevronRight class="size-4 text-neutral-300 opacity-0 group-hover:opacity-100 transition-opacity" />
                        </div>
                    </div>
                </div>
            </section>

        </div>
    </div>
</template>
