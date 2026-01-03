package com.iam.core.application.service;

import com.iam.core.domain.entity.TransMapping;
import com.iam.core.domain.entity.TransRuleVersion;
import com.iam.core.domain.repository.TransMappingRepository;
import com.iam.core.domain.repository.TransRuleVersionRepository;
import com.iam.core.domain.vo.UniversalData;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final TransRuleVersionRepository ruleVersionRepository;
    private final RuleScriptEngine ruleScriptEngine;
    private final UniversalMapper universalMapper;

    /**
     * Transforms raw data using configured rules for the system.
     *
     * @param systemId The source system ID (e.g., "SAP_HR")
     * @param rawData  Raw data snapshot from the source
     * @return Transformed data as a Map of attribute names to UniversalData
     */
    @Transactional(readOnly = true)
    public Map<String, UniversalData> transform(String systemId, Map<String, Object> rawData) {
        log.info("Starting transformation for system: {}", systemId);

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
                TransRuleVersion currentVersion = ruleVersionRepository.findByRuleIdAndIsCurrentTrue(ruleId)
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
                if (mapping.getIsMandatory()) {
                    throw new RuntimeException("Mandatory rule execution failed: " + ruleId, e);
                }
            }
        }

        return targetMap;
    }
}
