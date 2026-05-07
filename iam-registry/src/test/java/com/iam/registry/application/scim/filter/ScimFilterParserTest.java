package com.iam.registry.application.scim.filter;

import com.iam.registry.domain.common.exception.IamBusinessException;
import com.unboundid.scim2.common.filters.Filter;
import com.unboundid.scim2.common.filters.FilterType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("ScimFilterParser")
class ScimFilterParserTest {

    private final ScimFilterParser parser = new ScimFilterParser();

    // ---- 필터 문자열 생성 헬퍼 ----
    private String filterOfLength(int len) {
        // "userName eq "x...x"" 형태
        String prefix = "userName eq \"";
        String suffix = "\"";
        int valueLen = len - prefix.length() - suffix.length();
        if (valueLen < 0) throw new IllegalArgumentException("len too short");
        return prefix + "a".repeat(valueLen) + suffix;
    }

    @Nested
    @DisplayName("성공 케이스")
    class Success {

        @Test
        @DisplayName("null → null 반환")
        void parse_null_returnsNull() {
            assertThat(parser.parse(null)).isNull();
        }

        @Test
        @DisplayName("빈 문자열 → null 반환")
        void parse_blank_returnsNull() {
            assertThat(parser.parse("   ")).isNull();
        }

        @Test
        @DisplayName("eq 파싱 → EQUAL")
        void parse_eq() {
            assertThat(parser.parse("userName eq \"john\"").getFilterType())
                    .isEqualTo(FilterType.EQUAL);
        }

        @Test
        @DisplayName("ne 파싱 → NOT_EQUAL")
        void parse_ne() {
            assertThat(parser.parse("active ne \"false\"").getFilterType())
                    .isEqualTo(FilterType.NOT_EQUAL);
        }

        @Test
        @DisplayName("pr 파싱 → PRESENT")
        void parse_pr() {
            assertThat(parser.parse("externalId pr").getFilterType())
                    .isEqualTo(FilterType.PRESENT);
        }

        @Test
        @DisplayName("co 파싱 → CONTAINS")
        void parse_co() {
            assertThat(parser.parse("displayName co \"kim\"").getFilterType())
                    .isEqualTo(FilterType.CONTAINS);
        }

        @Test
        @DisplayName("sw 파싱 → STARTS_WITH")
        void parse_sw() {
            assertThat(parser.parse("userName sw \"jo\"").getFilterType())
                    .isEqualTo(FilterType.STARTS_WITH);
        }

        @Test
        @DisplayName("ew 파싱 → ENDS_WITH")
        void parse_ew() {
            assertThat(parser.parse("userName ew \"hn\"").getFilterType())
                    .isEqualTo(FilterType.ENDS_WITH);
        }

        @Test
        @DisplayName("and 복합 → AND, 자식 2개")
        void parse_and() {
            Filter f = parser.parse("userName eq \"a\" and active eq \"true\"");
            assertThat(f.getFilterType()).isEqualTo(FilterType.AND);
            assertThat(f.getCombinedFilters()).hasSize(2);
        }

        @Test
        @DisplayName("or 복합 → OR, 자식 2개")
        void parse_or() {
            Filter f = parser.parse("userName eq \"a\" or userName eq \"b\"");
            assertThat(f.getFilterType()).isEqualTo(FilterType.OR);
        }

        @Test
        @DisplayName("not 필터 → NOT")
        void parse_not() {
            assertThat(parser.parse("not (active eq \"false\")").getFilterType())
                    .isEqualTo(FilterType.NOT);
        }
    }

    @Nested
    @DisplayName("경계선 케이스")
    class Boundary {

        @Test
        @DisplayName("정확히 2048자 → 허용")
        void parse_exactly2048Chars_allowed() {
            assertThatCode(() -> parser.parse(filterOfLength(2048)))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("2049자 → INVALID_SCIM_FILTER (길이 초과)")
        void parse_2049Chars_throwsLengthExceeded() {
            assertThatThrownBy(() -> parser.parse(filterOfLength(2049)))
                    .isInstanceOf(IamBusinessException.class)
                    .hasMessageContaining("길이 초과");
        }

        @Test
        @DisplayName("and 내부에 Phase B 연산자 → 재귀 검증으로 거부")
        void parse_nestedUnsupportedOperator_rejected() {
            // Phase A 합법 연산자 and 안에 gt 삽입
            assertThatThrownBy(() ->
                    parser.parse("userName eq \"a\" and meta.created gt \"2024-01-01\""))
                    .isInstanceOf(IamBusinessException.class)
                    .hasMessageContaining("지원하지 않는 연산자");
        }
    }

    @Nested
    @DisplayName("비즈니스 Fail 케이스")
    class Fail {

        @Test
        @DisplayName("잘못된 구문 → INVALID_SCIM_FILTER (파싱 오류)")
        void parse_invalidSyntax_throws() {
            assertThatThrownBy(() -> parser.parse("userName BADOP \"john\""))
                    .isInstanceOf(IamBusinessException.class);
        }

        @Test
        @DisplayName("gt 연산자 (Phase B) → INVALID_SCIM_FILTER")
        void parse_gt_rejected() {
            assertThatThrownBy(() -> parser.parse("meta.created gt \"2024-01-01\""))
                    .isInstanceOf(IamBusinessException.class)
                    .hasMessageContaining("지원하지 않는 연산자");
        }

        @Test
        @DisplayName("ge 연산자 (Phase B) → INVALID_SCIM_FILTER")
        void parse_ge_rejected() {
            assertThatThrownBy(() -> parser.parse("meta.created ge \"2024-01-01\""))
                    .isInstanceOf(IamBusinessException.class)
                    .hasMessageContaining("지원하지 않는 연산자");
        }

        @Test
        @DisplayName("lt 연산자 (Phase B) → INVALID_SCIM_FILTER")
        void parse_lt_rejected() {
            assertThatThrownBy(() -> parser.parse("meta.created lt \"2024-01-01\""))
                    .isInstanceOf(IamBusinessException.class)
                    .hasMessageContaining("지원하지 않는 연산자");
        }

        @Test
        @DisplayName("not 내부에 Phase B 연산자 → INVALID_SCIM_FILTER")
        void parse_notContainingPhaseBOperator_rejected() {
            assertThatThrownBy(() -> parser.parse("not (meta.created gt \"2024-01-01\")"))
                    .isInstanceOf(IamBusinessException.class)
                    .hasMessageContaining("지원하지 않는 연산자");
        }
    }
}
