import { request } from './client'
import type { User } from '@/types'

// SCIM Types from Backend (matching ScimListResponse & ScimUserResponse)
interface ScimListResponse<T> {
    schemas: string[]
    totalResults: number
    itemsPerPage: number
    startIndex: number
    Resources: T[]
}

interface ScimUserResponse {
    schemas: string[]
    id: string
    externalId?: string
    userName: string
    name: {
        familyName: string
        givenName: string
        formatted?: string
    }
    title?: string
    emails: { value: string; primary: boolean }[]
    active: boolean
    meta: {
        resourceType: string
        created: string
        lastModified: string
        location: string
    }
    'urn:ietf:params:scim:schemas:extension:enterprise:2.0:User'?: {
        department?: string
        employeeNumber?: string
        costCenter?: string
        organization?: string
        division?: string
    }
}

export const UserService = {
    async getUsers(): Promise<User[]> {
        const response = await request<ScimListResponse<ScimUserResponse>>('/scim/v2/Users')
        return response.Resources.map(this.toUser)
    },

    async getUser(id: string): Promise<User> {
        const response = await request<ScimUserResponse>(`/scim/v2/Users/${id}`)
        return this.toUser(response)
    },

    // Convert Backend SCIM Response to Frontend User Type
    toUser(scim: ScimUserResponse): User {
        return {
            id: scim.id,
            externalId: scim.externalId,
            userName: scim.userName,
            name: scim.name,
            title: scim.title || '',
            active: scim.active,
            emails: scim.emails,
            meta: scim.meta,
            'urn:ietf:params:scim:schemas:extension:enterprise:2.0:User': scim['urn:ietf:params:scim:schemas:extension:enterprise:2.0:User']
        }
    }
}
