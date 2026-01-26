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

export interface ScimSchemaDto {
    id: string;
    name: string;
    description: string;
    attributes: any[]; // We can refine this if needed
}
