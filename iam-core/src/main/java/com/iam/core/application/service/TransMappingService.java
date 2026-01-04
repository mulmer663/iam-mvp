package com.iam.core.application.service;

import com.iam.core.domain.entity.*;
import com.iam.core.domain.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransMappingService {

    private final TransFieldMappingRepository fieldMappingRepository;
    private final TransRuleMetaRepository ruleMetaRepository;
    private final TransMappingRepository transMappingRepository;
    private final TransCodeMetaRepository codeMetaRepository;
    private final TransCodeValueRepository codeValueRepository;
    private final RuleScriptGenerator scriptGenerator;

    public long countRuleMeta() {
        return ruleMetaRepository.count();
    }

    @Transactional
    public TransRuleMeta saveRuleMeta(TransRuleMeta meta) {
        return ruleMetaRepository.save(meta);
    }

    @Transactional
    public TransMapping saveTransMapping(TransMapping mapping) {
        return transMappingRepository.save(mapping);
    }

    @Transactional
    public TransCodeMeta saveCodeMeta(TransCodeMeta meta) {
        return codeMetaRepository.save(meta);
    }

    @Transactional
    public void saveCodeValues(List<TransCodeValue> values) {
        codeValueRepository.saveAll(values);
    }

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

        // 2. 해당 Rule의 메인 Version 엔티티를 찾거나 새로 생성
        TransRuleMeta version = ruleMetaRepository.findById(ruleId)
                .orElse(new TransRuleMeta());

        // 3. 내용 업데이트
        version.setRuleId(ruleId);
        version.setScriptContent(generatedScript);
        version.setScriptHash(calculateHash(generatedScript));

        // 4. 저장 (이 순간 REVINFO와 TRANS_RULE_VERSION_AUD가 동시에 쌓임)
        ruleMetaRepository.save(version);
    }

    private String calculateHash(String scriptContent) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(scriptContent.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (Exception e) {
            throw new RuntimeException("Hash calculation failed", e);
        }
    }
}
