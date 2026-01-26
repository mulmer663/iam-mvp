package com.iam.core.config.init;

import com.iam.core.adapter.web.ScimEndpointManager;
import com.iam.core.domain.common.constant.ScimEndpointConstants;
import com.iam.core.domain.scim.ScimResourceTypeMetaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 서버 시작 시 DB에 등록된 동적 리소스 타입 엔드포인트를 매핑합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(2) // ScimMetaInitializer(1) 이후 실행
public class ScimDynamicEndpointInitializer implements CommandLineRunner {

    private final ScimResourceTypeMetaRepository repository;
    private final ScimEndpointManager endpointManager;

    @Override
    public void run(String... args) {
        log.info("🚀 [ScimDynamicEndpointInitializer] 동적 리소스 엔드포인트 등록을 시작합니다...");
        
        repository.findAll().stream()
                .filter(meta -> !ScimEndpointConstants.isCoreType(meta.getId()))
                .forEach(meta -> {
                    endpointManager.register(meta.getId(), meta.getEndpoint());
                });
                
        log.info("✅ [ScimDynamicEndpointInitializer] 동적 리소스 엔드포인트 등록을 완료했습니다.");
    }
}
