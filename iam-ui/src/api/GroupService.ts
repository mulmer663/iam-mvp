import { request } from './client'

export interface Group {
    id: string
    externalId?: string
    displayName: string
    members?: Array<{ value: string; display?: string; type?: string }>
    meta?: { resourceType: string; created: string; lastModified: string }
    [key: string]: any
}

interface ScimListResponse {
    totalResults: number
    Resources: Group[]
}

const GROUP_SCHEMA = 'urn:ietf:params:scim:schemas:core:2.0:Group'

export const GroupService = {
    async getGroups(): Promise<Group[]> {
        const res = await request<ScimListResponse>('/scim/v2/Groups')
        return res.Resources ?? []
    },

    async getGroup(id: string): Promise<Group> {
        return request<Group>(`/scim/v2/Groups/${encodeURIComponent(id)}`)
    },

    async createGroup(payload: Record<string, any>): Promise<Group> {
        const body = { schemas: [GROUP_SCHEMA], ...payload }
        return request<Group>('/scim/v2/Groups', {
            method: 'POST',
            body: JSON.stringify(body)
        })
    },

    async updateGroup(id: string, payload: Record<string, any>): Promise<Group> {
        const body = { schemas: [GROUP_SCHEMA], ...payload }
        return request<Group>(`/scim/v2/Groups/${encodeURIComponent(id)}`, {
            method: 'PUT',
            body: JSON.stringify(body)
        })
    },

    async deleteGroup(id: string): Promise<void> {
        await request<void>(`/scim/v2/Groups/${encodeURIComponent(id)}`, { method: 'DELETE' })
    }
}
