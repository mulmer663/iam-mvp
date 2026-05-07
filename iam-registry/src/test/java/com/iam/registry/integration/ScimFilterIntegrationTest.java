package com.iam.registry.integration;

import com.iam.registry.application.common.ScimListResponse;
import com.iam.registry.application.common.ScimUserResponse;
import com.iam.registry.application.scim.ScimResourceService;
import com.iam.registry.application.scim.ScimSearchRequest;
import com.iam.registry.application.user.UserQueryService;
import com.iam.registry.domain.common.exception.IamBusinessException;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * RFC 7644 필터·페이징 통합 테스트.
 *
 * 실제 PostgreSQL + Flyway 마이그레이션 환경에서:
 * - LIKE escape (%, _, ! 특수문자)
 * - OffsetBasedPageable 비정렬 offset 정확성
 * - count=0 totalResults-only 동작
 * - 복합 필터(and/or/not)
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ScimFilterIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-trixie")
            .withDatabaseName("iam_db")
            .withUsername("iam_user")
            .withPassword("iam_password");

    @Container
    static RabbitMQContainer rabbitMQ = new RabbitMQContainer("rabbitmq:3-management");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");

        registry.add("spring.rabbitmq.host", rabbitMQ::getHost);
        registry.add("spring.rabbitmq.port", rabbitMQ::getAmqpPort);
        registry.add("spring.rabbitmq.username", rabbitMQ::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbitMQ::getAdminPassword);
    }

    @Autowired
    UserQueryService userQueryService;

    @Autowired
    ScimResourceService scimResourceService;

    // ---- 테스트 픽스처 설정 ----

    @BeforeEach
    void clearUsers() {
        // 각 테스트 독립 실행: 전체 사용자 조회 후 삭제
        ScimListResponse<ScimUserResponse> all =
                userQueryService.getUsers(ScimSearchRequest.of(null, 1, 200));
        all.resources().forEach(u -> scimResourceService.deleteUser(Long.parseLong(u.id())));
    }

    private ScimUserResponse createUser(String userName, String displayName, boolean active) {
        return scimResourceService.createUser(Map.of(
                "userName", userName,
                "displayName", displayName,
                "active", active));
    }

    // ---- 성공 케이스 ----

    @Test
    @Order(1)
    @DisplayName("eq 필터: userName eq → 정확히 해당 사용자만 반환")
    void filter_eq_userName() {
        createUser("alice", "Alice Kim", true);
        createUser("bob", "Bob Lee", true);

        ScimListResponse<ScimUserResponse> result =
                userQueryService.getUsers(ScimSearchRequest.of("userName eq \"alice\"", 1, 10));

        assertThat(result.totalResults()).isEqualTo(1);
        assertThat(result.resources()).hasSize(1);
        assertThat(result.resources().get(0).userName()).isEqualTo("alice");
    }

    @Test
    @Order(2)
    @DisplayName("co 필터: displayName co → 부분 일치 반환")
    void filter_co_displayName() {
        createUser("user1", "홍길동", true);
        createUser("user2", "홍길순", true);
        createUser("user3", "이영희", true);

        ScimListResponse<ScimUserResponse> result =
                userQueryService.getUsers(ScimSearchRequest.of("displayName co \"홍길\"", 1, 10));

        assertThat(result.totalResults()).isEqualTo(2);
        assertThat(result.resources())
                .extracting(ScimUserResponse::userName)
                .containsExactlyInAnyOrder("user1", "user2");
    }

    @Test
    @Order(3)
    @DisplayName("sw 필터: userName sw → 접두사 일치 반환")
    void filter_sw_userName() {
        createUser("john_doe", "John", true);
        createUser("john_smith", "Smith", true);
        createUser("jane_doe", "Jane", true);

        ScimListResponse<ScimUserResponse> result =
                userQueryService.getUsers(ScimSearchRequest.of("userName sw \"john\"", 1, 10));

        assertThat(result.totalResults()).isEqualTo(2);
    }

    @Test
    @Order(4)
    @DisplayName("active eq 필터: 비활성 사용자만 반환")
    void filter_eq_active_false() {
        createUser("active_user", "Active", true);
        createUser("inactive_user", "Inactive", false);

        ScimListResponse<ScimUserResponse> result =
                userQueryService.getUsers(ScimSearchRequest.of("active eq \"false\"", 1, 10));

        assertThat(result.totalResults()).isEqualTo(1);
        assertThat(result.resources().get(0).userName()).isEqualTo("inactive_user");
    }

    @Test
    @Order(5)
    @DisplayName("and 복합 필터: sw + active eq → 교집합 반환")
    void filter_and_swAndActive() {
        createUser("kim_active", "Kim Active", true);
        createUser("kim_inactive", "Kim Inactive", false);
        createUser("lee_active", "Lee Active", true);

        ScimListResponse<ScimUserResponse> result =
                userQueryService.getUsers(ScimSearchRequest.of(
                        "userName sw \"kim\" and active eq \"true\"", 1, 10));

        assertThat(result.totalResults()).isEqualTo(1);
        assertThat(result.resources().get(0).userName()).isEqualTo("kim_active");
    }

    @Test
    @Order(6)
    @DisplayName("or 복합 필터: 두 userName 중 하나 일치 반환")
    void filter_or_twoUserNames() {
        createUser("alpha", "Alpha", true);
        createUser("beta", "Beta", true);
        createUser("gamma", "Gamma", true);

        ScimListResponse<ScimUserResponse> result =
                userQueryService.getUsers(ScimSearchRequest.of(
                        "userName eq \"alpha\" or userName eq \"beta\"", 1, 10));

        assertThat(result.totalResults()).isEqualTo(2);
    }

    @Test
    @Order(7)
    @DisplayName("not 필터: not(active eq false) → 활성 사용자만 반환")
    void filter_not_active() {
        createUser("active_1", "Active One", true);
        createUser("inactive_1", "Inactive One", false);

        ScimListResponse<ScimUserResponse> result =
                userQueryService.getUsers(ScimSearchRequest.of(
                        "not (active eq \"false\")", 1, 10));

        assertThat(result.totalResults()).isEqualTo(1);
        assertThat(result.resources().get(0).userName()).isEqualTo("active_1");
    }

    // ---- 경계선 케이스 — 페이징 ----

    @Test
    @Order(10)
    @DisplayName("페이징: startIndex=1, count=2 → 처음 2건")
    void paging_firstPage() {
        // id ASC 정렬 → 생성 순서대로
        for (int i = 1; i <= 5; i++) {
            createUser("pager" + i, "Pager " + i, true);
        }

        ScimListResponse<ScimUserResponse> result =
                userQueryService.getUsers(ScimSearchRequest.of(null, 1, 2));

        assertThat(result.totalResults()).isEqualTo(5);
        assertThat(result.resources()).hasSize(2);
        assertThat(result.startIndex()).isEqualTo(1);
        assertThat(result.itemsPerPage()).isEqualTo(2);
    }

    @Test
    @Order(11)
    @DisplayName("페이징: startIndex=3, count=2 → 3번째부터 2건 (비정렬 offset)")
    void paging_nonAlignedOffset() {
        // 5건 생성
        List<String> names = List.of("a_user", "b_user", "c_user", "d_user", "e_user");
        names.forEach(n -> createUser(n, n, true));

        // startIndex=3, count=2 → offset=2 → 3,4번째 사용자
        ScimListResponse<ScimUserResponse> result =
                userQueryService.getUsers(ScimSearchRequest.of(null, 3, 2));

        assertThat(result.totalResults()).isEqualTo(5);
        assertThat(result.resources()).hasSize(2);
        assertThat(result.startIndex()).isEqualTo(3);
        // offset=2, limit=2 이므로 비정렬 offset (2/2=1 page) — OffsetBasedPageable이 정확히 동작해야 함
    }

    @Test
    @Order(12)
    @DisplayName("페이징: startIndex=4, count=3 총 5건 → 마지막 2건만 반환")
    void paging_lastPartialPage() {
        for (int i = 1; i <= 5; i++) {
            createUser("last" + i, "Last " + i, true);
        }

        ScimListResponse<ScimUserResponse> result =
                userQueryService.getUsers(ScimSearchRequest.of(null, 4, 3));

        assertThat(result.totalResults()).isEqualTo(5);
        assertThat(result.resources()).hasSize(2); // 4,5번째
    }

    @Test
    @Order(13)
    @DisplayName("count=0 → Resources=[], totalResults=전체 건수")
    void paging_countZero_returnsOnlyTotalResults() {
        createUser("count_user1", "Count 1", true);
        createUser("count_user2", "Count 2", true);
        createUser("count_user3", "Count 3", true);

        ScimListResponse<ScimUserResponse> result =
                userQueryService.getUsers(ScimSearchRequest.of(null, 1, 0));

        assertThat(result.totalResults()).isEqualTo(3);
        assertThat(result.resources()).isEmpty();
        assertThat(result.itemsPerPage()).isEqualTo(0);
    }

    // ---- 경계선 케이스 — LIKE 이스케이프 ----

    @Test
    @Order(20)
    @DisplayName("LIKE escape: % 포함 displayName co → % 가 와일드카드로 해석되지 않음")
    void likeEscape_percent_treatedLiterally() {
        // displayName에 % 포함한 사용자와 그렇지 않은 사용자
        createUser("escape_u1", "100% 완료", true);
        createUser("escape_u2", "100x 완료", true);

        // "100%"로 검색 → "100% 완료"만 매칭 (% 가 리터럴로 처리)
        ScimListResponse<ScimUserResponse> result =
                userQueryService.getUsers(ScimSearchRequest.of("displayName co \"100%\"", 1, 10));

        assertThat(result.totalResults()).isEqualTo(1);
        assertThat(result.resources().get(0).userName()).isEqualTo("escape_u1");
    }

    @Test
    @Order(21)
    @DisplayName("LIKE escape: _ 포함 displayName co → _ 가 와일드카드로 해석되지 않음")
    void likeEscape_underscore_treatedLiterally() {
        createUser("esc_u1", "a_b 매칭", true);
        createUser("esc_u2", "axb 비매칭", true);

        ScimListResponse<ScimUserResponse> result =
                userQueryService.getUsers(ScimSearchRequest.of("displayName co \"a_b\"", 1, 10));

        assertThat(result.totalResults()).isEqualTo(1);
        assertThat(result.resources().get(0).userName()).isEqualTo("esc_u1");
    }

    // ---- 비즈니스 Fail 케이스 ----

    @Test
    @Order(30)
    @DisplayName("잘못된 SCIM filter → IamBusinessException (파싱 오류)")
    void filter_invalidSyntax_throwsException() {
        assertThatThrownBy(() ->
                userQueryService.getUsers(ScimSearchRequest.of("userName INVALID \"x\"", 1, 10)))
                .isInstanceOf(IamBusinessException.class);
    }

    @Test
    @Order(31)
    @DisplayName("Phase B 연산자 gt → IamBusinessException")
    void filter_phaseB_gt_throwsException() {
        assertThatThrownBy(() ->
                userQueryService.getUsers(ScimSearchRequest.of("meta.created gt \"2024-01-01\"", 1, 10)))
                .isInstanceOf(IamBusinessException.class);
    }

    @Test
    @Order(32)
    @DisplayName("지원하지 않는 User 속성 → IamBusinessException")
    void filter_unsupportedAttribute_throwsException() {
        assertThatThrownBy(() ->
                userQueryService.getUsers(ScimSearchRequest.of("emails.value eq \"a@b.com\"", 1, 10)))
                .isInstanceOf(IamBusinessException.class);
    }

    @Test
    @Order(33)
    @DisplayName("필터 없는 조회, 결과 없으면 totalResults=0, Resources=[]")
    void getUsers_empty_returnsEmptyListResponse() {
        ScimListResponse<ScimUserResponse> result =
                userQueryService.getUsers(ScimSearchRequest.of(null, 1, 10));

        assertThat(result.totalResults()).isEqualTo(0);
        assertThat(result.resources()).isEmpty();
        assertThat(result.schemas())
                .contains("urn:ietf:params:scim:api:messages:2.0:ListResponse");
    }
}
