package com.iam.registry.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iam.registry.domain.user.IamUser;
import com.iam.registry.domain.user.IamUserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 이 테스트는 전체 파이프라인 중 Engine -> RabbitMQ -> Registry 로 이어지는
 * 실제 MSA 통합 구간을 Testcontainers(내장 Docker)를 띄워 100% 통합 검증합니다.
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class DataPipelineIntegrationTest {

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
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private IamUserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("시나리오: 실제 RabbitMQ 큐에 CDM 데이터를 넣으면 Registry가 이를 수신하여 DB에 영속화한다.")
    void scenario_fullDataPipeline_viaRabbitMQ() throws Exception {
        // given
        String traceId = "T-INTEGRATION-MSA-001";
        String externalId = "HR-0099";

        // Engine이 Groovy Script를 통해 변환했다고 가정한 Flattened CDM Map
        Map<String, Object> transformedData = Map.of(
                "userName", "integration.user",
                "externalId", externalId,
                "active", true,
                "firstName", "Integration",
                "lastName", "User");

        Map<String, Object> rabbitMqPayload = Map.of(
                "payload", transformedData,
                "systemId", "SAP_HR",
                "traceId", traceId,
                "eventType", "USER_CREATE");

        // when
        // 1. 직접 서비스(UserRegistryService)를 모킹 호출하는 대신, RabbitMQ Exchange/Queue 로 실제 발행
        // (application.yml 설정이나 코드 상에 정의된 바인딩 사용)
        rabbitTemplate.convertAndSend("iam.exchange", "iam.routing.cdm", rabbitMqPayload);

        // then
        // 2. 비동기 큐 소비(Async Consume)가 이루어지므로 즉시 조회가 안될 수 있음. Awaitility를 사용하여 DB 반영 대기.
        await().atMost(10, TimeUnit.SECONDS).until(userIsSavedToDb(externalId));

        Optional<IamUser> savedUserOpt = userRepository.findByExternalId(externalId);
        assertTrue(savedUserOpt.isPresent(), "실제 Postgres DB에 사용자가 저장되어야 합니다.");

        IamUser user = savedUserOpt.get();
        assertEquals("integration.user", user.getUserName());
        assertEquals(true, user.isActive());
    }

    private Callable<Boolean> userIsSavedToDb(String externalId) {
        return () -> userRepository.findByExternalId(externalId).isPresent();
    }
}
