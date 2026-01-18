<script setup lang="ts">
import {computed, onMounted} from 'vue'
import {Button} from '@/components/ui/button'
import {Database, Lock, Plus} from 'lucide-vue-next'
import {useMillerStore} from '@/stores/miller'
import {useAttributeStore} from '@/stores/attribute'

import {getMetadataBySchema, SCIM_RESOURCE_TYPES} from '@/utils/scim-metadata'

const millerStore = useMillerStore()
const attributeStore = useAttributeStore()
const props = defineProps<{
    paneIndex?: number
}>()

onMounted(() => {
    attributeStore.fetchAttributes()
})

// Group attributes by Schema URI and ensure all known schemas are included
const schemas = computed(() => {
    const groups: Record<string, { uri: string, count: number, fixed: boolean }> = {}
    
    // Initialize with all known schemas from metadata
    Object.keys(SCIM_RESOURCE_TYPES).forEach(uri => {
        groups[uri] = { uri: uri, count: 0, fixed: false }
    })
    
    // Count attributes and mark fixed if CORE
    attributeStore.attributes.forEach(attr => {
        const uri = attr.scimSchemaUri || 'urn:ietf:params:scim:schemas:core:2.0:User' // Fallback
        if (!groups[uri]) {
            groups[uri] = { uri: uri, count: 0, fixed: false }
        }
        groups[uri].count++
        if (attr.category === 'CORE') groups[uri].fixed = true
    })
    
    return Object.values(groups).map(g => {
        const meta = getMetadataBySchema(g.uri)
        return {
            ...g,
            displayName: meta ? meta.name : shortenUri(g.uri)
        }
    })
})

function shortenUri(uri: string): string {
    // Logic: extract the last meaningful parts. 
    // e.g. urn:ietf:params:scim:schemas:core:2.0:User -> core:2.0:User
    // e.g. urn:ietf:params:scim:schemas:extension:enterprise:2.0:User -> extension:enterprise:2.0:User
    
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
</script>

<template>
    <div class="h-full flex flex-col bg-white">
        <!-- Toolbar -->
        <div class="h-10 border-b border-neutral-100 flex items-center px-3 justify-between shrink-0 bg-neutral-50/50">
            <span class="font-bold text-sm text-neutral-700">SCIM Schemas</span>
            <Button size="xs" variant="outline" class="h-7 text-xs flex gap-1">
                <Plus class="size-3" /> New Schema
            </Button>
        </div>

        <!-- List -->
        <div class="flex-1 overflow-y-auto p-2 space-y-2">
            <div v-for="schema in schemas" :key="schema.uri" 
                 @click="openAttributePane(schema.uri, schema.displayName)"
                 class="flex items-center justify-between p-3 border border-neutral-100 rounded-lg hover:bg-neutral-50 cursor-pointer group hover:border-neutral-300 transition-all shadow-sm"
            >
                <div class="flex items-center gap-3 overflow-hidden">
                    <div class="size-9 rounded-md bg-white border border-neutral-200 flex items-center justify-center text-neutral-500 shrink-0">
                        <Database class="size-4" />
                    </div>
                    <div class="flex-1 min-w-0">
                        <div class="font-bold text-sm text-neutral-800 truncate" :title="schema.displayName">{{ schema.displayName }}</div>
                        <div class="text-[10px] text-neutral-400 truncate" :title="schema.uri">{{ schema.uri }}</div>
                    </div>
                </div>
                <div class="flex items-center gap-2 shrink-0">
                     <span class="text-[10px] bg-neutral-100 px-1.5 py-0.5 rounded text-neutral-500 font-medium">{{ schema.count }} Attrs</span>
                     <Lock v-if="schema.fixed" class="size-3 text-neutral-300" />
                </div>
            </div>
        </div>
    </div>
</template>
