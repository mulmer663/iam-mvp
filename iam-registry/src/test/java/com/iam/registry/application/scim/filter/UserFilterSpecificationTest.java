package com.iam.registry.application.scim.filter;

import com.iam.registry.domain.common.exception.IamBusinessException;
import com.iam.registry.domain.user.IamUser;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyChar;
import static org.mockito.Mockito.verify;

@DisplayName("UserFilterSpecification")
@ExtendWith(MockitoExtension.class)
class UserFilterSpecificationTest {

    private final UserFilterSpecification spec = new UserFilterSpecification();
    private final ScimFilterParser parser = new ScimFilterParser();

    /**
     * RETURNS_DEEP_STUBS: 체인 호출(cb.lower(root.get(...))) 이 자동으로 mock 반환.
     * 성공 경로는 "예외 없음" 검증만 하고, escape char 검증은 통합 테스트에서 수행.
     */
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Root<IamUser> root;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private CriteriaBuilder cb;

    @Mock
    private CriteriaQuery<?> query;

    private void invoke(String filterStr) {
        spec.from(parser.parse(filterStr)).toPredicate(root, query, cb);
    }

    @Nested
    @DisplayName("성공 케이스 — 예외 없이 실행")
    class Success {

        @Test
        @DisplayName("eq on userName → 예외 없음")
        void eq_userName_noException() {
            assertThatCode(() -> invoke("userName eq \"john\"")).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("eq on displayName → 예외 없음")
        void eq_displayName_noException() {
            assertThatCode(() -> invoke("displayName eq \"홍길동\"")).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("eq on active → 예외 없음")
        void eq_active_noException() {
            assertThatCode(() -> invoke("active eq \"true\"")).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("pr on externalId → 예외 없음")
        void pr_externalId_noException() {
            assertThatCode(() -> invoke("externalId pr")).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("co (contains) on displayName → cb.like() 호출 + escape char '!'")
        void co_displayName_callsLikeWithEscapeChar() {
            invoke("displayName co \"검색\"");
            // escape char '!' 로 cb.like(expr, pattern, '!') 가 호출됐는지 검증
            verify(cb).like(any(), any(String.class), eq('!'));
        }

        @Test
        @DisplayName("sw (starts_with) on userName → cb.like() + escape '!'")
        void sw_userName_callsLikeWithEscapeChar() {
            invoke("userName sw \"jo\"");
            verify(cb).like(any(), any(String.class), eq('!'));
        }

        @Test
        @DisplayName("ew (ends_with) on userName → cb.like() + escape '!'")
        void ew_userName_callsLikeWithEscapeChar() {
            invoke("userName ew \"hn\"");
            verify(cb).like(any(), any(String.class), eq('!'));
        }

        @Test
        @DisplayName("and 복합 필터 → 예외 없음")
        void and_filter_noException() {
            assertThatCode(() -> invoke("userName eq \"a\" and active eq \"true\""))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("name.familyName 경로 → 예외 없음")
        void eq_nameFamilyName_noException() {
            assertThatCode(() -> invoke("name.familyName eq \"홍\"")).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("경계선 케이스")
    class Boundary {

        @Test
        @DisplayName("not 필터 → 예외 없음")
        void not_filter_noException() {
            assertThatCode(() -> invoke("not (active eq \"false\")")).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("or 복합 필터 → 예외 없음")
        void or_filter_noException() {
            assertThatCode(() -> invoke("userName eq \"a\" or userName eq \"b\""))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("비즈니스 Fail 케이스")
    class Fail {

        @Test
        @DisplayName("지원하지 않는 속성 → INVALID_SCIM_FILTER 예외")
        void unsupportedAttribute_throwsException() {
            assertThatThrownBy(() -> invoke("emails.value eq \"a@b.com\""))
                    .isInstanceOf(IamBusinessException.class)
                    .hasMessageContaining("지원하지 않는 User 속성");
        }

        @Test
        @DisplayName("지원하지 않는 속성 deptCode (DynamicResource 전용) → 예외")
        void deptCodeAttribute_throwsException() {
            assertThatThrownBy(() -> invoke("deptCode eq \"D001\""))
                    .isInstanceOf(IamBusinessException.class);
        }

        @Test
        @DisplayName("active 에 co 연산자 → 지원하지 않는 연산자 예외")
        void activeWithCoOperator_throwsException() {
            assertThatThrownBy(() -> invoke("active co \"tr\""))
                    .isInstanceOf(IamBusinessException.class)
                    .hasMessageContaining("지원하지 않는 연산자");
        }

        @Test
        @DisplayName("active 에 sw 연산자 → 지원하지 않는 연산자 예외")
        void activeWithSwOperator_throwsException() {
            assertThatThrownBy(() -> invoke("active sw \"tr\""))
                    .isInstanceOf(IamBusinessException.class)
                    .hasMessageContaining("지원하지 않는 연산자");
        }

        @Test
        @DisplayName("id 에 숫자 아닌 값 → INVALID_SCIM_FILTER 예외")
        void idWithNonNumericValue_throwsException() {
            assertThatThrownBy(() -> invoke("id eq \"not-a-number\""))
                    .isInstanceOf(IamBusinessException.class)
                    .hasMessageContaining("id 속성은 숫자여야 합니다");
        }

        @Test
        @DisplayName("id 에 co 연산자 → 지원하지 않는 연산자 예외")
        void idWithCoOperator_throwsException() {
            assertThatThrownBy(() -> invoke("id co \"123\""))
                    .isInstanceOf(IamBusinessException.class)
                    .hasMessageContaining("지원하지 않는 연산자");
        }
    }
}
