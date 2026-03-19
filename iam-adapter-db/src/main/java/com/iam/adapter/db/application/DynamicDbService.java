package com.iam.adapter.db.application;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class DynamicDbService {

    /**
     * Executes a query on a dynamically configured database and returns the result.
     */
    public List<Map<String, Object>> fetchSourceData(String driver, String url, String user, String password,
            String query) {
        log.info("Fetching data dynamically from: {}", url);

        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName(driver);
        dataSource.setUrl(url);
        dataSource.setUsername(user);
        dataSource.setPassword(password);

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        // Execute the query to get RAW data
        return jdbcTemplate.queryForList(query);
    }

    /**
     * Executes an update on a dynamically configured database.
     */
    public int executeUpdate(String driver, String url, String user, String password, String query, Object... params) {
        log.info("Executing update dynamically on: {}", url);

        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName(driver);
        dataSource.setUrl(url);
        dataSource.setUsername(user);
        dataSource.setPassword(password);

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        return jdbcTemplate.update(query, params);
    }
}
