<script setup lang="ts">
import {computed, onMounted, ref} from 'vue'
import {useAttributeStore} from '@/stores/attribute'
import {Button} from '@/components/ui/button'
import {ExternalLink, Info, Lock, Plus, Trash2} from 'lucide-vue-next'
import {Badge} from '@/components/ui/badge'
import {useMillerStore} from '@/stores/miller'
import type {IamAttributeMeta} from '@/types/attribute'

import {getMetadataBySchema} from '@/utils/scim-metadata'

const props = defineProps<{
    paneIndex?: number
    schemaUri?: string // Schema Context
}>()

const store = useAttributeStore()
const millerStore = useMillerStore()

onMounted(() => {
    store.fetchAttributes()
})

const metadata = computed(() => {
    return props.schemaUri ? getMetadataBySchema(props.schemaUri) : null
})

const filteredAttributes = computed(() => {
    if (props.schemaUri) {
        return store.attributes.filter(a => a.scimSchemaUri === props.schemaUri)
    }
    return store.attributes
})

// Dialog / Edit Mode state controlled via child pane or inline for now?
// Let's use a "Dialog Pane" logic or simple edit mode if we want to stay in Miller context.
// Or we can just toggle a form here for simplicity.
const isCreating = ref(false)

function onCreate() {
    isCreating.value = true
    openDetailPane({ scimSchemaUri: props.schemaUri } as any) 
}

function openDetailPane(attr: IamAttributeMeta | null) {
    const paneId = attr ? `attr-${attr.code}` : `create-attr-${Date.now()}`
    
    // Check if pane exists
    const existingPane = millerStore.panes.find(p => p.id === paneId)
    if (existingPane) {
        millerStore.activePaneId = paneId
        return
    }

    const pane = {
        id: paneId,
        type: 'AttributeFormPane',
        title: attr && attr.code ? `Attribute: ${attr.displayName}` : 'New Attribute',
        data: { initialData: attr && attr.code ? attr : null, defaultSchemaUri: props.schemaUri },
        width: '500px'
    }
    
    // Add to stack
    if (typeof props.paneIndex === 'number') {
        millerStore.setPane(props.paneIndex + 1, pane)
    } else {
        millerStore.pushPane(pane)
    }
}

async function onDelete(code: String) {
    if (!confirm(`Are you sure you want to delete attribute ${code}?`)) return;
    try {
        await store.deleteAttribute(code as string)
    } catch (e) {
        alert('Failed to delete')
    }
}
</script>

<template>
    <div class="h-full flex flex-col bg-white">
        <!-- Toolbar -->
        <div class="h-10 border-b border-neutral-100 flex items-center px-3 justify-between shrink-0">
             <div class="font-bold text-sm text-neutral-700">
                Attributes
             </div>
            
            <Button size="xs" class="bg-blue-600 text-white hover:bg-blue-700 font-bold flex gap-1 h-7" @click="onCreate">
                <Plus class="size-3" /> Add
            </Button>
        </div>

        <!-- SCIM Resource Metadata -->
        <div v-if="metadata" class="p-4 bg-neutral-50/30 border-b border-neutral-100/50">
             <div class="flex items-start gap-3">
                <div class="size-8 rounded bg-white border border-neutral-200 flex items-center justify-center text-blue-500 shrink-0 shadow-sm">
                    <Info class="size-4" />
                </div>
                <div class="flex-1 min-w-0">
                    <div class="flex items-center gap-2">
                         <span class="text-xs font-black text-neutral-900 uppercase tracking-tight">{{ metadata.name }}</span>
                         <Badge variant="outline" class="text-[9px] h-4 bg-white font-mono">{{ metadata.id }}</Badge>
                    </div>
                    <p class="text-[10px] text-neutral-500 mt-1 leading-relaxed">
                        {{ metadata.description }}
                    </p>
                    
                    <div class="mt-3 grid grid-cols-2 gap-x-4 gap-y-2">
                        <div class="flex flex-col gap-0.5">
                            <span class="text-[9px] font-bold text-neutral-400 uppercase tracking-wider">Endpoint</span>
                            <div class="flex items-center gap-1.5">
                                <span class="text-[10px] font-mono text-blue-600 bg-blue-50/50 px-1 rounded">{{ metadata.endpoint }}</span>
                                <ExternalLink class="size-2.5 text-neutral-300" />
                            </div>
                        </div>
                        <div class="flex flex-col gap-0.5">
                            <span class="text-[9px] font-bold text-neutral-400 uppercase tracking-wider">Base Schema</span>
                            <div class="text-[9px] font-mono text-neutral-600 truncate" :title="metadata.schema">{{ metadata.schema }}</div>
                        </div>
                    </div>

                    <!-- Extensions if any -->
                    <div v-if="metadata.schemaExtensions && metadata.schemaExtensions.length > 0" class="mt-3">
                        <span class="text-[9px] font-bold text-neutral-400 uppercase tracking-wider">Schema Extensions</span>
                        <div class="mt-1 space-y-1">
                            <div v-for="ext in metadata.schemaExtensions" :key="ext.schema" class="flex items-center gap-2">
                                <div class="size-1 rounded-full bg-neutral-300"></div>
                                <span class="text-[9px] font-mono text-neutral-500 truncate" :title="ext.schema">{{ ext.schema.split(':').pop() }}</span>
                                <span v-if="ext.required" class="text-[8px] bg-red-50 text-red-500 border border-red-100 px-1 rounded">REQ</span>
                            </div>
                        </div>
                    </div>
                </div>
             </div>
        </div>

        <!-- List -->
        <div class="flex-1 overflow-y-auto p-2">
            <div v-for="attr in filteredAttributes" :key="attr.code" 
                 @click="openDetailPane(attr)"
                 class="flex items-center justify-between p-3 border-b border-neutral-50 last:border-0 hover:bg-neutral-50 group transition-all cursor-pointer"
            >
                <div>
                     <div class="flex items-center gap-2">
                            <span class="text-sm font-medium text-neutral-800">{{ attr.displayName }}</span>
                            <span class="text-[10px] text-neutral-400 font-mono">{{ attr.code }}</span>
                            <span v-if="attr.required" class="text-[8px] text-red-500 font-bold uppercase border border-red-200 px-0.5 rounded">*Req</span>
                     </div>
                     <div class="flex items-center gap-2 mt-0.5">
                           <Badge variant="secondary" class="text-[9px] px-1.5 py-0 h-4 bg-neutral-100 text-neutral-500 font-normal">
                                {{ attr.dataType }}
                           </Badge>
                     </div>
                </div>

                <div class="flex items-center gap-2">
                     <Lock v-if="attr.category === 'CORE'" class="size-3 text-neutral-300" />
                     <div v-if="attr.category === 'EXTENSION'" class="opacity-0 group-hover:opacity-100 transition-opacity">
                        <Button 
                            variant="ghost" size="icon" 
                            class="size-6 hover:bg-red-50 hover:text-red-600"
                            @click.stop="onDelete(attr.code)"
                        >
                            <Trash2 class="size-3" />
                        </Button>
                     </div>
                </div>
            </div>
        </div>
    </div>
</template>
