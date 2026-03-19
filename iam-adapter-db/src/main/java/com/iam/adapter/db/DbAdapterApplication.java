package com.iam.adapter.db;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;

// Exclude auto configuration for DataSources because this adapter connects dynamically
@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class })
@EnableDiscoveryClient
public class DbAdapterApplication {

    public static void main(String[] args) {
        SpringApplication.run(DbAdapterApplication.class, args);
    }
}
