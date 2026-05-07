package com.iam.registry.application.scim;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ScimSearchRequest")
class ScimSearchRequestTest {

    @Nested
    @DisplayName("성공 케이스")
    class Success {

        @Test
        @DisplayName("정상 파라미터 그대로 유지")
        void of_normalParams_preserved() {
            ScimSearchRequest req = ScimSearchRequest.of("userName eq \"a\"", 5, 20);
            assertThat(req.filter()).isEqualTo("userName eq \"a\"");
            assertThat(req.startIndex()).isEqualTo(5);
            assertThat(req.count()).isEqualTo(20);
        }

        @Test
        @DisplayName("offset() = startIndex - 1")
        void offset_isStartIndexMinusOne() {
            assertThat(ScimSearchRequest.of(null, 1, 10).offset()).isEqualTo(0);
            assertThat(ScimSearchRequest.of(null, 51, 100).offset()).isEqualTo(50);
            assertThat(ScimSearchRequest.of(null, 101, 50).offset()).isEqualTo(100);
        }

        @Test
        @DisplayName("count=1 → isCountOnly()=false")
        void of_count1_notCountOnly() {
            assertThat(ScimSearchRequest.of(null, 1, 1).isCountOnly()).isFalse();
        }

        @Test
        @DisplayName("null filter → filter 필드 null")
        void of_nullFilter_preserved() {
            assertThat(ScimSearchRequest.of(null, 1, 10).filter()).isNull();
        }
    }

    @Nested
    @DisplayName("경계선 케이스")
    class Boundary {

        @Test
        @DisplayName("startIndex=1 (최솟값) → 그대로 유지")
        void of_startIndex1_kept() {
            assertThat(ScimSearchRequest.of(null, 1, 10).startIndex()).isEqualTo(1);
        }

        @Test
        @DisplayName("startIndex=0 → 1로 정규화")
        void of_startIndex0_normalizesToOne() {
            assertThat(ScimSearchRequest.of(null, 0, 10).startIndex()).isEqualTo(1);
        }

        @Test
        @DisplayName("startIndex=-99 → 1로 정규화")
        void of_negativeStartIndex_normalizesToOne() {
            assertThat(ScimSearchRequest.of(null, -99, 10).startIndex()).isEqualTo(1);
        }

        @Test
        @DisplayName("null startIndex → 1로 정규화")
        void of_nullStartIndex_normalizesToOne() {
            assertThat(ScimSearchRequest.of(null, null, 10).startIndex()).isEqualTo(1);
        }

        @Test
        @DisplayName("count=0 → isCountOnly()=true, offset=0")
        void of_count0_isCountOnly() {
            ScimSearchRequest req = ScimSearchRequest.of(null, 1, 0);
            assertThat(req.isCountOnly()).isTrue();
            assertThat(req.count()).isEqualTo(0);
            assertThat(req.offset()).isEqualTo(0);
        }

        @Test
        @DisplayName("count=200 (최대) → 그대로 허용")
        void of_count200_allowed() {
            assertThat(ScimSearchRequest.of(null, 1, 200).count()).isEqualTo(200);
        }

        @Test
        @DisplayName("count=201 → 200으로 클램핑")
        void of_count201_clampedTo200() {
            assertThat(ScimSearchRequest.of(null, 1, 201).count()).isEqualTo(200);
        }

        @Test
        @DisplayName("count=9999 → 200으로 클램핑")
        void of_largeCount_clampedTo200() {
            assertThat(ScimSearchRequest.of(null, 1, 9999).count()).isEqualTo(200);
        }

        @Test
        @DisplayName("null count → DEFAULT_COUNT(100)")
        void of_nullCount_defaultsTo100() {
            assertThat(ScimSearchRequest.of(null, 1, null).count()).isEqualTo(100);
        }

        @Test
        @DisplayName("count=-1 → DEFAULT_COUNT(100)")
        void of_negativeCount_defaultsTo100() {
            assertThat(ScimSearchRequest.of(null, 1, -1).count()).isEqualTo(100);
        }
    }
}
