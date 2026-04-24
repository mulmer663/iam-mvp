package com.iam.registry.config.init;

import com.iam.registry.domain.common.constant.ScimEndpointConstants;
import com.iam.registry.domain.scim.ScimResourceTypeMetaRepository;
import com.iam.registry.interfaces.rest.ScimEndpointManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Registers HTTP endpoints for each non-core ResourceType present in the DB
 * after the schema seed runs. Core types (User/Group) are served by static
 * controllers and skipped here.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(2)
public class ScimDynamicEndpointInitializer implements CommandLineRunner {

    private final ScimResourceTypeMetaRepository repository;
    private final ScimEndpointManager endpointManager;

    @Override
    public void run(String... args) {
        log.info("[ScimDynamicEndpointInitializer] Registering dynamic resource endpoints...");

        repository.findAll().stream()
                .filter(meta -> !ScimEndpointConstants.isCoreType(meta.getId()))
                .forEach(meta -> endpointManager.register(meta.getId(), meta.getEndpoint()));

        log.info("[ScimDynamicEndpointInitializer] Done.");
    }
}
