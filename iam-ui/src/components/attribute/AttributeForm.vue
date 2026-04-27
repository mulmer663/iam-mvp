<script setup lang="ts">
import {computed, ref, watch} from 'vue'
import {Button} from '@/components/ui/button'
import {Input} from '@/components/ui/input'
import {Label} from '@/components/ui/label'
import {Checkbox} from '@/components/ui/checkbox'
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue,} from '@/components/ui/select'
import {Plus, Settings} from 'lucide-vue-next'
import type {IamAttributeMeta} from '@/types/attribute'
import {useAttributeStore} from '@/stores/attribute'
import {useMillerStore} from '@/stores/miller'

const props = defineProps<{
  initialData?: IamAttributeMeta | null
  defaultSchemaUri?: string
  defaultTargetDomain?: string
  parentAttributeName?: string
  paneIndex?: number
}>()

const emit = defineEmits(['save', 'cancel'])
const store = useAttributeStore()
const millerStore = useMillerStore()
const isEditMode = ref(false)
const isCreate = computed(() => !props.initialData)

// Sub-attributes Logic
const subAttributes = computed(() => {
    if (!formData.value.name) return []
    return store.attributes.filter(a => a.parentName === formData.value.name)
})

function openSubAttribute(attr: IamAttributeMeta | null) {
    const paneId = attr ? `attr-${attr.name}` : `create-sub-${formData.value.name}-${Date.now()}`
    
    // Check if pane exists
    const existingPane = millerStore.panes.find(p => p.id === paneId)
    if (existingPane) {
        millerStore.activePaneId = paneId
        return
    }

    const pane = {
        id: paneId,
        type: 'AttributeFormPane',
        title: attr && attr.name ? `Attribute: ${attr.displayName}` : 'New Sub-Attribute',
        data: { 
            initialData: attr, 
            defaultSchemaUri: props.defaultSchemaUri, 
            parentAttributeName: formData.value.name, // Pass current attribute as parent context
            paneIndex: (props.paneIndex ?? 0) + 1 
        },
        width: '500px'
    }
    
    if (typeof props.paneIndex === 'number') {
        millerStore.setPane(props.paneIndex + 1, pane)
    } else {
        millerStore.pushPane(pane)
    }
}

const isLocked = computed(() => {
    // We allow "Edit" for everyone to at least change display names/descriptions.
    // Structural changes will be blocked at the field level in canEditField.
    return false
})

// Initialize
watch(() => props.initialData, (val) => {
    isEditMode.value = !val // If new (no data), edit mode is true. If existing, false (view mode).
}, { immediate: true })

// Form State
const formData = ref<IamAttributeMeta>({
  name: '',
  targetDomain: 'USER', // TODO: Infer from Schema?
  category: 'EXTENSION', 
  scimSchemaUri: props.defaultSchemaUri || 'urn:ietf:params:scim:schemas:extension:enterprise:2.0:User',
  displayName: '',
  type: 'STRING',
  multiValued: false,
  parentName: props.parentAttributeName || '',
  description: '',
  required: false,
  mutability: 'READ_WRITE',
  adminOnly: false,
  viewLevel: 0,
  editLevel: 0,
  encrypted: false,
  uiComponent: 'text-input',
  display: true
})

// Validation computed property
const isValidComplexType = computed(() => {
    return formData.value.type === 'COMPLEX' || formData.value.type === 'STRING' // Allow applying preset even if default is STRING
})
const presets = [
    { label: 'ScimMultiValue (value, type, primary)', value: 'ScimMultiValue' },
    { label: 'ScimAddress (formatted, street, locality...)', value: 'ScimAddress' }
]
const selectedPreset = ref('')

function applyPreset() {
    if (selectedPreset.value === 'ScimMultiValue') {
        alert('Preset applied: Please create sub-attributes [value, display, type, primary] manually after saving.')
        // In a real implementation we could auto-create sub-attributes, but for now we just guide the user.
        // Or better, we can auto-create them if the backend supports batch creation.
        // Let's stick to the plan: Just help user configure the parent.
        formData.value.type = 'COMPLEX'
        formData.value.multiValued = true
    } else if (selectedPreset.value === 'ScimAddress') {
        formData.value.type = 'COMPLEX'
        formData.value.multiValued = true
    }
}

// Validation for readonly Mode
function canEditField(field: keyof IamAttributeMeta) {
    if (isCreate.value) return true 
    if (!isEditMode.value) return false 

    // Always immutable fields during update
    if (field === 'name') return false
    if (field === 'scimSchemaUri') return false 
    if (field === 'targetDomain') return false
    if (field === 'category') return false
    if (field as any === 'type') return false

    // If it's a CORE attribute, we strictly limit what can be edited
    if (formData.value.category === 'CORE') {
        const allowedCoreFields: (keyof IamAttributeMeta)[] = ['displayName', 'description', 'uiComponent', 'viewLevel', 'editLevel', 'adminOnly', 'encrypted', 'display']
        return allowedCoreFields.includes(field)
    }

    // For non-CORE (EXTENSION, CUSTOM), we allow more flexibility
    // But we still respect SCIM Mutability if set to READ_ONLY/IMMUTABLE
    if (formData.value.mutability === 'READ_ONLY' && field !== 'displayName' && field !== 'description') return false
    if (formData.value.mutability === 'IMMUTABLE' && field !== 'displayName' && field !== 'description') return false

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
            name: props.parentAttributeName ? `${props.parentAttributeName}.` : '',
            targetDomain: (props.defaultTargetDomain as any) || 'USER',
            category: 'EXTENSION',
            scimSchemaUri: props.defaultSchemaUri || 'urn:ietf:params:scim:schemas:extension:enterprise:2.0:User',
            displayName: '',
            type: 'STRING',
            multiValued: false,
            parentName: props.parentAttributeName || '',
            description: '',
            required: false,
            mutability: 'READ_WRITE',
            adminOnly: false,
            viewLevel: 0,
            editLevel: 0,
            encrypted: false,
            uiComponent: 'text-input',
            display: true
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
            alert('Attribute updated successfully')
            isEditMode.value = false // Return to view mode
        } else {
            await store.createAttribute(formData.value)
            alert('Attribute created successfully')
            emit('save')
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
</script>

<template>
  <div class="h-full flex flex-col bg-white">
    <!-- Header -->
    <div class="h-14 border-b border-neutral-100 flex items-center justify-between px-4 shrink-0 bg-neutral-50/50">
        <div class="font-bold text-base text-neutral-700">
            {{ isCreate ? 'New Attribute' : formData.displayName }}
        </div>
        <div v-if="!isCreate && !isEditMode">
            <Button 
                v-if="!isLocked"
                size="xs" variant="outline" @click="enableEdit" class="h-8 text-xs px-3"
            >
                Edit Attribute
            </Button>
        </div>
    </div>

    <!-- Content -->
    <div class="flex-1 overflow-y-auto p-4 space-y-4">
       <!-- Basic Info -->
       <div class="grid grid-cols-2 gap-4">
           <div class="space-y-2">
               <Label>Name (ID)</Label>
               <Input v-model="formData.name" :disabled="!canEditField('name')" placeholder="e.g. employeeNumber" />
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
 
            <div class="space-y-2">
                <Label>Mutability</Label>
                <Select v-model="formData.mutability" :disabled="!isCreate && formData.category === 'CORE'">
                  <SelectTrigger>
                    <SelectValue placeholder="Select mutability" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="READ_WRITE">Read-Write</SelectItem>
                    <SelectItem value="READ_ONLY">Read-Only</SelectItem>
                    <SelectItem value="WRITE_ONCE">Write-Once</SelectItem>
                    <SelectItem value="IMMUTABLE">Immutable</SelectItem>
                  </SelectContent>
                </Select>
            </div>
        </div>

       <div class="grid grid-cols-2 gap-4">
           <div class="space-y-2">
               <Label>Data Type</Label>
               <Select v-model="formData.type" :disabled="!canEditField('type' as any)">
                  <SelectTrigger>
                    <SelectValue placeholder="Select data type" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="STRING">String</SelectItem>
                    <SelectItem value="NUMBER">Number</SelectItem>
                    <SelectItem value="BOOLEAN">Boolean</SelectItem>
                    <SelectItem value="DATE">Date</SelectItem>
                    <SelectItem value="CODE">Code</SelectItem>
                    <SelectItem value="COMPLEX">Complex</SelectItem>
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

       <!-- Sub Attributes (For Complex Types) -->
       <div v-if="formData.type === 'COMPLEX' && !isCreate" class="space-y-3 pt-4 border-t border-neutral-100">
            <div class="flex items-center justify-between">
                <Label class="text-xs font-bold text-neutral-500 uppercase tracking-wider">Sub-Attributes</Label>
                <Button v-if="formData.category !== 'CORE'" size="xs" variant="outline" class="h-6 text-xs flex gap-1" @click="openSubAttribute(null)">
                    <Plus class="size-3" /> Add Sub-Attribute
                </Button>
            </div>
            
            <div class="border border-neutral-200 rounded-lg overflow-hidden bg-neutral-50/50">
                <div v-if="subAttributes.length === 0" class="p-4 text-center text-xs text-neutral-400 italic">
                    No sub-attributes configured.
                </div>
                <div v-else class="divide-y divide-neutral-100">
                    <div v-for="sub in subAttributes" :key="sub.name" 
                         class="p-2.5 flex items-center justify-between hover:bg-white cursor-pointer group transition-colors"
                         @click="openSubAttribute(sub)"
                    >
                        <div class="flex items-center gap-2">
                            <div class="size-1.5 rounded-full bg-blue-400"></div>
                            <span class="text-xs font-mono font-medium text-neutral-700">{{ sub.name.split('.').pop() }}</span>
                            <span class="text-xs text-neutral-400">({{ sub.type }})</span>
                        </div>
                        <div class="flex items-center gap-2">
                             <span v-if="sub.required" class="text-[10px] text-red-500 font-bold border border-red-200 px-1 rounded">REQ</span>
                             <div class="opacity-0 group-hover:opacity-100 transition-opacity">
                                 <Settings class="size-4 text-neutral-400" />
                             </div>
                        </div>
                    </div>
                </div>
            </div>
       </div>

       <!-- Presets for Complex -->
       <div v-if="isValidComplexType && isCreate" class="p-3 bg-blue-50 rounded-lg space-y-2 border border-blue-100">
            <Label class="text-xs font-bold text-blue-700 uppercase">Apply Preset</Label>
            <div class="flex gap-2">
                <Select v-model="selectedPreset">
                    <SelectTrigger class="h-9 text-xs bg-white">
                        <SelectValue placeholder="Select a preset..." />
                    </SelectTrigger>
                    <SelectContent>
                        <SelectItem v-for="p in presets" :key="p.value" :value="p.value">{{ p.label }}</SelectItem>
                    </SelectContent>
                </Select>
                <Button size="xs" variant="outline" class="h-9 bg-white" @click="applyPreset" :disabled="!selectedPreset">Apply</Button>
            </div>
            <p class="text-xs text-blue-600">Selecting a preset will configure this attribute as Complex & Multi-valued.</p>
       </div>

       <!-- SCIM Schema URI (Only for Extensions) -->
       <div v-if="formData.category === 'EXTENSION'" class="space-y-2">
           <Label>SCIM Schema URI</Label>
           <Input v-model="formData.scimSchemaUri" :disabled="!canEditField('scimSchemaUri')" placeholder="e.g. urn:ietf:params:scim:schemas:extension:custom:2.0:User" />
           <p class="text-xs text-neutral-400">URI defining the namespace for this extension attribute.</p>
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
            <div class="flex items-center space-x-2">
               <Checkbox id="display" :checked="formData.display ?? true" @update:checked="(v: boolean) => formData.display = v" :disabled="!canEditField('display')" />
               <Label for="display" title="Show this attribute in User Create/Edit forms">Display in Form</Label>
           </div>
       </div>
    </div>
    
    <div v-if="error" class="text-red-500 text-xs px-4">{{ error }}</div>

    <div v-if="isEditMode" class="flex justify-end gap-2 p-4 border-t border-neutral-100 bg-neutral-50">
        <Button variant="outline" @click="cancelEdit" :disabled="isSaving">Cancel</Button>
        <Button @click="onSubmit" :disabled="isSaving || (isLocked && !isCreate)" class="bg-blue-600 hover:bg-blue-700 text-white">
            {{ isSaving ? 'Saving...' : (isCreate ? 'Create Attribute' : 'Save Changes') }}
        </Button>
    </div>
  </div>
</template>
