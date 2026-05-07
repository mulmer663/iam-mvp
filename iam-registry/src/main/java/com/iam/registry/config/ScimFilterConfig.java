package com.iam.registry.config;

import com.iam.registry.application.scim.filter.ScimFilterParser;
import com.iam.registry.application.scim.filter.UserFilterSpecification;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ScimFilterConfig {

    @Bean
    public ScimFilterParser scimFilterParser() {
        return new ScimFilterParser();
    }

    @Bean
    public UserFilterSpecification userFilterSpecification() {
        return new UserFilterSpecification();
    }
}
