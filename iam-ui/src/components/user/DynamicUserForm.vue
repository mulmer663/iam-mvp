<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Checkbox } from '@/components/ui/checkbox'
import { Button } from '@/components/ui/button'
import {
    Select, SelectContent, SelectItem, SelectTrigger, SelectValue
} from '@/components/ui/select'
import { Plus, Trash2, Lock, ChevronDown, ChevronRight as ChevronRightIcon } from 'lucide-vue-next'
import type { IamAttributeMeta } from '@/types/attribute'

interface Props {
    modelValue: Record<string, any>
    attributes: IamAttributeMeta[]
    mode: 'create' | 'edit'
}

const props = defineProps<Props>()
const emit = defineEmits<{
    (e: 'update:modelValue', val: Record<string, any>): void
}>()

// Local form state — keep deep clone to avoid mutating parent prop
const form = ref<Record<string, any>>(JSON.parse(JSON.stringify(props.modelValue ?? {})))

watch(() => props.modelValue, (val) => {
    form.value = JSON.parse(JSON.stringify(val ?? {}))
}, { deep: true })

watch(form, (val) => {
    emit('update:modelValue', val)
}, { deep: true })

// ── Attribute classification ───────────────────────────────────────────────
// display === false hides the attribute from the dynamic form (sub-attrs follow
// their parent's visibility — they are still rendered nested under a visible parent).
const visible = (a: IamAttributeMeta) => a.display !== false

const coreAttrs = computed(() =>
    props.attributes.filter(a => a.category === 'CORE' && !a.parentName && visible(a))
)
const extensionsBySchema = computed(() => {
    const map = new Map<string, IamAttributeMeta[]>()
    for (const a of props.attributes) {
        if (a.category !== 'EXTENSION' || a.parentName) continue
        if (!visible(a)) continue
        const uri = a.scimSchemaUri || 'urn:unknown'
        if (!map.has(uri)) map.set(uri, [])
        map.get(uri)!.push(a)
    }
    return map
})

function subAttrsOf(parent: IamAttributeMeta): IamAttributeMeta[] {
    return props.attributes.filter(a => a.parentName === parent.name)
}

// ── Mutability/disabled logic ──────────────────────────────────────────────
function isDisabled(attr: IamAttributeMeta): boolean {
    if (attr.mutability === 'READ_ONLY' || attr.mutability === 'IMMUTABLE') return true
    if (attr.mutability === 'WRITE_ONCE' && props.mode === 'edit') return true
    return false
}

// ── Value get/set helpers ──────────────────────────────────────────────────
function getCoreValue(attr: IamAttributeMeta): any {
    return form.value[attr.name]
}
function setCoreValue(attr: IamAttributeMeta, val: any) {
    form.value[attr.name] = val
}

function getExtValue(uri: string, attr: IamAttributeMeta): any {
    return form.value?.[uri]?.[attr.name]
}
function setExtValue(uri: string, attr: IamAttributeMeta, val: any) {
    if (!form.value[uri]) form.value[uri] = {}
    form.value[uri][attr.name] = val
}

// ── COMPLEX (single value) sub-attribute helpers ──────────────────────────
function getComplexSubValue(parent: IamAttributeMeta, sub: IamAttributeMeta, scope: 'core' | string): any {
    if (scope === 'core') {
        return form.value?.[parent.name]?.[sub.name]
    }
    return form.value?.[scope]?.[parent.name]?.[sub.name]
}
function setComplexSubValue(parent: IamAttributeMeta, sub: IamAttributeMeta, scope: 'core' | string, val: any) {
    if (scope === 'core') {
        if (!form.value[parent.name]) form.value[parent.name] = {}
        form.value[parent.name][sub.name] = val
    } else {
        if (!form.value[scope]) form.value[scope] = {}
        if (!form.value[scope][parent.name]) form.value[scope][parent.name] = {}
        form.value[scope][parent.name][sub.name] = val
    }
}

// ── Multi-valued helpers (emails / phoneNumbers …) ────────────────────────
function getMultiArray(parent: IamAttributeMeta, scope: 'core' | string): any[] {
    if (scope === 'core') {
        if (!Array.isArray(form.value[parent.name])) form.value[parent.name] = []
        return form.value[parent.name]
    }
    if (!form.value[scope]) form.value[scope] = {}
    if (!Array.isArray(form.value[scope][parent.name])) form.value[scope][parent.name] = []
    return form.value[scope][parent.name]
}
function addMultiRow(parent: IamAttributeMeta, scope: 'core' | string) {
    const arr = getMultiArray(parent, scope)
    const subs = subAttrsOf(parent)
    const blank: Record<string, any> = {}
    for (const s of subs) blank[s.name] = s.type === 'BOOLEAN' ? false : ''
    arr.push(blank)
}
function removeMultiRow(parent: IamAttributeMeta, scope: 'core' | string, idx: number) {
    const arr = getMultiArray(parent, scope)
    arr.splice(idx, 1)
}

// ── Display helpers ────────────────────────────────────────────────────────
function shortUri(uri: string): string {
    if (!uri) return ''
    const parts = uri.split(':')
    return parts.slice(-3).join(':')
}

function inputType(attr: IamAttributeMeta): string {
    switch (attr.type) {
        case 'NUMBER': return 'number'
        case 'DATE': return 'date'
        default: return 'text'
    }
}

// ── Sub-attribute layout helpers (multi-valued COMPLEX rows) ─────────────
// Strip parent prefix so seed-data names like "emails.value" render as "value".
function subShortName(sub: IamAttributeMeta): string {
    const last = sub.name.split('.').pop() ?? sub.name
    return sub.displayName && sub.displayName !== sub.name ? sub.displayName : last
}

function hasCanonical(sub: IamAttributeMeta): boolean {
    return Array.isArray(sub.canonicalValues) && sub.canonicalValues.length > 0
}

// Returns a CSS column unit for one sub-attribute. Wider for free-form text
// (value/display), narrower for selects/booleans/primary flag.
function subColUnit(sub: IamAttributeMeta): string {
    if (sub.type === 'BOOLEAN') return '90px'
    if (hasCanonical(sub)) return 'minmax(110px, 1.2fr)'
    const last = sub.name.split('.').pop() ?? sub.name
    if (last === 'value') return 'minmax(180px, 3fr)'
    if (last === 'primary') return '90px'
    if (last === 'display') return 'minmax(140px, 2fr)'
    return 'minmax(120px, 1.5fr)'
}

function gridCols(parent: IamAttributeMeta): string {
    const subs = subAttrsOf(parent)
    return subs.map(subColUnit).join(' ') + ' 32px'
}

const expanded = ref<Record<string, boolean>>({})
function toggleSection(key: string) {
    expanded.value[key] = !(expanded.value[key] ?? true)
}
function isExpanded(key: string): boolean {
    return expanded.value[key] ?? true
}
</script>

<template>
    <div class="space-y-4 text-[12px]">

        <!-- ── Core Section ─────────────────────────────────────────────── -->
        <section v-if="coreAttrs.length" class="border border-neutral-100 rounded-lg overflow-hidden">
            <button type="button" class="w-full px-3 py-2 flex items-center justify-between bg-blue-50/40 hover:bg-blue-50/70 transition-colors"
                @click="toggleSection('core')">
                <div class="flex items-center gap-2">
                    <ChevronDown v-if="isExpanded('core')" class="size-3.5 text-blue-500" />
                    <ChevronRightIcon v-else class="size-3.5 text-blue-500" />
                    <span class="text-[10px] font-bold text-blue-600 uppercase tracking-wider">Core Attributes</span>
                </div>
                <span class="text-[10px] text-neutral-400">{{ coreAttrs.length }} fields</span>
            </button>

            <div v-show="isExpanded('core')" class="p-3 space-y-3 bg-white">
                <template v-for="attr in coreAttrs" :key="attr.name">

                    <!-- COMPLEX multi-valued (emails, phoneNumbers …) -->
                    <div v-if="attr.type === 'COMPLEX' && attr.multiValued" class="space-y-1.5">
                        <div class="flex items-center justify-between">
                            <Label class="text-[11px] font-semibold text-neutral-700">
                                {{ attr.displayName || attr.name }}
                                <span v-if="attr.required" class="text-red-500">*</span>
                            </Label>
                            <Button v-if="!isDisabled(attr)" type="button" size="xs" variant="ghost"
                                class="h-5 text-[10px] px-1.5 flex gap-1" @click="addMultiRow(attr, 'core')">
                                <Plus class="size-3" /> Add
                            </Button>
                        </div>
                        <div v-if="!getMultiArray(attr, 'core').length"
                            class="text-[10px] text-neutral-300 italic px-2 py-1.5 border border-dashed border-neutral-200 rounded">
                            No {{ attr.name }} added.
                        </div>
                        <div v-for="(_row, idx) in getMultiArray(attr, 'core')" :key="idx"
                            class="grid gap-2 p-2.5 border border-neutral-100 rounded bg-neutral-50/40 relative items-end"
                            :style="{ gridTemplateColumns: gridCols(attr) }">
                            <div v-for="sub in subAttrsOf(attr)" :key="sub.name" class="min-w-0">
                                <div class="text-[9px] font-bold text-neutral-400 uppercase tracking-tighter mb-1">
                                    {{ subShortName(sub) }}
                                </div>
                                <div v-if="sub.type === 'BOOLEAN'" class="h-8 flex items-center">
                                    <Checkbox
                                        :model-value="!!getMultiArray(attr, 'core')[idx][sub.name]"
                                        @update:model-value="(v: any) => getMultiArray(attr, 'core')[idx][sub.name] = !!v"
                                        :disabled="isDisabled(attr) || isDisabled(sub)" />
                                </div>
                                <Select v-else-if="hasCanonical(sub)"
                                    :model-value="getMultiArray(attr, 'core')[idx][sub.name] ?? ''"
                                    @update:model-value="(v: any) => getMultiArray(attr, 'core')[idx][sub.name] = v"
                                    :disabled="isDisabled(attr) || isDisabled(sub)">
                                    <SelectTrigger class="h-8 text-[11px]">
                                        <SelectValue placeholder="—" />
                                    </SelectTrigger>
                                    <SelectContent>
                                        <SelectItem v-for="cv in (sub.canonicalValues ?? [])" :key="cv" :value="cv">{{ cv }}</SelectItem>
                                    </SelectContent>
                                </Select>
                                <Input v-else
                                    :model-value="getMultiArray(attr, 'core')[idx][sub.name]"
                                    @update:model-value="(v: any) => getMultiArray(attr, 'core')[idx][sub.name] = v"
                                    :type="inputType(sub)"
                                    :disabled="isDisabled(attr) || isDisabled(sub)"
                                    class="h-8 text-[11px]" />
                            </div>
                            <Button v-if="!isDisabled(attr)" type="button" size="icon" variant="ghost"
                                class="size-7 hover:bg-red-50 hover:text-red-500"
                                @click="removeMultiRow(attr, 'core', idx)">
                                <Trash2 class="size-3" />
                            </Button>
                        </div>
                    </div>

                    <!-- COMPLEX single-valued (name) -->
                    <div v-else-if="attr.type === 'COMPLEX'" class="space-y-1.5">
                        <Label class="text-[11px] font-semibold text-neutral-700">
                            {{ attr.displayName || attr.name }}
                            <span v-if="attr.required" class="text-red-500">*</span>
                        </Label>
                        <div class="grid gap-2 p-2 pl-3 border-l-2 border-blue-100 bg-neutral-50/40"
                            :style="{ gridTemplateColumns: `repeat(${Math.min(subAttrsOf(attr).length, 2)}, minmax(0,1fr))` }">
                            <div v-for="sub in subAttrsOf(attr)" :key="sub.name" class="min-w-0">
                                <Label class="text-[10px] text-neutral-500">
                                    {{ sub.displayName || sub.name }}
                                    <span v-if="sub.required" class="text-red-500">*</span>
                                    <Lock v-if="isDisabled(sub)" class="inline size-2.5 text-neutral-300 ml-1" />
                                </Label>
                                <Input
                                    :model-value="getComplexSubValue(attr, sub, 'core')"
                                    @update:model-value="(v: any) => setComplexSubValue(attr, sub, 'core', v)"
                                    :type="inputType(sub)"
                                    :disabled="isDisabled(attr) || isDisabled(sub)"
                                    class="h-7 text-[11px] mt-0.5" />
                            </div>
                        </div>
                    </div>

                    <!-- BOOLEAN -->
                    <div v-else-if="attr.type === 'BOOLEAN'" class="flex items-center gap-2">
                        <Checkbox :id="`core-${attr.name}`"
                            :model-value="getCoreValue(attr) ?? false"
                            @update:model-value="(v: any) => setCoreValue(attr, v)"
                            :disabled="isDisabled(attr)" />
                        <Label :for="`core-${attr.name}`" class="text-[11px] font-medium text-neutral-700">
                            {{ attr.displayName || attr.name }}
                            <span v-if="attr.required" class="text-red-500">*</span>
                            <Lock v-if="isDisabled(attr)" class="inline size-2.5 text-neutral-300 ml-1" />
                        </Label>
                    </div>

                    <!-- Scalar (STRING / NUMBER / DATE / CODE) -->
                    <div v-else class="space-y-1">
                        <Label class="text-[11px] font-semibold text-neutral-700">
                            {{ attr.displayName || attr.name }}
                            <span v-if="attr.required" class="text-red-500">*</span>
                            <Lock v-if="isDisabled(attr)" class="inline size-2.5 text-neutral-300 ml-1" />
                        </Label>
                        <Select v-if="attr.uiComponent === 'select'"
                            :model-value="getCoreValue(attr)"
                            @update:model-value="(v: any) => setCoreValue(attr, v)"
                            :disabled="isDisabled(attr)">
                            <SelectTrigger class="h-7 text-[11px]">
                                <SelectValue :placeholder="`Select ${attr.displayName || attr.name}`" />
                            </SelectTrigger>
                            <SelectContent />
                        </Select>
                        <Input v-else
                            :model-value="getCoreValue(attr)"
                            @update:model-value="(v: any) => setCoreValue(attr, v)"
                            :type="inputType(attr)"
                            :disabled="isDisabled(attr)"
                            :placeholder="attr.description || attr.name"
                            class="h-7 text-[11px]" />
                    </div>
                </template>
            </div>
        </section>

        <!-- ── Extension Sections ───────────────────────────────────────── -->
        <section v-for="[uri, attrs] in extensionsBySchema" :key="uri"
            class="border border-purple-100 rounded-lg overflow-hidden">
            <button type="button" class="w-full px-3 py-2 flex items-center justify-between bg-purple-50/40 hover:bg-purple-50/70 transition-colors"
                @click="toggleSection(uri)">
                <div class="flex items-center gap-2 min-w-0">
                    <ChevronDown v-if="isExpanded(uri)" class="size-3.5 text-purple-500 shrink-0" />
                    <ChevronRightIcon v-else class="size-3.5 text-purple-500 shrink-0" />
                    <span class="text-[10px] font-bold text-purple-600 uppercase tracking-wider truncate">
                        {{ shortUri(uri) }}
                    </span>
                </div>
                <span class="text-[10px] text-neutral-400 shrink-0 ml-2">{{ attrs.length }} fields</span>
            </button>

            <div v-show="isExpanded(uri)" class="p-3 space-y-3 bg-white">
                <template v-for="attr in attrs" :key="attr.name">

                    <div v-if="attr.type === 'COMPLEX' && attr.multiValued" class="space-y-1.5">
                        <div class="flex items-center justify-between">
                            <Label class="text-[11px] font-semibold text-neutral-700">
                                {{ attr.displayName || attr.name }}
                                <span v-if="attr.required" class="text-red-500">*</span>
                            </Label>
                            <Button v-if="!isDisabled(attr)" type="button" size="xs" variant="ghost"
                                class="h-5 text-[10px] px-1.5 flex gap-1" @click="addMultiRow(attr, uri)">
                                <Plus class="size-3" /> Add
                            </Button>
                        </div>
                        <div v-if="!getMultiArray(attr, uri).length"
                            class="text-[10px] text-neutral-300 italic px-2 py-1.5 border border-dashed border-neutral-200 rounded">
                            No {{ attr.name }} added.
                        </div>
                        <div v-for="(_row, idx) in getMultiArray(attr, uri)" :key="idx"
                            class="grid gap-2 p-2.5 border border-neutral-100 rounded bg-neutral-50/40 items-end"
                            :style="{ gridTemplateColumns: gridCols(attr) }">
                            <div v-for="sub in subAttrsOf(attr)" :key="sub.name" class="min-w-0">
                                <div class="text-[9px] font-bold text-neutral-400 uppercase tracking-tighter mb-1">
                                    {{ subShortName(sub) }}
                                </div>
                                <div v-if="sub.type === 'BOOLEAN'" class="h-8 flex items-center">
                                    <Checkbox
                                        :model-value="!!getMultiArray(attr, uri)[idx][sub.name]"
                                        @update:model-value="(v: any) => getMultiArray(attr, uri)[idx][sub.name] = !!v"
                                        :disabled="isDisabled(attr) || isDisabled(sub)" />
                                </div>
                                <Select v-else-if="hasCanonical(sub)"
                                    :model-value="getMultiArray(attr, uri)[idx][sub.name] ?? ''"
                                    @update:model-value="(v: any) => getMultiArray(attr, uri)[idx][sub.name] = v"
                                    :disabled="isDisabled(attr) || isDisabled(sub)">
                                    <SelectTrigger class="h-8 text-[11px]">
                                        <SelectValue placeholder="—" />
                                    </SelectTrigger>
                                    <SelectContent>
                                        <SelectItem v-for="cv in (sub.canonicalValues ?? [])" :key="cv" :value="cv">{{ cv }}</SelectItem>
                                    </SelectContent>
                                </Select>
                                <Input v-else
                                    :model-value="getMultiArray(attr, uri)[idx][sub.name]"
                                    @update:model-value="(v: any) => getMultiArray(attr, uri)[idx][sub.name] = v"
                                    :type="inputType(sub)"
                                    :disabled="isDisabled(attr) || isDisabled(sub)"
                                    class="h-8 text-[11px]" />
                            </div>
                            <Button v-if="!isDisabled(attr)" type="button" size="icon" variant="ghost"
                                class="size-7 hover:bg-red-50 hover:text-red-500"
                                @click="removeMultiRow(attr, uri, idx)">
                                <Trash2 class="size-3" />
                            </Button>
                        </div>
                    </div>

                    <div v-else-if="attr.type === 'COMPLEX'" class="space-y-1.5">
                        <Label class="text-[11px] font-semibold text-neutral-700">
                            {{ attr.displayName || attr.name }}
                            <span v-if="attr.required" class="text-red-500">*</span>
                        </Label>
                        <div class="grid gap-2 p-2 pl-3 border-l-2 border-purple-100 bg-neutral-50/40"
                            :style="{ gridTemplateColumns: `repeat(${Math.min(subAttrsOf(attr).length, 2)}, minmax(0,1fr))` }">
                            <div v-for="sub in subAttrsOf(attr)" :key="sub.name" class="min-w-0">
                                <Label class="text-[10px] text-neutral-500">{{ sub.displayName || sub.name }}</Label>
                                <Input
                                    :model-value="getComplexSubValue(attr, sub, uri)"
                                    @update:model-value="(v: any) => setComplexSubValue(attr, sub, uri, v)"
                                    :type="inputType(sub)"
                                    :disabled="isDisabled(attr) || isDisabled(sub)"
                                    class="h-7 text-[11px] mt-0.5" />
                            </div>
                        </div>
                    </div>

                    <div v-else-if="attr.type === 'BOOLEAN'" class="flex items-center gap-2">
                        <Checkbox :id="`${uri}-${attr.name}`"
                            :model-value="getExtValue(uri, attr) ?? false"
                            @update:model-value="(v: any) => setExtValue(uri, attr, v)"
                            :disabled="isDisabled(attr)" />
                        <Label :for="`${uri}-${attr.name}`" class="text-[11px] font-medium text-neutral-700">
                            {{ attr.displayName || attr.name }}
                            <span v-if="attr.required" class="text-red-500">*</span>
                            <Lock v-if="isDisabled(attr)" class="inline size-2.5 text-neutral-300 ml-1" />
                        </Label>
                    </div>

                    <div v-else class="space-y-1">
                        <Label class="text-[11px] font-semibold text-neutral-700">
                            {{ attr.displayName || attr.name }}
                            <span v-if="attr.required" class="text-red-500">*</span>
                            <Lock v-if="isDisabled(attr)" class="inline size-2.5 text-neutral-300 ml-1" />
                        </Label>
                        <Input
                            :model-value="getExtValue(uri, attr)"
                            @update:model-value="(v: any) => setExtValue(uri, attr, v)"
                            :type="inputType(attr)"
                            :disabled="isDisabled(attr)"
                            :placeholder="attr.description || attr.name"
                            class="h-7 text-[11px]" />
                    </div>
                </template>
            </div>
        </section>

        <div v-if="!coreAttrs.length && !extensionsBySchema.size"
            class="text-center text-neutral-300 italic text-[11px] py-8">
            No attributes available for User schema.
        </div>
    </div>
</template>
