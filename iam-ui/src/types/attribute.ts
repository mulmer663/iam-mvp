export type AttributeCategory = 'CORE' | 'EXTENSION'
export type AttributeDataType = 'STRING' | 'NUMBER' | 'BOOLEAN' | 'DATE' | 'CODE' | 'COMPLEX'
export type AttributeMutability = 'READ_ONLY' | 'READ_WRITE' | 'WRITE_ONCE' | 'IMMUTABLE'
export type AttributeTargetDomain = 'USER' | 'DEPARTMENT' | 'GROUP'

export interface IamAttributeMeta {
    name: string
    targetDomain: AttributeTargetDomain
    category: AttributeCategory
    displayName: string
    type: AttributeDataType
    multiValued: boolean
    parentName?: string
    scimSchemaUri?: string
    description?: string
    required: boolean
    mutability: AttributeMutability
    adminOnly: boolean
    viewLevel: number
    editLevel: number
    encrypted: boolean
    uiComponent?: string
    display?: boolean
    canonicalValues?: string[]
    referenceTypes?: string[]
    caseExact?: boolean
}
