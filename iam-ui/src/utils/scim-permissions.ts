/**
 * SCIM 편집 가능성 매트릭스 — RFC 7643/7644 + 프로젝트 정책 단일 진원지.
 *
 * 모든 UI 가드는 이 모듈의 헬퍼만 사용해야 합니다.
 * (직접 isStandardRt(rt.id === 'User'...) 같은 inline 체크 금지)
 *
 * 상세 정책: .agent/rules/scim-permissions-guide.md
 */

import type { IamAttributeMeta } from '@/types/attribute'
import type { ScimResourceTypeDto, ScimSchemaDto } from '@/types/scim'

// ─────────────────────────────────────────────────────────────────────────────
// 표준(RFC) 식별자
// ─────────────────────────────────────────────────────────────────────────────

/** RFC 7643이 정의한 ResourceType IDs (기본 컨트롤러 보유) */
export const RFC_RESOURCE_TYPE_IDS = new Set(['User', 'Group'])

/** RFC 7643/7644가 정의한 Schema URI prefix */
const RFC_SCHEMA_URI_PREFIXES = [
    'urn:ietf:params:scim:schemas:core:2.0:',
    'urn:ietf:params:scim:schemas:extension:enterprise:2.0:'
]

export function isStandardSchema(uri: string | undefined): boolean {
    if (!uri) return false
    return RFC_SCHEMA_URI_PREFIXES.some(p => uri.startsWith(p))
}

export function isStandardResourceType(id: string | undefined): boolean {
    return !!id && RFC_RESOURCE_TYPE_IDS.has(id)
}

/** RFC 표준 스키마에 정의된 속성인가? (= 해당 속성의 scimSchemaUri가 표준 스키마) */
export function isStandardAttribute(attr: IamAttributeMeta): boolean {
    return isStandardSchema(attr.scimSchemaUri)
}

// ─────────────────────────────────────────────────────────────────────────────
// Capabilities — UI 가드용 단일 객체
// ─────────────────────────────────────────────────────────────────────────────

export interface SchemaCapabilities {
    /** 이름/설명 등 스키마 메타 수정 */
    canEditMeta: boolean
    /** 스키마 자체 삭제 */
    canDelete: boolean
    /** 스키마에 속성 추가 */
    canAddAttribute: boolean
    /** 스키마의 속성 삭제 */
    canRemoveAttribute: boolean
}

export interface ResourceTypeCapabilities {
    /** name/description/endpoint 수정 */
    canEditMeta: boolean
    /** ResourceType 삭제 */
    canDelete: boolean
    /** schemaExtensions 추가/제거 (RFC 표준 RT에도 허용 — RFC §3.3) */
    canEditExtensions: boolean
}

export interface AttributeCapabilities {
    /** 속성 메타 수정 */
    canEdit: boolean
    /** 속성 삭제 */
    canDelete: boolean
}

export function schemaCapabilities(schema: ScimSchemaDto | { id: string } | string): SchemaCapabilities {
    const id = typeof schema === 'string' ? schema : schema.id
    const standard = isStandardSchema(id)
    return {
        canEditMeta: !standard,
        canDelete: !standard,
        canAddAttribute: !standard,
        canRemoveAttribute: !standard
    }
}

export function resourceTypeCapabilities(rt: ScimResourceTypeDto | { id: string } | string): ResourceTypeCapabilities {
    const id = typeof rt === 'string' ? rt : rt.id
    const standard = isStandardResourceType(id)
    return {
        canEditMeta: !standard,
        canDelete: !standard,
        // RFC §3.3 — 표준 RT에도 schemaExtensions 부착은 SCIM의 본래 의도
        canEditExtensions: true
    }
}

export function attributeCapabilities(attr: IamAttributeMeta): AttributeCapabilities {
    const standard = isStandardAttribute(attr)
    return {
        canEdit: !standard,
        canDelete: !standard
    }
}
