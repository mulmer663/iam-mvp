-- ============================================================
-- V1: 초기 스키마 (Hibernate ddl-auto: update가 Envers 감사 테이블 보완)
-- ============================================================

-- ===== Envers 이력 테이블 =====
CREATE TABLE IF NOT EXISTS iam_rev_info (
    id             INTEGER   NOT NULL,
    timestamp      BIGINT    NOT NULL,
    trace_id       VARCHAR(255),
    operator_id    VARCHAR(255),
    operation_type VARCHAR(255),
    created_at     TIMESTAMP,
    PRIMARY KEY (id)
);

-- ===== 사용자 테이블 =====

CREATE TABLE IF NOT EXISTS iam_user (
    user_id            BIGINT       NOT NULL,
    version            BIGINT,
    external_id        VARCHAR(255),
    user_name          VARCHAR(255) NOT NULL,
    family_name        VARCHAR(255),
    given_name         VARCHAR(255),
    formatted_name     VARCHAR(255),
    display_name       VARCHAR(255),
    nick_name          VARCHAR(255),
    profile_url        VARCHAR(255),
    title              VARCHAR(255),
    user_type          VARCHAR(255),
    preferred_language VARCHAR(255),
    locale             VARCHAR(255),
    timezone           VARCHAR(255),
    active             BOOLEAN      NOT NULL DEFAULT FALSE,
    resource_type      VARCHAR(255),
    created            TIMESTAMP,
    last_modified      TIMESTAMP,
    PRIMARY KEY (user_id),
    CONSTRAINT uq_iam_user_name UNIQUE (user_name)
);

CREATE TABLE IF NOT EXISTS iam_user_emails (
    user_id      BIGINT       NOT NULL,
    attr_value   VARCHAR(255),
    attr_type    VARCHAR(255),
    is_primary   BOOLEAN      NOT NULL DEFAULT FALSE,
    attr_display VARCHAR(255),
    attr_ref     VARCHAR(255),
    CONSTRAINT fk_user_emails FOREIGN KEY (user_id) REFERENCES iam_user (user_id)
);

CREATE TABLE IF NOT EXISTS iam_user_phone_numbers (
    user_id      BIGINT       NOT NULL,
    attr_value   VARCHAR(255),
    attr_type    VARCHAR(255),
    is_primary   BOOLEAN      NOT NULL DEFAULT FALSE,
    attr_display VARCHAR(255),
    attr_ref     VARCHAR(255),
    CONSTRAINT fk_user_phones FOREIGN KEY (user_id) REFERENCES iam_user (user_id)
);

CREATE TABLE IF NOT EXISTS iam_user_addresses (
    user_id        BIGINT       NOT NULL,
    street_address VARCHAR(255),
    locality       VARCHAR(255),
    region         VARCHAR(255),
    postal_code    VARCHAR(255),
    country        VARCHAR(255),
    attr_type      VARCHAR(255),
    is_primary     BOOLEAN      NOT NULL DEFAULT FALSE,
    formatted      VARCHAR(255),
    CONSTRAINT fk_user_addresses FOREIGN KEY (user_id) REFERENCES iam_user (user_id)
);

CREATE TABLE IF NOT EXISTS iam_user_ims (
    user_id      BIGINT       NOT NULL,
    attr_value   VARCHAR(255),
    attr_type    VARCHAR(255),
    is_primary   BOOLEAN      NOT NULL DEFAULT FALSE,
    attr_display VARCHAR(255),
    attr_ref     VARCHAR(255),
    CONSTRAINT fk_user_ims FOREIGN KEY (user_id) REFERENCES iam_user (user_id)
);

CREATE TABLE IF NOT EXISTS iam_user_photos (
    user_id      BIGINT       NOT NULL,
    attr_value   VARCHAR(255),
    attr_type    VARCHAR(255),
    is_primary   BOOLEAN      NOT NULL DEFAULT FALSE,
    attr_display VARCHAR(255),
    attr_ref     VARCHAR(255),
    CONSTRAINT fk_user_photos FOREIGN KEY (user_id) REFERENCES iam_user (user_id)
);

CREATE TABLE IF NOT EXISTS iam_user_groups (
    user_id      BIGINT       NOT NULL,
    attr_value   VARCHAR(255),
    attr_type    VARCHAR(255),
    is_primary   BOOLEAN      NOT NULL DEFAULT FALSE,
    attr_display VARCHAR(255),
    attr_ref     VARCHAR(255),
    CONSTRAINT fk_user_groups FOREIGN KEY (user_id) REFERENCES iam_user (user_id)
);

CREATE TABLE IF NOT EXISTS iam_user_entitlements (
    user_id      BIGINT       NOT NULL,
    attr_value   VARCHAR(255),
    attr_type    VARCHAR(255),
    is_primary   BOOLEAN      NOT NULL DEFAULT FALSE,
    attr_display VARCHAR(255),
    attr_ref     VARCHAR(255),
    CONSTRAINT fk_user_entitlements FOREIGN KEY (user_id) REFERENCES iam_user (user_id)
);

CREATE TABLE IF NOT EXISTS iam_user_roles (
    user_id      BIGINT       NOT NULL,
    attr_value   VARCHAR(255),
    attr_type    VARCHAR(255),
    is_primary   BOOLEAN      NOT NULL DEFAULT FALSE,
    attr_display VARCHAR(255),
    attr_ref     VARCHAR(255),
    CONSTRAINT fk_user_roles FOREIGN KEY (user_id) REFERENCES iam_user (user_id)
);

CREATE TABLE IF NOT EXISTS iam_user_x509_certificates (
    user_id      BIGINT       NOT NULL,
    attr_value   VARCHAR(255),
    attr_type    VARCHAR(255),
    is_primary   BOOLEAN      NOT NULL DEFAULT FALSE,
    attr_display VARCHAR(255),
    attr_ref     VARCHAR(255),
    CONSTRAINT fk_user_x509 FOREIGN KEY (user_id) REFERENCES iam_user (user_id)
);

CREATE TABLE IF NOT EXISTS iam_user_extension (
    user_id    BIGINT NOT NULL,
    extensions JSONB,
    PRIMARY KEY (user_id),
    CONSTRAINT fk_user_ext FOREIGN KEY (user_id) REFERENCES iam_user (user_id)
);

CREATE TABLE IF NOT EXISTS iam_user_schema (
    user_id    BIGINT       NOT NULL,
    schema_uri VARCHAR(255),
    CONSTRAINT fk_user_schema FOREIGN KEY (user_id) REFERENCES iam_user (user_id)
);

CREATE TABLE IF NOT EXISTS iam_identity_link (
    id          BIGINT       NOT NULL,
    iam_user_id BIGINT       NOT NULL,
    system_type VARCHAR(255) NOT NULL,
    external_id VARCHAR(255) NOT NULL,
    is_active   BOOLEAN      NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id)
);

-- ===== SCIM 메타 테이블 =====

CREATE TABLE IF NOT EXISTS iam_attribute_meta (
    attribute_code   VARCHAR(50)  NOT NULL,
    target_domain    VARCHAR(20)  NOT NULL,
    category         VARCHAR(20)  NOT NULL,
    display_name     VARCHAR(255) NOT NULL,
    data_type        VARCHAR(20)  NOT NULL,
    multi_valued     BOOLEAN      NOT NULL DEFAULT FALSE,
    scim_schema_uri  VARCHAR(255),
    parent_name      VARCHAR(50),
    description      VARCHAR(255),
    is_required      BOOLEAN      NOT NULL DEFAULT FALSE,
    mutability       VARCHAR(20),
    admin_only       BOOLEAN      NOT NULL DEFAULT FALSE,
    is_display       BOOLEAN      NOT NULL DEFAULT TRUE,
    view_level       INTEGER      NOT NULL DEFAULT 0,
    edit_level       INTEGER      NOT NULL DEFAULT 5,
    is_encrypted     BOOLEAN      NOT NULL DEFAULT FALSE,
    returned         VARCHAR(20),
    uniqueness       VARCHAR(20),
    case_exact       BOOLEAN      NOT NULL DEFAULT FALSE,
    canonical_values JSONB,
    reference_types  JSONB,
    ui_component     VARCHAR(255),
    PRIMARY KEY (attribute_code, target_domain)
);

CREATE TABLE IF NOT EXISTS scim_resource_type_meta (
    resource_type_id VARCHAR(50)  NOT NULL,
    name             VARCHAR(255) NOT NULL,
    description      VARCHAR(255),
    endpoint         VARCHAR(255) NOT NULL,
    schema_uri       VARCHAR(255) NOT NULL,
    PRIMARY KEY (resource_type_id)
);

CREATE TABLE IF NOT EXISTS scim_resource_type_extensions (
    resource_type_id     VARCHAR(50)  NOT NULL,
    extension_schema_uri VARCHAR(255) NOT NULL,
    required             BOOLEAN      NOT NULL,
    CONSTRAINT fk_rt_ext FOREIGN KEY (resource_type_id) REFERENCES scim_resource_type_meta (resource_type_id)
);

CREATE TABLE IF NOT EXISTS scim_schema_meta (
    schema_uri  VARCHAR(100) NOT NULL,
    name        VARCHAR(255) NOT NULL,
    description VARCHAR(255),
    PRIMARY KEY (schema_uri)
);

CREATE TABLE IF NOT EXISTS scim_dynamic_resource (
    id            BIGINT      NOT NULL,
    scim_id       VARCHAR(64) NOT NULL,
    resource_type VARCHAR(50) NOT NULL,
    external_id   VARCHAR(100),
    attributes    JSONB,
    created_at    TIMESTAMP,
    last_modified TIMESTAMP,
    version       BIGINT,
    PRIMARY KEY (id),
    CONSTRAINT uq_scim_dynamic_scim_id UNIQUE (scim_id)
);

-- ===== 동기화 도메인 테이블 =====

CREATE TABLE IF NOT EXISTS iam_source_system (
    system_id   VARCHAR(50)  NOT NULL,
    system_name VARCHAR(100) NOT NULL,
    system_type VARCHAR(20)  NOT NULL,
    conn_info   JSONB        NOT NULL,
    is_active   BOOLEAN      DEFAULT TRUE,
    created_at  TIMESTAMP,
    created_by  VARCHAR(50),
    updated_at  TIMESTAMP,
    PRIMARY KEY (system_id)
);

CREATE TABLE IF NOT EXISTS iam_sync_history (
    history_id      BIGINT      NOT NULL,
    trace_id        VARCHAR(64) NOT NULL,
    event_type      VARCHAR(32) NOT NULL,
    status          VARCHAR(20),
    sync_direction  VARCHAR(20),
    source_system   VARCHAR(100),
    target_system   VARCHAR(100),
    target_user     VARCHAR(255),
    iam_user_id     BIGINT,
    message         TEXT,
    request_payload JSONB,
    result_data     JSONB,
    created_at      TIMESTAMP,
    PRIMARY KEY (history_id)
);

CREATE TABLE IF NOT EXISTS iam_sync_transform_failure (
    failure_id    BIGINT       NOT NULL,
    history_id    BIGINT       NOT NULL,
    field_name    VARCHAR(100),
    invalid_value TEXT,
    rule_name     VARCHAR(100),
    error_type    VARCHAR(50),
    error_message TEXT,
    suggested_fix TEXT,
    created_at    TIMESTAMP,
    PRIMARY KEY (failure_id)
);

CREATE TABLE IF NOT EXISTS iam_trans_code_meta (
    code_group_id VARCHAR(50)  NOT NULL,
    description   VARCHAR(255),
    created_at    TIMESTAMP,
    PRIMARY KEY (code_group_id)
);

CREATE TABLE IF NOT EXISTS iam_trans_code_value (
    val_id        BIGINT       NOT NULL GENERATED BY DEFAULT AS IDENTITY,
    code_group_id VARCHAR(50)  NOT NULL,
    source_value  VARCHAR(100) NOT NULL,
    target_value  VARCHAR(100) NOT NULL,
    label         VARCHAR(100),
    PRIMARY KEY (val_id)
);

CREATE TABLE IF NOT EXISTS iam_trans_field_mapping (
    mapping_id       BIGINT       NOT NULL GENERATED BY DEFAULT AS IDENTITY,
    rule_id          VARCHAR(50)  NOT NULL,
    source_field     VARCHAR(100) NOT NULL,
    target_field     VARCHAR(100) NOT NULL,
    is_required      BOOLEAN      DEFAULT FALSE,
    min_length       INTEGER,
    max_length       INTEGER,
    transform_type   VARCHAR(20)  NOT NULL DEFAULT 'DIRECT',
    transform_params TEXT,
    code_group_id    VARCHAR(50),
    default_value    VARCHAR(255),
    transform_script TEXT,
    created_at       TIMESTAMP,
    updated_at       TIMESTAMP,
    PRIMARY KEY (mapping_id)
);

CREATE TABLE IF NOT EXISTS iam_trans_mapping (
    map_id       BIGINT      NOT NULL GENERATED BY DEFAULT AS IDENTITY,
    system_id    VARCHAR(50) NOT NULL,
    rule_id      VARCHAR(50) NOT NULL,
    exec_order   INTEGER     DEFAULT 0,
    is_mandatory BOOLEAN     DEFAULT TRUE,
    created_at   TIMESTAMP,
    PRIMARY KEY (map_id)
);

CREATE TABLE IF NOT EXISTS iam_trans_rule_meta (
    rule_id        VARCHAR(50)  NOT NULL,
    rule_name      VARCHAR(100) NOT NULL,
    target_attr    VARCHAR(50)  NOT NULL,
    description    TEXT,
    status         VARCHAR(20)  DEFAULT 'DRAFT',
    script_content TEXT,
    script_hash    VARCHAR(64),
    created_at     TIMESTAMP,
    updated_at     TIMESTAMP,
    PRIMARY KEY (rule_id)
);
