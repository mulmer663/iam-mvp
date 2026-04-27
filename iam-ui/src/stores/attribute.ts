import {defineStore} from 'pinia'
import axios from 'axios'
import type {AttributeTargetDomain, IamAttributeMeta} from '@/types/attribute'

export const useAttributeStore = defineStore('attribute', {
    state: () => ({
        attributes: [] as IamAttributeMeta[],
        userAttributes: [] as IamAttributeMeta[],
        deptAttributes: [] as IamAttributeMeta[],
        groupAttributes: [] as IamAttributeMeta[],
        loading: false,
        error: null as string | null
    }),

    getters: {
        getAttributeByCode: (state) => (name: string) => {
            return state.attributes.find(attr => attr.name === name)
        },
        coreAttributes: (state) => {
            return state.attributes.filter(attr => attr.category === 'CORE')
        },
        extensionAttributes: (state) => {
            return state.attributes.filter(attr => attr.category === 'EXTENSION')
        }
    },

    actions: {
        async fetchAttributes(domain?: AttributeTargetDomain) {
            this.loading = true
            this.error = null
            try {
                const url = domain ? `/api/attributes?domain=${domain}` : '/api/attributes'
                const response = await axios.get<IamAttributeMeta[]>(url)

                if (domain === 'USER') {
                    this.userAttributes = response.data
                } else if (domain === 'DEPARTMENT') {
                    this.deptAttributes = response.data
                } else if (domain === 'GROUP') {
                    this.groupAttributes = response.data
                }

                // If fetching all or merging, update main list
                // Simple strategy: Replace main list if no domain specified, or merge if needed.
                // For now, let's keep it simple and just update the specific lists + all.
                if (!domain) {
                    this.attributes = response.data
                    this.userAttributes = this.attributes.filter(a => a.targetDomain === 'USER')
                    this.deptAttributes = this.attributes.filter(a => a.targetDomain === 'DEPARTMENT')
                    this.groupAttributes = this.attributes.filter(a => a.targetDomain === 'GROUP')
                } else {
                    // Update specific list
                    if (domain === 'USER') this.userAttributes = response.data
                    if (domain === 'DEPARTMENT') this.deptAttributes = response.data
                    if (domain === 'GROUP') this.groupAttributes = response.data

                    // Rebuild main list from parts (this might be slightly inefficient but safe)
                    // Or just append/replace
                    const otherAttributes = this.attributes.filter(a => a.targetDomain !== domain)
                    this.attributes = [...otherAttributes, ...response.data]
                }

            } catch (err: any) {
                console.error('Failed to fetch attributes:', err)
                this.error = err.message || 'Failed to fetch attributes'
            } finally {
                this.loading = false
            }
        },

        async fetchUserAttributes() {
            return this.fetchAttributes('USER')
        },

        async fetchDeptAttributes() {
            return this.fetchAttributes('DEPARTMENT')
        },
        async fetchGroupAttributes() {
            return this.fetchAttributes('GROUP')
        },

        async createAttribute(attribute: IamAttributeMeta) {
            try {
                const response = await axios.post<IamAttributeMeta>('/api/attributes', attribute)
                this.attributes.push(response.data)
                // Refresh tailored lists
                if (response.data.targetDomain === 'USER') this.userAttributes.push(response.data)
                if (response.data.targetDomain === 'DEPARTMENT') this.deptAttributes.push(response.data)
                if (response.data.targetDomain === 'GROUP') this.groupAttributes.push(response.data)
                return response.data
            } catch (err: any) {
                console.error('Failed to create attribute:', err)
                throw err
            }
        },

        async updateAttribute(attribute: IamAttributeMeta) {
            try {
                // Backend PK is composite (name, targetDomain) so the path mirrors that.
                const response = await axios.put<IamAttributeMeta>(
                    `/api/attributes/${attribute.targetDomain}/${attribute.name}`,
                    attribute
                )

                // Update in local state
                const index = this.attributes.findIndex(a => a.name === attribute.name)
                if (index !== -1) {
                    this.attributes[index] = response.data
                }

                // Update specific lists
                if (attribute.targetDomain === 'USER') {
                    const idx = this.userAttributes.findIndex(a => a.name === attribute.name)
                    if (idx !== -1) this.userAttributes[idx] = response.data
                } else if (attribute.targetDomain === 'DEPARTMENT') {
                    const idx = this.deptAttributes.findIndex(a => a.name === attribute.name)
                    if (idx !== -1) this.deptAttributes[idx] = response.data
                } else if (attribute.targetDomain === 'GROUP') {
                    const idx = this.groupAttributes.findIndex(a => a.name === attribute.name)
                    if (idx !== -1) this.groupAttributes[idx] = response.data
                }

                return response.data
            } catch (err: any) {
                console.error('Failed to update attribute:', err)
                throw err
            }
        },

        async deleteAttribute(name: string, targetDomain: AttributeTargetDomain) {
            try {
                await axios.delete(`/api/attributes/${targetDomain}/${name}`)

                const matches = (a: IamAttributeMeta) => !(a.name === name && a.targetDomain === targetDomain)
                this.attributes = this.attributes.filter(matches)
                this.userAttributes = this.userAttributes.filter(matches)
                this.deptAttributes = this.deptAttributes.filter(matches)
                this.groupAttributes = this.groupAttributes.filter(matches)
            } catch (err: any) {
                console.error('Failed to delete attribute:', err)
                throw err
            }
        }
    }
})
