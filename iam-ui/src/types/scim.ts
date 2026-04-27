export interface ScimResourceTypeDto {
    id: string;
    name: string;
    description: string;
    endpoint: string;
    schema: string;
    schemaExtensions: Array<{
        schema: string;
        required: boolean;
        name?: string;
        description?: string;
    }>;
}

export interface ScimSchemaAttributeDto {
    name: string;
    type: string;
    multiValued: boolean;
    description: string;
    required: boolean;
    mutability: string;
    returned: string;
    uniqueness: string;
    caseExact: boolean;
    canonicalValues: string[];
    referenceTypes: string[];
    subAttributes: ScimSchemaAttributeDto[] | null;
}

export interface ScimSchemaDto {
    id: string;
    name: string;
    description: string;
    attributes: ScimSchemaAttributeDto[];
}

export type SchemaCategory = 'core' | 'extension';

export function getSchemaCategory(id: string): SchemaCategory {
    return id.includes(':extension:') ? 'extension' : 'core';
}

export function isStandardSchema(id: string): boolean {
    return id.startsWith('urn:ietf:params:scim:schemas:core:2.0:') ||
        id.startsWith('urn:ietf:params:scim:schemas:extension:enterprise:2.0:');
}

export function shortenUrn(urn: string): string {
    const prefix = 'urn:ietf:params:scim:schemas:';
    return urn.startsWith(prefix) ? urn.substring(prefix.length) : urn;
}
