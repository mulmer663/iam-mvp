<script setup lang="ts">
import {computed, onMounted, ref} from 'vue'
import {useAttributeStore} from '@/stores/attribute'
import {Button} from '@/components/ui/button'
import {ExternalLink, Info, Lock, Plus, Settings, Trash2, Save, X as CloseIcon} from 'lucide-vue-next'
import {Badge} from '@/components/ui/badge'
import {useMillerStore} from '@/stores/miller'
import {Input} from '@/components/ui/input'
import Textarea from '@/components/ui/textarea/Textarea.vue'
import {Label} from '@/components/ui/label'
import {Checkbox} from '@/components/ui/checkbox'
import type {AttributeTargetDomain, IamAttributeMeta} from '@/types/attribute'
import type {ScimResourceTypeDto} from '@/types/scim'

import {useResourceTypeStore} from '@/stores/resourceType'
import {toast} from '@/utils/toast'

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
            id: rt.id,
            name: rt.name,
            description: rt.description,
            endpoint: rt.endpoint, 
            schema: rt.schema,
            schemaExtensions: rt.schemaExtensions,
            type: undefined,
            mutability: undefined,
            category: undefined,
            rawRt: rt
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

const isEditMode = ref(false)
const formData = ref<ScimResourceTypeDto | null>(null)
const isSaving = ref(false)

function startEdit() {
    if (metadata.value?.rawRt) {
        const data = JSON.parse(JSON.stringify(metadata.value.rawRt))
        // Mark existing extensions so we can lock their PK info
        data.schemaExtensions.forEach((e: any) => {
            e.isExisting = true
        })
        formData.value = data
        isEditMode.value = true
    }
}

function cancelEdit() {
    isEditMode.value = false
    formData.value = null
}

async function saveResourceType() {
    if (!formData.value) return
    isSaving.value = true
    try {
        // Clean up internal flags before sending to API
        const payload = JSON.parse(JSON.stringify(formData.value))
        payload.schemaExtensions = payload.schemaExtensions.map((e: any) => ({
            schema: e.schema,
            required: e.required
        }))
        
        await resourceTypeStore.updateResourceType(payload)
        isEditMode.value = false
        formData.value = null
        toast.success('Resource Type updated')
    } catch (e) {
        toast.error('Failed to save resource type')
    } finally {
        isSaving.value = false
    }
}

function addExtension() {
    if (formData.value) {
        formData.value.schemaExtensions.push({ schema: '', required: false })
    }
}

function removeExtension(index: number) {
    if (formData.value && formData.value.schemaExtensions[index]) {
        const extUri = formData.value.schemaExtensions[index].schema
        const hasAttrs = store.attributes.some(a => a.scimSchemaUri === extUri)
        
        if (hasAttrs && extUri) {
            const attrCount = store.attributes.filter(a => a.scimSchemaUri === extUri).length
            if (!confirm(`Warn: 이 익스텐션(${shortenExtensionUri(extUri)})에 속한 속성이 ${attrCount}개 존재합니다. 삭제하면 해당 속성들이 이 리소스 타입에서 보이지 않게 됩니다. 그래도 삭제하시겠습니까?`)) {
                return
            }
        }
        formData.value.schemaExtensions.splice(index, 1)
    }
}

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
           uri.startsWith('urn:ietf:params:scim:schemas:extension:enterprise:2.0:User')
}

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

async function onDelete(name: string, targetDomain: AttributeTargetDomain) {
    if (!confirm(`Are you sure you want to delete attribute ${name} (${targetDomain})?`)) return;
    try {
        await store.deleteAttribute(name, targetDomain)
        toast.success('Attribute deleted')
    } catch (e) {
        toast.error('Failed to delete attribute')
    }
}
</script>

<template>
    <div class="h-full flex flex-col bg-white">
        <!-- Toolbar -->
        <div class="h-12 border-b border-neutral-100 flex items-center px-4 justify-between shrink-0">
             <div class="font-bold text-base text-neutral-700">
                Attributes
             </div>
            
            <Button size="xs" class="bg-blue-600 text-white hover:bg-blue-700 font-bold flex gap-1 h-8 px-3 text-sm" @click="onCreate">
                <Plus class="size-4" /> Add
            </Button>
        </div>

        <!-- Header Metadata -->
        <div v-if="metadata" class="p-4 bg-neutral-50/30 border-b border-neutral-100/50">
             <div v-if="!isEditMode" class="flex items-start gap-3">
                <div class="size-8 rounded bg-white border border-neutral-200 flex items-center justify-center shrink-0 shadow-sm"
                     :class="metadata.isAttribute ? 'text-green-600' : 'text-blue-500'"
                >
                    <Info class="size-4" />
                </div>
                <div class="flex-1 min-w-0">
                    <div class="flex items-center justify-between">
                        <div class="flex items-center gap-2">
                             <span class="text-sm font-black text-neutral-900 tracking-tight">{{ metadata.name }}</span>
                             <Badge variant="outline" class="text-sm h-6 bg-white font-mono">{{ metadata.id }}</Badge>
                        </div>
                        <Button v-if="metadata.rawRt" size="xs" variant="ghost" class="h-7 text-sm text-blue-600" @click="startEdit">
                            <Settings class="size-4 mr-1" /> Edit Info
                        </Button>
                    </div>
                    <p class="text-sm text-neutral-500 mt-1 leading-relaxed">
                        {{ metadata.description }}
                    </p>
                    
                    <!-- Attribute Specific Info -->
                    <div v-if="metadata.isAttribute" class="mt-3 flex flex-wrap gap-2">
                        <Badge variant="secondary" class="text-xs px-1.5 py-0 h-5 bg-neutral-100 text-neutral-500 font-normal border-0">
                            {{ metadata.type }}
                        </Badge>
                         <Badge variant="outline" 
                               :class="[
                                   'text-xs px-1.5 py-0 h-5 font-normal transition-colors',
                                   metadata.mutability === 'READ_ONLY' || metadata.mutability === 'IMMUTABLE' 
                                        ? 'bg-amber-50 text-amber-600 border-amber-100' 
                                        : 'bg-emerald-50 text-emerald-600 border-emerald-100'
                               ]">
                             {{ metadata.mutability === 'READ_ONLY' ? 'RO' : (metadata.mutability === 'IMMUTABLE' ? 'IMM' : 'RW') }}
                        </Badge>
                        <Badge variant="outline" class="text-xs px-1.5 py-0 h-5 font-normal bg-blue-50 text-blue-600 border-blue-100">
                            {{ metadata.category }}
                        </Badge>
                    </div>

                    <!-- Schema Specific Info -->
                    <div v-else class="mt-3 grid grid-cols-2 gap-x-4 gap-y-2">
                        <div v-if="metadata.endpoint" class="flex flex-col gap-0.5">
                            <span class="text-xs font-bold text-neutral-400 uppercase tracking-wider">Endpoint</span>
                            <div class="flex items-center gap-1.5">
                                <span class="text-sm font-mono text-blue-600 bg-blue-50/50 px-1 rounded">{{ metadata.endpoint }}</span>
                                <ExternalLink class="size-3 text-neutral-300" />
                            </div>
                        </div>
                        <div v-if="metadata.schema && metadata.endpoint" class="flex flex-col gap-0.5">
                            <span class="text-xs font-bold text-neutral-400 uppercase tracking-wider">Base Schema</span>
                            <div class="text-sm font-mono text-neutral-600 truncate" :title="metadata.schema">
                                {{ shortenUri(metadata.schema) }}
                            </div>
                        </div>
                        <div v-else-if="metadata.schema" class="flex flex-col gap-0.5 col-span-2">
                             <span class="text-xs font-bold text-neutral-400 uppercase tracking-wider">Extension URI</span>
                             <div class="text-sm font-mono text-purple-600 bg-purple-50/50 px-2 py-1 rounded break-all whitespace-normal border border-purple-100" :title="metadata.schema">
                                {{ metadata.schema }}
                             </div>
                        </div>
                    </div>

                    <!-- Extensions if any (Only for Schema) -->
                    <div v-if="!metadata.isAttribute && metadata.schemaExtensions && metadata.schemaExtensions.length > 0" class="mt-3">
                        <span class="text-xs font-bold text-neutral-400 uppercase tracking-wider">Schema Extensions</span>
                        <div class="mt-1 space-y-1">
                            <div v-for="ext in metadata.schemaExtensions" :key="ext.schema" class="flex items-center gap-2">
                                <div class="size-1 rounded-full bg-neutral-300"></div>
                                <button class="text-sm font-mono text-purple-600 hover:underline text-left transition-colors" 
                                        :title="ext.schema"
                                        @click="openAttributePane(ext.schema, shortenExtensionUri(ext.schema))"
                                >
                                    {{ shortenExtensionUri(ext.schema) }}
                                </button>
                                <span v-if="ext.required" class="text-[10px] bg-red-50 text-red-500 border border-red-100 px-1.5 py-0.5 rounded font-bold">REQ</span>
                            </div>
                        </div>
                    </div>
                </div>
             </div>

             <!-- Edit Mode Form -->
             <div v-else-if="formData" class="space-y-4">
                <div class="flex items-center justify-between">
                    <span class="text-xs font-black text-blue-600 uppercase tracking-widest">Edit Resource Details</span>
                     <div class="flex gap-1">
                         <Button size="xs" variant="ghost" class="h-7 px-2 text-sm" @click="cancelEdit">
                             <CloseIcon class="size-4 mr-1" /> Cancel
                         </Button>
                         <Button size="xs" class="h-7 px-2 text-sm bg-blue-600 text-white hover:bg-blue-700" @click="saveResourceType" :disabled="isSaving">
                             <Save class="size-4 mr-1" /> {{ isSaving ? 'Saving...' : 'Save' }}
                         </Button>
                     </div>
                </div>

                 <div class="grid grid-cols-2 gap-3">
                     <div class="space-y-1">
                         <Label class="text-xs font-bold text-neutral-400 uppercase">Name</Label>
                         <Input v-model="formData.name" class="h-8 text-sm" />
                     </div>
                     <div class="space-y-1">
                         <Label class="text-xs font-bold text-neutral-400 uppercase">Endpoint</Label>
                         <Input v-model="formData.endpoint" class="h-8 text-sm" :disabled="isStandardSchema(formData.schema)" />
                     </div>
                 </div>

                 <div class="space-y-1">
                     <Label class="text-xs font-bold text-neutral-400 uppercase">Description</Label>
                     <Input v-model="formData.description" class="h-8 text-sm" />
                 </div>

                <!-- Extensions Edit -->
                <div class="space-y-2 pt-2">
                     <div class="flex items-center justify-between">
                         <Label class="text-xs font-bold text-neutral-400 uppercase">Schema Extensions</Label>
                         <Button size="xs" variant="ghost" class="h-6 text-sm text-blue-600" @click="addExtension">
                             <Plus class="size-3 mr-1" /> Add Extension
                         </Button>
                     </div>
                    <div v-for="(ext, idx) in formData.schemaExtensions" :key="idx" class="flex flex-col gap-2 p-2 bg-white border border-neutral-100 rounded">
                        <div class="flex items-start gap-2">
                            <Textarea v-model="ext.schema" class="min-h-[60px] text-sm font-mono flex-1 leading-normal" 
                                      placeholder="urn:ietf:params:scim:schemas:extension:..."
                                      :disabled="(ext as any).isExisting" />
                            <Button v-if="!(ext as any).isExisting || !isStandardSchema(ext.schema)" 
                                    variant="ghost" size="icon" class="size-6 hover:text-red-500 shrink-0" @click="removeExtension(idx)">
                                <Trash2 class="size-4" />
                            </Button>
                        </div>
                        <div class="flex items-center gap-1">
                            <Checkbox :id="'ext-req-'+idx" :checked="ext.required" @update:checked="(v: boolean) => ext.required = v" />
                            <Label :for="'ext-req-'+idx" class="text-xs">Required</Label>
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
                            <span class="text-xs text-neutral-400 font-mono">{{ attr.name }}</span>
                            <span v-if="attr.required" class="text-[10px] text-red-500 font-bold uppercase border border-red-200 px-1 rounded shadow-sm bg-red-50/50">*Req</span>
                     </div>
                     <div class="flex items-center gap-2 mt-1">
                            <Badge variant="secondary" class="text-xs px-1.5 py-0 h-5 bg-neutral-100 text-neutral-500 font-normal border-0 hover:bg-neutral-100">
                                 {{ attr.type }}
                            </Badge>
                            <Badge variant="outline" 
                                   :class="[
                                       'text-sm px-2 py-0.5 h-6 font-normal transition-colors',
                                       attr.mutability === 'READ_ONLY' || attr.mutability === 'IMMUTABLE' 
                                            ? 'bg-amber-50 text-amber-600 border-amber-100' 
                                            : 'bg-emerald-50 text-emerald-600 border-emerald-100'
                                   ]">
                                  {{ attr.mutability === 'READ_ONLY' ? 'RO' : (attr.mutability === 'IMMUTABLE' ? 'IMM' : 'RW') }}
                             </Badge>
                     </div>
                </div>

                <div class="flex items-center gap-2">
                     <Lock v-if="attr.category === 'CORE'" class="size-3 text-neutral-300" />
                     <div v-if="attr.category !== 'CORE'" class="opacity-0 group-hover:opacity-100 transition-opacity">
                        <Button 
                            variant="ghost" size="icon" 
                            class="size-6 hover:bg-red-50 hover:text-red-600"
                            @click.stop="onDelete(attr.name, attr.targetDomain)"
                        >
                            <Trash2 class="size-3" />
                        </Button>
                     </div>
                </div>
            </div>
        </div>
    </div>
</template>
