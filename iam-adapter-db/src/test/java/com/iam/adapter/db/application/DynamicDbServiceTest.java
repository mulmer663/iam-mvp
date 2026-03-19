package com.iam.adapter.db.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
class DynamicDbServiceTest {

    @InjectMocks
    private DynamicDbService dynamicDbService;

    @Test
    @DisplayName("원천지 DB 연동 조회 단위 테스트 (H2 In-Memory)")
    void fetchSourceData_success() {
        // given
        String driver = "org.h2.Driver";
        String url = "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1";
        String username = "sa";
        String password = "";
        String sql = "SELECT * FROM HR_USERS";

        // 단위 테스트용 H2 스키마 및 데이터 초기화
        org.springframework.jdbc.datasource.DriverManagerDataSource dataSource = new org.springframework.jdbc.datasource.DriverManagerDataSource();
        dataSource.setDriverClassName(driver);
        dataSource.setUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);

        org.springframework.jdbc.core.JdbcTemplate realJdbcTemplate = new org.springframework.jdbc.core.JdbcTemplate(
                dataSource);
        try {
            realJdbcTemplate.execute("DROP TABLE HR_USERS");
        } catch (Exception e) {
        }
        realJdbcTemplate.execute("CREATE TABLE HR_USERS (EMP_NO VARCHAR(50), NAME VARCHAR(100), STATUS VARCHAR(1))");
        realJdbcTemplate.execute("INSERT INTO HR_USERS VALUES ('E1001', '홍길동', 'A')");
        realJdbcTemplate.execute("INSERT INTO HR_USERS VALUES ('E1002', '김철수', 'I')");

        // when
        List<Map<String, Object>> result = dynamicDbService.fetchSourceData(driver, url, username, password, sql);

        // then
        assertNotNull(result);
        assertEquals(2, result.size());

        Map<String, Object> firstMsg = result.get(0);
        assertNotNull(firstMsg);
        assertEquals("E1001", firstMsg.get("EMP_NO"));
        assertEquals("홍길동", firstMsg.get("NAME"));
    }
}
