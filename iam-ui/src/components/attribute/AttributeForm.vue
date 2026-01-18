<script setup lang="ts">
import {computed, ref, watch} from 'vue'
import {Button} from '@/components/ui/button'
import {Input} from '@/components/ui/input'
import {Label} from '@/components/ui/label'
import {Checkbox} from '@/components/ui/checkbox'
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue,} from '@/components/ui/select'
import type {IamAttributeMeta} from '@/types/attribute'
import {useAttributeStore} from '@/stores/attribute'

const props = defineProps<{
  initialData?: IamAttributeMeta | null
  defaultSchemaUri?: string // Context for creation
}>()

const emit = defineEmits(['save', 'cancel'])
const store = useAttributeStore()
const isEditMode = ref(false)
const isCreate = computed(() => !props.initialData)

// Initialize
watch(() => props.initialData, (val) => {
    isEditMode.value = !val // If new (no data), edit mode is true. If existing, false (view mode).
}, { immediate: true })

// Form State
const formData = ref<IamAttributeMeta>({
  code: '',
  targetDomain: 'USER', // TODO: Infer from Schema?
  category: 'EXTENSION', 
  scimSchemaUri: props.defaultSchemaUri || 'urn:ietf:params:scim:schemas:extension:enterprise:2.0:User',
  displayName: '',
  dataType: 'STRING',
  description: '',
  required: false,
  mutability: 'READ_WRITE',
  adminOnly: false,
  viewLevel: 0,
  editLevel: 0,
  encrypted: false,
  uiComponent: 'text-input'
})

// Validation for readonly Mode
function canEditField(field: keyof IamAttributeMeta) {
    if (isCreate.value) return true // All editable on create
    if (!isEditMode.value) return false // Nothing editable in view mode

    // Specific field immutability rules for Update
    if (field === 'code') return false
    if (field === 'category') return false
    if (field === 'targetDomain') return false
    if (field === 'dataType') return false
    if (field === 'scimSchemaUri') {
         // If a default schema was enforced (via context), it shouldn't be changed.
         // Or if it's Extension, maybe fine? BUT for consistency in "Schema View", it should be locked.
         if (props.defaultSchemaUri) return false;
         return true
    }

    return true
}

function enableEdit() {
    isEditMode.value = true
}

function cancelEdit() {
    if (isCreate.value) {
        emit('cancel')
    } else {
        isEditMode.value = false
        // Reset data
        if (props.initialData) formData.value = { ...props.initialData }
    }
}

// Initialize form
watch(() => props.initialData, (newVal) => {
    if (newVal) {
        formData.value = { ...newVal }
    } else {
        // Reset defaults
        formData.value = {
            code: '',
            targetDomain: 'USER',
            category: 'EXTENSION',
            scimSchemaUri: props.defaultSchemaUri || 'urn:ietf:params:scim:schemas:extension:enterprise:2.0:User',
            displayName: '',
            dataType: 'STRING',
            description: '',
            required: false,
            mutability: 'READ_WRITE',
            adminOnly: false,
            viewLevel: 0,
            editLevel: 0,
            encrypted: false,
            uiComponent: 'text-input'
        }
    }
}, { immediate: true })

const isSaving = ref(false)
const error = ref<string | null>(null)

async function onSubmit() {
    error.value = null
    isSaving.value = true
    try {
        if (!isCreate.value) {
            await store.updateAttribute(formData.value)
            isEditMode.value = false // Return to view mode
        } else {
            await store.createAttribute(formData.value)
            emit('save')
        }
    } catch (e: any) {
        error.value = e.response?.data?.message || e.message || 'Failed to save'
    } finally {
        isSaving.value = false
    }
}
</script>

<template>
  <div class="h-full flex flex-col bg-white">
    <!-- Header -->
    <div class="h-10 border-b border-neutral-100 flex items-center justify-between px-3 shrink-0 bg-neutral-50/50">
        <div class="font-bold text-sm text-neutral-700">
            {{ isCreate ? 'New Attribute' : formData.displayName }}
        </div>
        <div v-if="!isCreate && !isEditMode">
            <Button size="xs" variant="outline" @click="enableEdit" class="h-7 text-xs">
                Edit Attribute
            </Button>
        </div>
    </div>

    <!-- Content -->
    <div class="flex-1 overflow-y-auto p-4 space-y-4">
       <!-- Basic Info -->
       <div class="grid grid-cols-2 gap-4">
           <div class="space-y-2">
               <Label>Code (ID)</Label>
               <Input v-model="formData.code" :disabled="!canEditField('code')" placeholder="e.g. employeeNumber" />
           </div>
           
           <div class="space-y-2">
               <Label>Display Name</Label>
               <Input v-model="formData.displayName" :disabled="!canEditField('displayName')" placeholder="e.g. Employee ID" />
           </div>
       </div>

        <div class="grid grid-cols-2 gap-4">
             <div class="space-y-2">
                <Label>Target Domain</Label>
                <Select v-model="formData.targetDomain" :disabled="!canEditField('targetDomain')">
                  <SelectTrigger>
                    <SelectValue placeholder="Select domain" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="USER">User</SelectItem>
                    <SelectItem value="DEPARTMENT">Department</SelectItem>
                    <SelectItem value="GROUP">Group</SelectItem>
                  </SelectContent>
                </Select>
            </div>
 
            <div class="space-y-2">
                <Label>Category</Label>
                <Select v-model="formData.category" :disabled="!canEditField('category')">
                  <SelectTrigger>
                    <SelectValue placeholder="Select category" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="EXTENSION">Extension</SelectItem>
                    <SelectItem value="CORE" disabled>Core (System Only)</SelectItem>
                  </SelectContent>
                </Select>
            </div>
        </div>

       <div class="grid grid-cols-2 gap-4">
           <div class="space-y-2">
               <Label>Data Type</Label>
               <Select v-model="formData.dataType" :disabled="!canEditField('dataType')">
                  <SelectTrigger>
                    <SelectValue placeholder="Select data type" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="STRING">String</SelectItem>
                    <SelectItem value="NUMBER">Number</SelectItem>
                    <SelectItem value="BOOLEAN">Boolean</SelectItem>
                    <SelectItem value="DATE">Date</SelectItem>
                    <SelectItem value="CODE">Code</SelectItem>
                  </SelectContent>
               </Select>
           </div>
           
           <div class="space-y-2">
                <Label>UI Component</Label>
                <Select v-model="formData.uiComponent" :disabled="!canEditField('uiComponent')">
                   <SelectTrigger>
                     <SelectValue placeholder="Select UI component" />
                   </SelectTrigger>
                   <SelectContent>
                     <SelectItem value="text-input">Text Input</SelectItem>
                     <SelectItem value="number-input">Number Input</SelectItem>
                     <SelectItem value="checkbox">Checkbox</SelectItem>
                     <SelectItem value="datepicker">Date Picker</SelectItem>
                     <SelectItem value="select">Select</SelectItem>
                   </SelectContent>
                </Select>
           </div>
       </div>

       <div class="space-y-2">
           <label class="text-sm font-medium leading-none peer-disabled:cursor-not-allowed peer-disabled:opacity-70">Description</label>
           <Input v-model="formData.description" :disabled="!canEditField('description')" placeholder="Short description of this attribute" />
       </div>

       <!-- SCIM Schema URI (Only for Extensions) -->
       <div v-if="formData.category === 'EXTENSION'" class="space-y-2">
           <Label>SCIM Schema URI</Label>
           <Input v-model="formData.scimSchemaUri" :disabled="!canEditField('scimSchemaUri')" placeholder="e.g. urn:ietf:params:scim:schemas:extension:custom:2.0:User" />
           <p class="text-[10px] text-neutral-400">URI defining the namespace for this extension attribute.</p>
       </div>
       <!-- Flags -->
       <div class="grid grid-cols-2 gap-4 bg-neutral-50 p-2 rounded">
           <div class="flex items-center space-x-2">
               <Checkbox id="required" :checked="formData.required" @update:checked="(v: boolean) => formData.required = v" :disabled="!canEditField('required')" />
               <Label for="required">Required</Label>
           </div>
           <div class="flex items-center space-x-2">
               <Checkbox id="encrypted" :checked="formData.encrypted" @update:checked="(v: boolean) => formData.encrypted = v" :disabled="!canEditField('encrypted')" />
               <Label for="encrypted">Encrypted Storage</Label>
           </div>
            <div class="flex items-center space-x-2">
               <Checkbox id="adminOnly" :checked="formData.adminOnly" @update:checked="(v: boolean) => formData.adminOnly = v" :disabled="!canEditField('adminOnly')" />
               <Label for="adminOnly">Admin Only</Label>
           </div>
       </div>
    </div>
    
    <div v-if="error" class="text-red-500 text-xs px-4">{{ error }}</div>

    <div v-if="isEditMode" class="flex justify-end gap-2 p-4 border-t border-neutral-100 bg-neutral-50">
        <Button variant="outline" @click="cancelEdit" :disabled="isSaving">Cancel</Button>
        <Button @click="onSubmit" :disabled="isSaving" class="bg-blue-600 hover:bg-blue-700 text-white">
            {{ isSaving ? 'Saving...' : (isCreate ? 'Create Attribute' : 'Save Changes') }}
        </Button>
    </div>
  </div>
</template>
