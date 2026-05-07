package com.iam.registry.application.scim.filter;

import com.iam.registry.domain.common.exception.IamBusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@DisplayName("DynamicResourceFilterQuery")
class DynamicResourceFilterQueryTest {

    private DynamicResourceFilterQuery queryBuilder;
    private final ScimFilterParser parser = new ScimFilterParser();

    @BeforeEach
    void setUp() {
        queryBuilder = new DynamicResourceFilterQuery();
    }

    /** 파싱 후 바로 build */
    private Map<String, Object> build(String filterStr) {
        return queryBuilder.build(parser.parse(filterStr));
    }

    @Nested
    @DisplayName("성공 케이스 — WHERE 절 구조")
    class WhereClauseStructure {

        @Test
        @DisplayName("eq on id → scim_id 컬럼 exact match")
        void build_eqId_usesScimIdColumn() {
            Map<String, Object> params = build("id eq \"abc-123\"");
            assertThat(queryBuilder.getWhereClause()).contains("scim_id").contains(" = ");
            assertThat(params).containsValue("abc-123");
        }

        @Test
        @DisplayName("eq on externalId → external_id 컬럼 case-insensitive (RFC 7643 caseExact=false)")
        void build_eqExternalId() {
            Map<String, Object> params = build("externalId eq \"EXT-001\"");
            assertThat(queryBuilder.getWhereClause()).contains("external_id").contains(" = ");
            // externalId는 CASE_EXACT 미포함 → 파라미터 소문자 변환
            assertThat(params).containsValue("ext-001");
        }

        @Test
        @DisplayName("eq on displayName → LOWER(attributes->>'displayName') = LOWER(값)")
        void build_eqDisplayName_caseInsensitive() {
            Map<String, Object> params = build("displayName eq \"홍길동\"");
            String where = queryBuilder.getWhereClause();
            assertThat(where).contains("LOWER(attributes->>'displayName')")
                    .contains(" = ");
            // 파라미터 값은 소문자 변환됨
            assertThat(params).containsValue("홍길동");
        }

        @Test
        @DisplayName("eq on active → caseExact=true, exact match")
        void build_eqActive_exactMatch() {
            build("active eq \"true\"");
            assertThat(queryBuilder.getWhereClause())
                    .contains("attributes->>'active'")
                    .doesNotContain("LOWER");
        }

        @Test
        @DisplayName("pr (present) → IS NOT NULL")
        void build_present() {
            build("displayName pr");
            assertThat(queryBuilder.getWhereClause()).contains("IS NOT NULL");
        }

        @Test
        @DisplayName("ne → != 연산자")
        void build_notEqual() {
            build("active ne \"false\"");
            assertThat(queryBuilder.getWhereClause()).contains("!=");
        }

        @Test
        @DisplayName("and 복합 → ( A AND B ) 괄호 포함")
        void build_andFilter() {
            build("displayName eq \"kim\" and active eq \"true\"");
            assertThat(queryBuilder.getWhereClause()).contains(" AND ");
        }

        @Test
        @DisplayName("or 복합 → ( A OR B ) 괄호 포함")
        void build_orFilter() {
            build("displayName eq \"kim\" or displayName eq \"lee\"");
            assertThat(queryBuilder.getWhereClause()).contains(" OR ");
        }

        @Test
        @DisplayName("not 필터 → NOT ( A )")
        void build_notFilter() {
            build("not (active eq \"false\")");
            assertThat(queryBuilder.getWhereClause()).startsWith("NOT (");
        }
    }

    @Nested
    @DisplayName("경계선 케이스 — LIKE 이스케이프")
    class LikeEscape {

        @Test
        @DisplayName("co → ESCAPE '!' 절 포함")
        void build_contains_hasEscapeClause() {
            build("displayName co \"검색어\"");
            assertThat(queryBuilder.getWhereClause()).contains("ESCAPE '!'");
        }

        @Test
        @DisplayName("sw → ESCAPE '!' 절 포함")
        void build_startsWith_hasEscapeClause() {
            build("displayName sw \"홍\"");
            assertThat(queryBuilder.getWhereClause()).contains("ESCAPE '!'");
        }

        @Test
        @DisplayName("ew → ESCAPE '!' 절 포함")
        void build_endsWith_hasEscapeClause() {
            build("displayName ew \"동\"");
            assertThat(queryBuilder.getWhereClause()).contains("ESCAPE '!'");
        }

        @Test
        @DisplayName("co 값에 % → !% 이스케이프")
        void build_contains_percentEscaped() {
            Map<String, Object> params = build("displayName co \"100%\"");
            assertThat(params).containsValue("100!%");
        }

        @Test
        @DisplayName("co 값에 _ → !_ 이스케이프")
        void build_contains_underscoreEscaped() {
            Map<String, Object> params = build("displayName co \"a_b\"");
            assertThat(params).containsValue("a!_b");
        }

        @Test
        @DisplayName("co 값에 ! → !! 이스케이프 (escape char 자신)")
        void build_contains_exclamationEscaped() {
            Map<String, Object> params = build("displayName co \"a!b\"");
            assertThat(params).containsValue("a!!b");
        }

        @Test
        @DisplayName("co 값에 혼합 특수문자 → 모두 이스케이프 (! 우선)")
        void build_contains_mixedSpecialChars() {
            // "50%_off!" → ! 우선: "50%_off!" → "50!%!_off!!"
            Map<String, Object> params = build("displayName co \"50%_off!\"");
            assertThat(params).containsValue("50!%!_off!!");
        }

        @Test
        @DisplayName("eq는 ESCAPE 절 없음 (exact match)")
        void build_eq_noEscapeClause() {
            build("displayName eq \"100%\"");
            assertThat(queryBuilder.getWhereClause()).doesNotContain("ESCAPE '!'");
        }

        @Test
        @DisplayName("id co 는 caseExact path — ESCAPE '!' 포함")
        void build_idContains_exactPathEscaped() {
            // id는 CASE_EXACT=true 이므로 caseExact 경로의 LIKE escape 테스트
            Map<String, Object> params = build("id co \"abc%\"");
            assertThat(queryBuilder.getWhereClause()).contains("ESCAPE '!'");
            assertThat(params).containsValue("abc!%");
        }
    }

    @Nested
    @DisplayName("비즈니스 Fail 케이스")
    class Fail {

        @Test
        @DisplayName("지원하지 않는 속성 userName → INVALID_SCIM_FILTER 예외")
        void build_userNameAttribute_rejected() {
            // userName은 iam_user 전용, DynamicResource에서 미지원
            assertThatThrownBy(() -> build("userName eq \"john\""))
                    .isInstanceOf(IamBusinessException.class);
        }

        @Test
        @DisplayName("지원하지 않는 속성 emails.value → INVALID_SCIM_FILTER 예외")
        void build_emailsAttribute_rejected() {
            assertThatThrownBy(() -> build("emails.value eq \"a@b.com\""))
                    .isInstanceOf(IamBusinessException.class);
        }

        @Test
        @DisplayName("지원하지 않는 속성 title → INVALID_SCIM_FILTER 예외")
        void build_titleAttribute_rejected() {
            assertThatThrownBy(() -> build("title eq \"Manager\""))
                    .isInstanceOf(IamBusinessException.class);
        }
    }
}
