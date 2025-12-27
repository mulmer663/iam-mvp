export interface Department {
    id: string
    name: string
    parentId: string | null
}

export interface User {
    id: string
    externalId?: string
    userName: string
    name: {
        familyName: string
        givenName: string
        formatted?: string
    }
    title?: string
    active: boolean
    emails: Array<{ value: string; primary: boolean }>
    'urn:ietf:params:scim:schemas:extension:enterprise:2.0:User'?: {
        employeeNumber?: string
        department?: string
        costCenter?: string
        organization?: string
        division?: string
    }
    meta?: {
        resourceType: string
        created: string
        lastModified: string
        location: string
    }
    [key: string]: any
}

export interface AttributeMapping {
    fromLabel: string
    toLabel: string
    fromField: string
    toField: string
    value: string
}

export interface HistoryLog {
    id: string
    traceId: string
    type: 'HR_SYNC' | 'AD_PROVISION' | 'USER_UPDATE'
    status: 'SUCCESS' | 'PENDING' | 'FAILURE'
    target: string
    time: string
    userId?: string
    syncType?: 'JOIN' | 'REJOIN' | 'UPDATE_SIMPLE' | 'UPDATE_CRITICAL' | 'LEAVE'
    payload?: Record<string, any>
    changes?: Array<{ field: string, old: string, new: string }>
    mappings?: AttributeMapping[]
    snapshot?: {
        layer: 'HR' | 'IAM' | 'AD'
        data: Record<string, any>
    }
}

export interface MillerPane {
    id: string
    type: string
    title: string
    data: any
    width?: string
    maxWidth?: string
}
