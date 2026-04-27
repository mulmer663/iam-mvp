<script setup lang="ts">
import {computed, ref, watch} from 'vue'
import {Button} from '@/components/ui/button'
import {Input} from '@/components/ui/input'
import {Label} from '@/components/ui/label'
import {Plus, Trash2} from 'lucide-vue-next'
import {Checkbox} from '@/components/ui/checkbox'
import type {ScimResourceTypeDto} from '@/types/scim'
import {useResourceTypeStore} from '@/stores/resourceType'
import {useMillerStore} from '@/stores/miller'

const props = defineProps<{
  initialData?: ScimResourceTypeDto | null
  paneIndex?: number
}>()

const emit = defineEmits(['save', 'cancel'])
const store = useResourceTypeStore()
const millerStore = useMillerStore()
const isEditMode = ref(false)
const isCreate = computed(() => !props.initialData)

const isStandardSchema = (uri?: string) => {
    if (!uri) return false
    return uri.startsWith('urn:ietf:params:scim:schemas:core:2.0:') || 
           uri.startsWith('urn:ietf:params:scim:schemas:extension:enterprise:2.0:')
}

const isLocked = computed(() => {
    if (isCreate.value) return false
    // Also consider it locked if it matches User, Group core or Enterprise extensions
    return isStandardSchema(formData.value.schema)
})

// Form State
const formData = ref<ScimResourceTypeDto>({
  id: '',
  name: '',
  description: '',
  endpoint: '',
  schema: '',
  schemaExtensions: []
})

watch(() => props.initialData, (newVal) => {
    if (newVal) {
        formData.value = JSON.parse(JSON.stringify(newVal))
        
        // Normalize schemaExtensions (handle string array from old backend or object array from new)
        if (formData.value.schemaExtensions) {
            formData.value.schemaExtensions = formData.value.schemaExtensions.map(ext => {
                if (typeof ext === 'string') {
                    return { schema: ext, required: false }
                }
                return ext
            })
        }
        
        isEditMode.value = false
    } else {
        formData.value = {
            id: '',
            name: '',
            description: '',
            endpoint: '',
            schema: '',
            schemaExtensions: []
        }
        isEditMode.value = true
    }
}, { immediate: true })

const isSaving = ref(false)
const error = ref<string | null>(null)

async function onSubmit() {
    error.value = null
    isSaving.value = true
    try {
        if (!isCreate.value) {
            await store.updateResourceType(formData.value)
            isEditMode.value = false
        } else {
            await store.createResourceType(formData.value)
            alert('Resource Type created successfully')
            emit('save')
            // Close pane if paneIndex is provided
            if (typeof props.paneIndex === 'number') {
                millerStore.removePane(props.paneIndex)
            }
        }
    } catch (e: any) {
        error.value = e.response?.data?.message || e.message || 'Failed to save'
    } finally {
        isSaving.value = false
    }
}

function addExtension() {
    formData.value.schemaExtensions.push({ schema: '', required: false })
}

function removeExtension(index: number) {
    formData.value.schemaExtensions.splice(index, 1)
}

function enableEdit() {
    isEditMode.value = true
}

function cancelEdit() {
    if (isCreate.value) {
        emit('cancel')
    } else {
        isEditMode.value = false
        if (props.initialData) formData.value = JSON.parse(JSON.stringify(props.initialData))
    }
}
</script>

<template>
  <div class="h-full flex flex-col bg-white">
    <div class="h-10 border-b border-neutral-100 flex items-center justify-between px-3 shrink-0 bg-neutral-50/50">
        <div class="font-bold text-sm text-neutral-700">
            {{ isCreate ? 'New Resource' : formData.name }}
        </div>
        <div v-if="!isCreate && !isEditMode">
            <Button 
                size="xs" variant="outline" @click="enableEdit" class="h-7 text-xs"
            >
                Edit Resource
            </Button>
        </div>
    </div>

    <div class="flex-1 overflow-y-auto p-4 space-y-4">
       <div class="grid grid-cols-2 gap-4">
           <div class="space-y-2">
               <Label>ID</Label>
               <Input v-model="formData.id" :disabled="!isCreate" placeholder="e.g. User" />
           </div>
           
           <div class="space-y-2">
               <Label>Name</Label>
               <Input v-model="formData.name" :disabled="!isEditMode" placeholder="e.g. User Account" />
           </div>
       </div>

       <div class="space-y-2">
           <Label>Description</Label>
           <Input v-model="formData.description" :disabled="!isEditMode" />
       </div>

        <div class="grid grid-cols-2 gap-4">
            <div class="space-y-2">
                <Label>Endpoint</Label>
                <Input v-model="formData.endpoint" :disabled="!isEditMode || isLocked" placeholder="e.g. /Users" />
            </div>
            
            <div class="space-y-2">
                <Label>Base Schema URI</Label>
                <Input v-model="formData.schema" :disabled="!isEditMode || isLocked" placeholder="urn:..." />
            </div>
        </div>

       <!-- Extensions -->
       <div class="space-y-3">
           <div class="flex items-center justify-between">
               <Label class="text-xs font-bold text-neutral-500 uppercase">Schema Extensions</Label>
               <Button v-if="isEditMode" size="xs" variant="ghost" class="h-6 text-[10px] text-blue-600" @click="addExtension">
                   <Plus class="size-3 mr-1" /> Add Extension
               </Button>
           </div>
           
           <div v-for="(ext, idx) in formData.schemaExtensions" :key="idx" 
                class="p-3 bg-neutral-50/50 border border-neutral-100 rounded-lg space-y-3 relative group"
           >
               <div class="space-y-1.5">
                   <Label class="text-[10px] font-medium text-neutral-400">Extension URI</Label>
                   <Input v-model="ext.schema" :disabled="!isEditMode || (isLocked && isStandardSchema(ext.schema))" 
                          placeholder="urn:ietf:params:scim:schemas:extension:..." class="h-8 text-xs font-mono" />
               </div>

               <div v-if="isEditMode" class="grid grid-cols-2 gap-2">
                   <div class="space-y-1.5">
                       <Label class="text-[10px] font-medium text-neutral-400">Extension Name</Label>
                       <Input v-model="ext.name" placeholder="e.g. EnterpriseUser" class="h-8 text-xs" />
                   </div>
                   <div class="space-y-1.5">
                       <Label class="text-[10px] font-medium text-neutral-400">Description</Label>
                       <Input v-model="ext.description" placeholder="Optional description" class="h-8 text-xs" />
                   </div>
               </div>

               <div class="flex items-center space-x-2">
                   <Checkbox :id="'req-'+idx" :model-value="ext.required"
                             @update:model-value="(v: any) => ext.required = !!v"
                             :disabled="!isEditMode || (isLocked && isStandardSchema(ext.schema))" />
                   <Label :for="'req-'+idx" class="text-xs">Required</Label>
               </div>

               <Button v-if="isEditMode && !(isLocked && isStandardSchema(ext.schema))" 
                       variant="ghost" size="icon" 
                       class="absolute top-1 right-1 size-6 opacity-0 group-hover:opacity-100 text-neutral-400 hover:text-red-500 transition-all"
                       @click="removeExtension(idx)"
               >
                   <Trash2 class="size-3" />
               </Button>
           </div>
           
           <div v-if="formData.schemaExtensions.length === 0" class="text-[10px] text-neutral-300 italic text-center py-2 border border-dashed rounded">
               No extensions defined
           </div>
       </div>
    </div>

    <div v-if="error" class="text-red-500 text-xs px-4 pb-2">{{ error }}</div>

    <div v-if="isEditMode" class="flex justify-end gap-2 p-4 border-t border-neutral-100 bg-neutral-50">
        <Button variant="outline" @click="cancelEdit" :disabled="isSaving">Cancel</Button>
        <Button @click="onSubmit" :disabled="isSaving" class="bg-blue-600 hover:bg-blue-700 text-white">
            {{ isSaving ? 'Saving...' : (isCreate ? 'Create Resource' : 'Save Changes') }}
        </Button>
    </div>
  </div>
</template>
