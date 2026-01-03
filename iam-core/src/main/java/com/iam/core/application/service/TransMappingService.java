package com.iam.core.application.service;

import com.iam.core.domain.entity.TransFieldMapping;
import com.iam.core.domain.entity.TransRuleVersion;
import com.iam.core.domain.repository.TransFieldMappingRepository;
import com.iam.core.domain.repository.TransRuleVersionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransMappingService {

    private final TransFieldMappingRepository fieldMappingRepository;
    private final TransRuleVersionRepository ruleVersionRepository;
    private final RuleScriptGenerator scriptGenerator;

    public List<TransFieldMapping> getMappings(String ruleId) {
        return fieldMappingRepository.findByRuleId(ruleId);
    }

    @Transactional
    public TransFieldMapping saveMapping(TransFieldMapping mapping) {
        TransFieldMapping saved = fieldMappingRepository.save(mapping);
        if (mapping.getRuleId() != null) {
            refreshRuleScript(mapping.getRuleId());
        }
        return saved;
    }

    @Transactional
    public void deleteMapping(Long mappingId) {
        fieldMappingRepository.findById(mappingId).ifPresent(mapping -> {
            String ruleId = mapping.getRuleId();
            fieldMappingRepository.deleteById(mappingId);
            refreshRuleScript(ruleId);
        });
    }

    private void refreshRuleScript(String ruleId) {
        log.info("Refreshing script for rule: {}", ruleId);
        List<TransFieldMapping> mappings = fieldMappingRepository.findByRuleId(ruleId);
        String generatedScript = scriptGenerator.generate(mappings);

        // Deactivate current version
        ruleVersionRepository.findByRuleIdAndIsCurrentTrue(ruleId).ifPresent(v -> {
            v.setIsCurrent(false);
            ruleVersionRepository.save(v);
        });

        List<TransRuleVersion> allVersions = ruleVersionRepository.findAll();
        // Get max version number for this rule
        Integer nextVer = allVersions.stream()
                .filter(v -> ruleId.equals(v.getRuleId()))
                .map(TransRuleVersion::getVersionNo)
                .max(Integer::compareTo)
                .orElse(0) + 1;

        // Create new version
        TransRuleVersion newVersion = TransRuleVersion.builder()
                .ruleId(ruleId)
                .versionNo(nextVer)
                .scriptContent(generatedScript)
                .scriptHash(UUID.randomUUID().toString()) // Real hash could be SHA-256
                .changeLog("Auto-generated from field mappings")
                .isCurrent(true)
                .createdBy("SYSTEM")
                .build();

        ruleVersionRepository.save(newVersion);
    }
}
