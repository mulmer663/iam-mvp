-- ============================================================
-- V2: 검색 성능 인덱스 (RFC 7644 필터 지원 대상 컬럼)
-- ============================================================

-- ===== iam_user: 명시적 식별 컬럼 =====

-- externalId: 현재 인덱스 없음 → 조회 버그 수준
CREATE INDEX IF NOT EXISTS idx_iam_user_external_id
    ON iam_user (external_id);

-- active: eq 필터 (active eq true)
CREATE INDEX IF NOT EXISTS idx_iam_user_active
    ON iam_user (active);

-- displayName: eq/co/sw 필터 (case-insensitive)
CREATE INDEX IF NOT EXISTS idx_iam_user_display_name
    ON iam_user (LOWER(display_name));

-- userName은 이미 UNIQUE 제약으로 인덱스 생성됨. case-insensitive 검색용 추가.
CREATE INDEX IF NOT EXISTS idx_iam_user_name_lower
    ON iam_user (LOWER(user_name));

-- ===== scim_dynamic_resource: JSONB 핵심 속성 (B-tree functional index) =====

-- resource_type + displayName 복합 인덱스 (eq/co/sw, 가장 빈번한 검색)
CREATE INDEX IF NOT EXISTS idx_scim_res_type_display
    ON scim_dynamic_resource (resource_type, (attributes->>'displayName'));

-- resource_type + active 상태 필터
CREATE INDEX IF NOT EXISTS idx_scim_res_type_active
    ON scim_dynamic_resource (resource_type, (attributes->>'active'));

-- resource_type + externalId 복합 (external_id 컬럼은 이미 idx_scim_res_ext_id 존재,
-- attributes JSONB 안에 중복 저장되는 경우 대비)
CREATE INDEX IF NOT EXISTS idx_scim_res_type_ext_id
    ON scim_dynamic_resource (resource_type, external_id);

-- ===== 기존 누락 인덱스 보완 =====

-- iam_identity_link: system_type + external_id (이미 @Index 선언이나 DDL에 없었음)
CREATE INDEX IF NOT EXISTS idx_link_external
    ON iam_identity_link (system_type, external_id);

-- iam_sync_history: trace_id, target_user (이미 @Index 선언)
CREATE INDEX IF NOT EXISTS idx_sync_trace
    ON iam_sync_history (trace_id);

CREATE INDEX IF NOT EXISTS idx_sync_target
    ON iam_sync_history (target_user);

-- iam_sync_transform_failure: history_id, field_name
CREATE INDEX IF NOT EXISTS idx_transform_history
    ON iam_sync_transform_failure (history_id);

CREATE INDEX IF NOT EXISTS idx_transform_field
    ON iam_sync_transform_failure (field_name);
