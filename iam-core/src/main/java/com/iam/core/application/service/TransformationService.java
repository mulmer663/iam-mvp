package com.iam.core.application.service;

import com.iam.core.application.dto.TransformationResult;
import com.iam.core.domain.entity.TransMapping;
import com.iam.core.domain.exception.RuleEngineException;
import com.iam.core.domain.exception.TransformationException;
import com.iam.core.domain.repository.TransMappingRepository;
import com.iam.core.domain.repository.TransRuleMetaRepository;
import com.iam.core.domain.vo.UniversalData;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Orchestrates the transformation rules execution.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TransformationService {

    private final TransMappingRepository mappingRepository;
    private final TransRuleMetaRepository transRuleMetaRepository;
    private final RuleScriptEngine ruleScriptEngine;
    private final UniversalMapper universalMapper;

    @PersistenceContext
    private final EntityManager entityManager;

    /**
     * Transforms raw data using configured rules for the system.
     *
     * @param systemId The source system ID (e.g., "SAP_HR")
     * @param rawData  Raw data snapshot from the source
     * @return Transformed data as a Map of attribute names to UniversalData
     */
    @Transactional(readOnly = true)
    public TransformationResult transform(String systemId, Map<String, Object> rawData) {
        log.info("Starting transformation for system: {}", systemId);
        // Envers를 통해 현재 시점의 최신 리비전 번호를 가져옵니다.
        AuditReader reader = AuditReaderFactory.get(entityManager);
        long currentRevId;
        try {
            // 2. 현재 시각(new Date()) 기준으로 유효한 가장 마지막 리비전 번호를 획득
            Number rev = reader.getRevisionNumberForDate(new Date());
            currentRevId = rev.longValue();
        } catch (Exception e) {
            // 데이터가 하나도 없는 초기 상태이거나 리비전이 없을 경우 예외 처리
            log.warn("No revision found, defaulting to 0");
            currentRevId = 1L;
        }

        // 1. Initial State: Map common attributes or keep it empty
        Map<String, UniversalData> sourceMap = universalMapper.map(rawData);
        Map<String, UniversalData> targetMap = new HashMap<>(sourceMap);

        // 2. Load Mappings
        List<TransMapping> mappings = mappingRepository.findBySystemIdOrderByExecOrderAsc(systemId);
        log.info("Found {} rule mappings for system: {}", mappings.size(), systemId);

        // 3. Execute Rules Sequentially
        for (TransMapping mapping : mappings) {
            String ruleId = mapping.getRuleId();
            try {
                var currentVersion = transRuleMetaRepository.findById(ruleId)
                        .orElseThrow(() -> new RuntimeException("Active rule version not found: " + ruleId));

                log.debug("Executing rule: {} (order: {})", ruleId, mapping.getExecOrder());

                Map<String, Object> params = new HashMap<>();
                params.put("source", sourceMap);
                params.put("target", targetMap);

                Object result = ruleScriptEngine.execute(
                        currentVersion.getScriptContent(),
                        currentVersion.getScriptHash(),
                        params);

                if (result instanceof Map<?, ?> resultMap) {
                    log.debug("Rule result: {}", resultMap);
                    resultMap.forEach((k, v) -> {
                        if (k instanceof String key) {
                            if (v instanceof UniversalData val) {
                                targetMap.put(key, val);
                            } else {
                                targetMap.put(key, universalMapper.toUniversalData(v));
                            }
                        }
                    });
                }

            } catch (Exception e) {
                log.error("Failed to execute rule: {}. Reason: {}", ruleId, e.getMessage());

                if (mapping.getIsMandatory() && e instanceof RuleEngineException ree) {
                    throw new TransformationException(ree.getErrorCode(), ree.getMessage(), ruleId, currentRevId, e);
                }
            }
        }

        return new TransformationResult(targetMap, currentRevId);
    }
}
