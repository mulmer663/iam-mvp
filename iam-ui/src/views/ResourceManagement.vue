<script setup lang="ts">
import {computed, onMounted} from 'vue'
import {Button} from '@/components/ui/button'
import {Badge} from '@/components/ui/badge'
import {Database, Lock, Plus, Settings, Trash2} from 'lucide-vue-next'
import {useMillerStore} from '@/stores/miller'
import {useAttributeStore} from '@/stores/attribute'
import {useResourceTypeStore} from '@/stores/resourceType'
import {toast} from '@/utils/toast'

const millerStore = useMillerStore()
const attributeStore = useAttributeStore()
const resourceTypeStore = useResourceTypeStore()
const props = defineProps<{
    paneIndex?: number
}>()

onMounted(() => {
    attributeStore.fetchAttributes()
    resourceTypeStore.fetchResourceTypes()
})

const isStandardSchema = (uri?: string) => {
    if (!uri) return false
    return uri.startsWith('urn:ietf:params:scim:schemas:core:2.0:') || 
           uri.startsWith('urn:ietf:params:scim:schemas:extension:enterprise:2.0:')
}

// Group attributes by Schema URI and ensure all known schemas are included
const schemas = computed(() => {
    const groups: Record<string, { uri: string, count: number, fixed: boolean }> = {}
    
    // Initialize with all known schemas from store (Base + Extensions)
    resourceTypeStore.resourceTypes.forEach(rt => {
        // Base Schema
        if (!groups[rt.schema]) {
            groups[rt.schema] = { uri: rt.schema, count: 0, fixed: isStandardSchema(rt.schema) }
        }
        // Extensions
        rt.schemaExtensions?.forEach(ext => {
            if (!groups[ext.schema]) {
                groups[ext.schema] = { uri: ext.schema, count: 0, fixed: isStandardSchema(ext.schema) }
            }
        })
    })
    
    // Count attributes
    attributeStore.attributes.forEach(attr => {
        const uri = attr.scimSchemaUri || 'urn:ietf:params:scim:schemas:core:2.0:User'
        if (!groups[uri]) {
            groups[uri] = { uri: uri, count: 0, fixed: false }
        }
        groups[uri].count++
    })
    
    const list = Object.values(groups).map(g => {
        const rt = resourceTypeStore.resourceTypes.find(r => r.schema === g.uri)
        return {
            ...g,
            displayName: rt ? rt.name : shortenUri(g.uri),
            isExtension: g.uri.includes(':extension:')
        }
    })

    return {
        core: list.filter(s => !s.isExtension),
        extension: list.filter(s => s.isExtension)
    }
})

function shortenUri(uri: string): string {
    if (!uri) return ''
    const prefix = "urn:ietf:params:scim:schemas:"
    if (uri.startsWith(prefix)) {
        return uri.substring(prefix.length)
    }
    return uri
}

function openAttributePane(schemaUri: string, displayName: string) {
    const paneId = `schema-${schemaUri.replace(/[:.]/g, '-')}`
    
    // Check if pane exists
    const existingPane = millerStore.panes.find(p => p.id === paneId)
    if (existingPane) {
        millerStore.activePaneId = paneId
        return
    }

    const pane = {
        id: paneId,
        type: 'AttributeManagementPane', 
        title: displayName,
        data: { schemaUri: schemaUri }, // Pass schema context
        width: '500px'
    }
    
    // Add to stack
    if (typeof props.paneIndex === 'number') {
        millerStore.setPane(props.paneIndex + 1, pane)
    } else {
        millerStore.pushPane(pane)
    }
}

function onCreate() {
    const paneId = `create-rt-${Date.now()}`
    const pane = {
        id: paneId,
        type: 'ResourceTypeFormPane',
        title: 'New Resource Type',
        data: { initialData: null, paneIndex: (props.paneIndex ?? 0) + 1 },
        width: '500px'
    }
    
    if (typeof props.paneIndex === 'number') {
        millerStore.setPane(props.paneIndex + 1, pane)
    } else {
        millerStore.pushPane(pane)
    }
}

function openResourceTypeDetail(schemaUri: string, displayName: string) {
    openAttributePane(schemaUri, displayName)
}

async function onDelete(schemaUri: string) {
    const rt = resourceTypeStore.resourceTypes.find(r => r.schema === schemaUri)
    if (!rt) return
    if (!confirm(`Are you sure you want to delete resource type ${rt.name}?`)) return
    
    try {
        await resourceTypeStore.deleteResourceType(rt.id)
        toast.success('Resource Type deleted')
    } catch (e) {
        toast.error('Failed to delete resource type')
    }
}
</script>

<template>
    <div class="h-full flex flex-col bg-white">
        <!-- Toolbar -->
        <div class="h-10 border-b border-neutral-100 flex items-center px-3 justify-between shrink-0 bg-neutral-50/50">
            <span class="font-bold text-sm text-neutral-700">Resource Types</span>
            <Button size="xs" variant="outline" class="h-7 text-xs flex gap-1" @click="onCreate">
                <Plus class="size-3" /> New Resource Type
            </Button>
        </div>

        <!-- List Grouped -->
        <div class="flex-1 overflow-y-auto p-2 space-y-6">
            <!-- Core Schemas -->
            <div v-if="schemas.core.length > 0" class="space-y-2">
                <div class="px-2 text-[10px] font-bold text-neutral-400 uppercase tracking-widest">Core Schemas</div>
                <div v-for="schema in schemas.core" :key="schema.uri" 
                     @click="openAttributePane(schema.uri, schema.displayName)"
                     class="flex items-center justify-between p-3 border border-neutral-100 rounded-lg hover:bg-neutral-50 cursor-pointer group hover:border-neutral-300 transition-all shadow-sm"
                >
                    <div class="flex items-center gap-3 overflow-hidden">
                        <div class="size-9 rounded-md bg-white border border-neutral-200 flex items-center justify-center text-blue-500 shrink-0">
                            <Database class="size-4" />
                        </div>
                        <div class="flex-1 min-w-0">
                            <div class="flex items-center gap-2">
                                <span class="font-bold text-sm text-neutral-800 truncate" :title="schema.displayName">{{ schema.displayName }}</span>
                                <Badge variant="outline" class="text-[8px] px-1 h-3.5 font-bold uppercase tracking-tighter bg-blue-50 text-blue-600 border-blue-200">Core</Badge>
                            </div>
                            <div class="text-[10px] text-neutral-400 truncate" :title="schema.uri">{{ schema.uri }}</div>
                        </div>
                    </div>
                    <div class="flex items-center gap-2 shrink-0">
                         <span class="text-[10px] bg-neutral-100 px-1.5 py-0.5 rounded text-neutral-500 font-medium">{{ schema.count }} Attrs</span>
                         <div class="opacity-0 group-hover:opacity-100 transition-opacity flex items-center gap-1">
                            <Button variant="ghost" size="icon" class="size-6 hover:bg-neutral-100" @click.stop="openResourceTypeDetail(schema.uri, schema.displayName)">
                                <Settings class="size-3 text-neutral-400" />
                            </Button>
                         </div>
                         <Lock v-if="schema.fixed" class="size-3 text-neutral-300" />
                    </div>
                </div>
            </div>

            <!-- Extension Schemas -->
            <div v-if="schemas.extension.length > 0" class="space-y-2">
                <div class="px-2 text-[10px] font-bold text-neutral-400 uppercase tracking-widest">Extension Schemas</div>
                <div v-for="schema in schemas.extension" :key="schema.uri" 
                     @click="openAttributePane(schema.uri, schema.displayName)"
                     class="flex items-center justify-between p-3 border border-neutral-100 rounded-lg hover:bg-neutral-50 cursor-pointer group hover:border-neutral-300 transition-all shadow-sm"
                >
                    <div class="flex items-center gap-3 overflow-hidden">
                        <div class="size-9 rounded-md bg-white border border-neutral-200 flex items-center justify-center text-purple-500 shrink-0">
                            <Settings class="size-4" />
                        </div>
                        <div class="flex-1 min-w-0">
                            <div class="flex items-center gap-2">
                                <span class="font-bold text-sm text-neutral-800 truncate" :title="schema.displayName">{{ schema.displayName }}</span>
                                <Badge variant="outline" class="text-[8px] px-1 h-3.5 font-bold uppercase tracking-tighter bg-purple-50 text-purple-600 border-purple-200">Ext</Badge>
                            </div>
                            <div class="text-[10px] text-neutral-400 truncate" :title="schema.uri">{{ schema.uri }}</div>
                        </div>
                    </div>
                    <div class="flex items-center gap-2 shrink-0">
                         <span class="text-[10px] bg-neutral-100 px-1.5 py-0.5 rounded text-neutral-500 font-medium">{{ schema.count }} Attrs</span>
                         <div class="opacity-0 group-hover:opacity-100 transition-opacity flex items-center gap-1">
                            <Button variant="ghost" size="icon" class="size-6 hover:bg-neutral-100" @click.stop="openResourceTypeDetail(schema.uri, schema.displayName)">
                                <Settings class="size-3 text-neutral-400" />
                            </Button>
                             <Button 
                                 v-if="!schema.fixed"
                                 variant="ghost" size="icon" class="size-6 hover:bg-red-50 hover:text-red-500"
                                 @click.stop="onDelete(schema.uri)"
                            >
                                 <Trash2 class="size-3" />
                             </Button>
                         </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>
