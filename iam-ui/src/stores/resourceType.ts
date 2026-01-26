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
        async fetchSchemas() {
            try {
                const response = await axios.get<ScimSchemaDto[]>('/api/schemas')
                this.schemas = response.data
            } catch (err: any) {
                console.error('Failed to fetch schemas:', err)
            }
        },

        async fetchResourceTypes() {
            this.loading = true
            this.error = null
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
            } finally {
                this.loading = false
            }
        },

        async createResourceType(resourceType: ScimResourceTypeDto) {
            try {
                const response = await axios.post<any>('/api/resource-types', resourceType)
                const normalized = {
                    ...response.data,
                    schemaExtensions: (response.data.schemaExtensions || []).map((ext: any) =>
                        typeof ext === 'string' ? { schema: ext, required: false } : ext
                    )
                }
                this.resourceTypes.push(normalized)
                return normalized
            } catch (err: any) {
                console.error('Failed to create resource type:', err)
                throw err
            }
        },

        async updateResourceType(resourceType: ScimResourceTypeDto) {
            try {
                const response = await axios.put<any>(`/api/resource-types/${resourceType.id}`, resourceType)
                const normalized = {
                    ...response.data,
                    schemaExtensions: (response.data.schemaExtensions || []).map((ext: any) =>
                        typeof ext === 'string' ? { schema: ext, required: false } : ext
                    )
                }
                const index = this.resourceTypes.findIndex(r => r.id === resourceType.id)
                if (index !== -1) {
                    this.resourceTypes[index] = normalized
                }
                return normalized
            } catch (err: any) {
                console.error('Failed to update resource type:', err)
                throw err
            }
        },

        async deleteResourceType(id: string) {
            try {
                await axios.delete(`/api/resource-types/${id}`)
                this.resourceTypes = this.resourceTypes.filter(r => r.id !== id)
            } catch (err: any) {
                console.error('Failed to delete resource type:', err)
                throw err
            }
        }
    }
})
