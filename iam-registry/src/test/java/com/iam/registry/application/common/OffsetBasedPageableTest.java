package com.iam.registry.application.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OffsetBasedPageable")
class OffsetBasedPageableTest {

    @Nested
    @DisplayName("성공 케이스")
    class Success {

        @Test
        @DisplayName("offset=0, limit=100 → pageNumber=0, offset=0")
        void of_firstPage() {
            OffsetBasedPageable p = OffsetBasedPageable.of(0, 100);
            assertThat(p.getOffset()).isEqualTo(0L);
            assertThat(p.getPageSize()).isEqualTo(100);
            assertThat(p.getPageNumber()).isEqualTo(0);
        }

        @Test
        @DisplayName("offset=100, limit=100 → pageNumber=1 (정렬된 2페이지)")
        void of_secondPage_aligned() {
            OffsetBasedPageable p = OffsetBasedPageable.of(100, 100);
            assertThat(p.getPageNumber()).isEqualTo(1);
            assertThat(p.getOffset()).isEqualTo(100L);
        }

        @Test
        @DisplayName("RFC 7644 비정렬 offset: startIndex=151, count=100 → offset=150")
        void of_nonAlignedOffset_preservesExactOffset() {
            // 기존 PageRequest.of(150/100, 100) = PageRequest.of(1, 100)은 offset=100을 반환하여 틀렸음
            // OffsetBasedPageable은 getOffset()=150을 그대로 반환해야 함
            OffsetBasedPageable p = OffsetBasedPageable.of(150, 100);
            assertThat(p.getOffset()).isEqualTo(150L);
        }

        @Test
        @DisplayName("기본 sort: id ASC")
        void of_defaultSort_isIdAsc() {
            Sort.Order order = OffsetBasedPageable.of(0, 10).getSort().getOrderFor("id");
            assertThat(order).isNotNull();
            assertThat(order.getDirection()).isEqualTo(Sort.Direction.ASC);
        }

        @Test
        @DisplayName("hasPrevious: offset=0 → false")
        void hasPrevious_atStart_isFalse() {
            assertThat(OffsetBasedPageable.of(0, 10).hasPrevious()).isFalse();
        }

        @Test
        @DisplayName("hasPrevious: offset>0 → true")
        void hasPrevious_notAtStart_isTrue() {
            assertThat(OffsetBasedPageable.of(10, 10).hasPrevious()).isTrue();
        }
    }

    @Nested
    @DisplayName("경계선 케이스 — 페이지 이동")
    class Navigation {

        @Test
        @DisplayName("next(): offset += limit")
        void next_incrementsByLimit() {
            OffsetBasedPageable p = OffsetBasedPageable.of(0, 50);
            Pageable next = p.next();
            assertThat(next.getOffset()).isEqualTo(50L);
            assertThat(next.getPageSize()).isEqualTo(50);
        }

        @Test
        @DisplayName("next().next(): 두 번 → offset = 2 * limit")
        void next_twice() {
            OffsetBasedPageable p = OffsetBasedPageable.of(0, 30);
            assertThat(p.next().next().getOffset()).isEqualTo(60L);
        }

        @Test
        @DisplayName("previousOrFirst(): offset=0이면 자기 자신 반환")
        void previousOrFirst_atStart_returnsSelf() {
            OffsetBasedPageable p = OffsetBasedPageable.of(0, 50);
            assertThat(p.previousOrFirst()).isSameAs(p);
        }

        @Test
        @DisplayName("previousOrFirst(): offset>0이면 offset-limit 반환")
        void previousOrFirst_movesBack() {
            OffsetBasedPageable p = OffsetBasedPageable.of(100, 50);
            assertThat(p.previousOrFirst().getOffset()).isEqualTo(50L);
        }

        @Test
        @DisplayName("previousOrFirst(): offset < limit이면 0 (음수 방어)")
        void previousOrFirst_smallOffset_doesNotGoNegative() {
            OffsetBasedPageable p = OffsetBasedPageable.of(30, 50);
            assertThat(p.previousOrFirst().getOffset()).isEqualTo(0L);
        }

        @Test
        @DisplayName("first(): 항상 offset=0 반환")
        void first_resetsToZero() {
            assertThat(OffsetBasedPageable.of(300, 50).first().getOffset()).isEqualTo(0L);
        }

        @Test
        @DisplayName("withPage(n): offset = n * limit")
        void withPage_computesCorrectOffset() {
            OffsetBasedPageable p = OffsetBasedPageable.of(0, 30);
            assertThat(p.withPage(0).getOffset()).isEqualTo(0L);
            assertThat(p.withPage(3).getOffset()).isEqualTo(90L);
        }

        @Test
        @DisplayName("limit=0 → pageNumber=0 (divide-by-zero 방어)")
        void of_zeroLimit_pageNumberIsSafe() {
            assertThat(OffsetBasedPageable.of(0, 0).getPageNumber()).isEqualTo(0);
        }
    }
}
