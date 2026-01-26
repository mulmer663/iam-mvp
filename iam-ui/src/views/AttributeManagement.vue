<script setup lang="ts">
import {computed, onMounted, ref} from 'vue'
import {useAttributeStore} from '@/stores/attribute'
import {Button} from '@/components/ui/button'
import {ExternalLink, Info, Lock, Plus, Trash2} from 'lucide-vue-next'
import {Badge} from '@/components/ui/badge'
import {useMillerStore} from '@/stores/miller'
import type {IamAttributeMeta} from '@/types/attribute'

import {useResourceTypeStore} from '@/stores/resourceType'

const props = defineProps<{
    paneIndex?: number
    schemaUri?: string // Schema Context
}>()

const store = useAttributeStore()
const millerStore = useMillerStore()
const resourceTypeStore = useResourceTypeStore()

onMounted(() => {
    store.fetchAttributes()
    resourceTypeStore.fetchResourceTypes()
    resourceTypeStore.fetchSchemas()
})

const metadata = computed(() => {
    if (!props.schemaUri) return null
    
    // Try to find as a base resource type first
    const rt = resourceTypeStore.resourceTypes.find(rt => rt.schema === props.schemaUri)
    if (rt) {
        return {
            isAttribute: false,
            id: rt.name, // Resource Type Name as ID visual
            name: rt.name,
            description: rt.description,
            endpoint: rt.endpoint, 
            schema: rt.schema,
            schemaExtensions: rt.schemaExtensions,
            type: undefined,
            mutability: undefined,
            category: undefined
        }
    }

    // If not a base resource type, it's an extension. Find the schema's own metadata
    const schema = resourceTypeStore.schemas.find(s => s.id === props.schemaUri)
    if (schema) {
        return {
            isAttribute: false,
            id: schema.id.split(':').pop() || '',
            name: schema.name,
            description: schema.description,
            endpoint: '', // Extensions don't have their own endpoint
            schema: schema.id,
            schemaExtensions: [],
            type: undefined,
            mutability: undefined,
            category: undefined
        }
    }
    
    return null
})

const filteredAttributes = computed(() => {
    let attrs = store.attributes
    
    if (props.schemaUri) {
        attrs = attrs.filter(a => a.scimSchemaUri === props.schemaUri)
    }

    // Default: Show only Root attributes (no parent)
    return attrs.filter(a => !a.parentName)
})

const isCreating = ref(false)

function onCreate() {
    isCreating.value = true
    openDetailForm({ scimSchemaUri: props.schemaUri } as any) 
}

const isStandardSchema = (uri?: string) => {
    if (!uri) return false
    return uri.startsWith('urn:ietf:params:scim:schemas:core:2.0:') || 
           uri.startsWith('urn:ietf:params:scim:schemas:extension:enterprise:2.0:')
}

function shortenUri(uri: string): string {
    if (!uri) return ''
    const prefix = "urn:ietf:params:scim:schemas:"
    if (uri.startsWith(prefix)) {
        return uri.substring(prefix.length)
    }
    return uri
}

function shortenExtensionUri(uri: string): string {
    if (!uri) return ''
    const schemaPrefix = "urn:ietf:params:scim:schemas:"
    if (uri.startsWith(schemaPrefix)) {
        return uri.substring(schemaPrefix.length)
    }
    return uri
}

function openDetailPane(attr: IamAttributeMeta) {
    if (attr.type === 'COMPLEX') {
        // Just open the form pane directly, sub-attributes will be handled inside AttributeForm
        openDetailForm(attr)
    } else {
        openDetailForm(attr)
    }
}

function openDetailForm(attr: IamAttributeMeta | null) {
    const paneId = attr && attr.name ? `attr-${attr.name}` : `create-attr-${Date.now()}`
    
    // Check if pane exists
    const existingPane = millerStore.panes.find(p => p.id === paneId)
    if (existingPane) {
        millerStore.activePaneId = paneId
        return
    }

    const pane = {
        id: paneId,
        type: 'AttributeFormPane',
        title: attr && attr.name ? `Attribute: ${attr.displayName}` : 'New Attribute',
        data: { 
            initialData: attr && attr.name ? attr : null, 
            defaultSchemaUri: props.schemaUri, 
            paneIndex: (props.paneIndex ?? 0) + 1 
        },
        width: '500px'
    }
    
    // Add to stack
    if (typeof props.paneIndex === 'number') {
        millerStore.setPane(props.paneIndex + 1, pane)
    } else {
        millerStore.pushPane(pane)
    }
}

async function onDelete(name: String) {
    if (!confirm(`Are you sure you want to delete attribute ${name}?`)) return;
    try {
        await store.deleteAttribute(name as string)
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

        <!-- Header Metadata -->
        <div v-if="metadata" class="p-4 bg-neutral-50/30 border-b border-neutral-100/50">
             <div class="flex items-start gap-3">
                <div class="size-8 rounded bg-white border border-neutral-200 flex items-center justify-center shrink-0 shadow-sm"
                     :class="metadata.isAttribute ? 'text-green-600' : 'text-blue-500'"
                >
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
                    
                    <!-- Attribute Specific Info -->
                    <div v-if="metadata.isAttribute" class="mt-3 flex flex-wrap gap-2">
                        <Badge variant="secondary" class="text-[9px] px-1.5 py-0 h-4 bg-neutral-100 text-neutral-500 font-normal border-0">
                            {{ metadata.type }}
                        </Badge>
                         <Badge variant="outline" 
                               :class="[
                                   'text-[9px] px-1.5 py-0 h-4 font-normal transition-colors',
                                   metadata.mutability === 'READ_ONLY' || metadata.mutability === 'IMMUTABLE' 
                                        ? 'bg-amber-50 text-amber-600 border-amber-100' 
                                        : 'bg-emerald-50 text-emerald-600 border-emerald-100'
                               ]">
                             {{ metadata.mutability === 'READ_ONLY' ? 'RO' : (metadata.mutability === 'IMMUTABLE' ? 'IMM' : 'RW') }}
                        </Badge>
                        <Badge variant="outline" class="text-[9px] px-1.5 py-0 h-4 font-normal bg-blue-50 text-blue-600 border-blue-100">
                            {{ metadata.category }}
                        </Badge>
                    </div>

                    <!-- Schema Specific Info -->
                    <div v-else class="mt-3 grid grid-cols-2 gap-x-4 gap-y-2">
                        <div v-if="metadata.endpoint" class="flex flex-col gap-0.5">
                            <span class="text-[9px] font-bold text-neutral-400 uppercase tracking-wider">Endpoint</span>
                            <div class="flex items-center gap-1.5">
                                <span class="text-[10px] font-mono text-blue-600 bg-blue-50/50 px-1 rounded">{{ metadata.endpoint }}</span>
                                <ExternalLink class="size-2.5 text-neutral-300" />
                            </div>
                        </div>
                        <div v-if="metadata.schema && metadata.endpoint" class="flex flex-col gap-0.5">
                            <span class="text-[9px] font-bold text-neutral-400 uppercase tracking-wider">Base Schema</span>
                            <div class="text-[9px] font-mono text-neutral-600 truncate" :title="metadata.schema">
                                {{ shortenUri(metadata.schema) }}
                            </div>
                        </div>
                        <div v-else-if="metadata.schema" class="flex flex-col gap-0.5 col-span-2">
                             <span class="text-[9px] font-bold text-neutral-400 uppercase tracking-wider">Extension URI</span>
                             <div class="text-[9px] font-mono text-purple-600 bg-purple-50/50 px-1 rounded w-fit truncate" :title="metadata.schema">
                                {{ metadata.schema }}
                             </div>
                        </div>
                    </div>

                    <!-- Extensions if any (Only for Schema) -->
                    <div v-if="!metadata.isAttribute && metadata.schemaExtensions && metadata.schemaExtensions.length > 0" class="mt-3">
                        <span class="text-[9px] font-bold text-neutral-400 uppercase tracking-wider">Schema Extensions</span>
                        <div class="mt-1 space-y-1">
                            <div v-for="ext in metadata.schemaExtensions" :key="ext.schema" class="flex items-center gap-2">
                                <div class="size-1 rounded-full bg-neutral-300"></div>
                                <span class="text-[9px] font-mono text-neutral-500 truncate" :title="ext.schema">
                                    {{ shortenExtensionUri(ext.schema) }}
                                </span>
                                <span v-if="ext.required" class="text-[8px] bg-red-50 text-red-500 border border-red-100 px-1 rounded">REQ</span>
                            </div>
                        </div>
                    </div>
                </div>
             </div>
        </div>

        <!-- List -->
        <div class="flex-1 overflow-y-auto p-2">
            <div v-for="attr in filteredAttributes" :key="attr.name" 
                 @click="openDetailPane(attr)"
                 class="flex items-center justify-between p-3 border-b border-neutral-50 last:border-0 hover:bg-neutral-50 group transition-all cursor-pointer"
            >
                <div>
                     <div class="flex items-center gap-2">
                            <span class="text-sm font-medium text-neutral-800">{{ attr.displayName }}</span>
                            <span class="text-[10px] text-neutral-400 font-mono">{{ attr.name }}</span>
                            <span v-if="attr.required" class="text-[8px] text-red-500 font-bold uppercase border border-red-200 px-0.5 rounded">*Req</span>
                     </div>
                     <div class="flex items-center gap-2 mt-1">
                            <Badge variant="secondary" class="text-[9px] px-1.5 py-0 h-4 bg-neutral-100 text-neutral-500 font-normal border-0 hover:bg-neutral-100">
                                 {{ attr.type }}
                            </Badge>
                            <Badge variant="outline" 
                                   :class="[
                                       'text-[9px] px-1.5 py-0 h-4 font-normal transition-colors',
                                       attr.mutability === 'READ_ONLY' || attr.mutability === 'IMMUTABLE' 
                                            ? 'bg-amber-50 text-amber-600 border-amber-100' 
                                            : 'bg-emerald-50 text-emerald-600 border-emerald-100'
                                   ]">
                                 {{ attr.mutability === 'READ_ONLY' ? 'RO' : (attr.mutability === 'IMMUTABLE' ? 'IMM' : 'RW') }}
                            </Badge>
                     </div>
                </div>

                <div class="flex items-center gap-2">
                     <Lock v-if="attr.category === 'CORE' || isStandardSchema(attr.scimSchemaUri)" class="size-3 text-neutral-300" />
                     <div v-if="attr.category === 'EXTENSION' && !isStandardSchema(attr.scimSchemaUri)" class="opacity-0 group-hover:opacity-100 transition-opacity">
                        <Button 
                            variant="ghost" size="icon" 
                            class="size-6 hover:bg-red-50 hover:text-red-600"
                            @click.stop="onDelete(attr.name)"
                        >
                            <Trash2 class="size-3" />
                        </Button>
                     </div>
                </div>
            </div>
        </div>
    </div>
</template>
