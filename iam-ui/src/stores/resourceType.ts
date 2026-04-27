import {defineStore} from 'pinia'
import axios from 'axios'
import type {ScimResourceTypeDto, ScimSchemaDto} from '@/types/scim'

export const useResourceTypeStore = defineStore('resourceType', {
    state: () => ({
        resourceTypes: [] as ScimResourceTypeDto[],
        schemas: [] as ScimSchemaDto[],
        loading: false,
        error: null as string | null
    }),

    actions: {
        // ── Schema CRUD ──────────────────────────────────────────────────────
        async fetchSchemas() {
            this.loading = true
            try {
                const response = await axios.get<ScimSchemaDto[]>('/api/schemas')
                this.schemas = response.data
            } catch (err: any) {
                console.error('Failed to fetch schemas:', err)
            } finally {
                this.loading = false
            }
        },

        async createSchema(dto: Pick<ScimSchemaDto, 'id' | 'name' | 'description'>) {
            const response = await axios.post<ScimSchemaDto>('/api/schemas', { ...dto, attributes: [] })
            this.schemas.push(response.data)
            return response.data
        },

        async updateSchema(uri: string, dto: Pick<ScimSchemaDto, 'name' | 'description'>) {
            const existing = this.schemas.find(s => s.id === uri)!
            const response = await axios.put<ScimSchemaDto>(`/api/schemas/${encodeURIComponent(uri)}`, {
                ...existing,
                ...dto
            })
            const idx = this.schemas.findIndex(s => s.id === uri)
            if (idx !== -1) this.schemas[idx] = response.data
            return response.data
        },

        async deleteSchema(uri: string) {
            await axios.delete(`/api/schemas/${encodeURIComponent(uri)}`)
            this.schemas = this.schemas.filter(s => s.id !== uri)
        },

        // ── ResourceType CRUD ─────────────────────────────────────────────────
        async fetchResourceTypes() {
            try {
                const response = await axios.get<any[]>('/api/resource-types')
                this.resourceTypes = response.data.map(rt => ({
                    ...rt,
                    schemaExtensions: (rt.schemaExtensions || []).map((ext: any) =>
                        typeof ext === 'string' ? { schema: ext, required: false } : ext
                    )
                }))
            } catch (err: any) {
                console.error('Failed to fetch resource types:', err)
                this.error = err.message || 'Failed to fetch resource types'
            }
        },

        async createResourceType(resourceType: ScimResourceTypeDto) {
            const response = await axios.post<any>('/api/resource-types', resourceType)
            const normalized = normalizeRt(response.data)
            this.resourceTypes.push(normalized)
            return normalized
        },

        async updateResourceType(resourceType: ScimResourceTypeDto) {
            const response = await axios.put<any>(`/api/resource-types/${resourceType.id}`, resourceType)
            const normalized = normalizeRt(response.data)
            const idx = this.resourceTypes.findIndex(r => r.id === resourceType.id)
            if (idx !== -1) this.resourceTypes[idx] = normalized
            return normalized
        },

        async deleteResourceType(id: string) {
            await axios.delete(`/api/resource-types/${id}`)
            this.resourceTypes = this.resourceTypes.filter(r => r.id !== id)
        }
    }
})

function normalizeRt(rt: any): ScimResourceTypeDto {
    return {
        ...rt,
        schemaExtensions: (rt.schemaExtensions || []).map((ext: any) =>
            typeof ext === 'string' ? { schema: ext, required: false } : ext
        )
    }
}
