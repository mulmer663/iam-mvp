package com.iam.core.application.sync;

import com.iam.core.domain.common.exception.ErrorCode;
import com.iam.core.domain.common.exception.IamBusinessException;
import com.iam.core.domain.sync.*;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.query.AuditEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;

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
    private final EntityManager entityManager;

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
    public List<TransFieldMapping> saveMappings(String ruleId, List<TransFieldMapping> mappings) {
        log.info("Batch saving {} mappings for rule: {}", mappings.size(), ruleId);
        mappings.forEach(m -> m.setRuleId(ruleId));
        List<TransFieldMapping> saved = fieldMappingRepository.saveAll(mappings);
        refreshRuleScript(ruleId);
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

    /**
     * 특정 시스템의 특정 리비전 시점 컬럼 매핑 리스트 조회
     */
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<TransFieldMapping> getMappingsAtRevision(String systemId, Long revId) {
        // 1. 해당 시스템에 매핑된 ruleId 조회 (TransMapping 엔티티 활용)
        TransMapping mapping = transMappingRepository.findBySystemId(systemId)
                .orElseThrow(
                        () -> new IamBusinessException(ErrorCode.RESOURCE_NOT_FOUND, systemId, "시스템 설정을 찾을 수 없습니다."));

        String ruleId = mapping.getRuleId();

        // 2. AuditReader를 통해 특정 리비전의 TransFieldMapping 리스트 조회
        AuditReader reader = AuditReaderFactory.get(entityManager);

        // ruleId가 일치하는 엔티티들을 특정 리비전 시점에서 필터링하여 가져옴
        return (List<TransFieldMapping>) reader.createQuery()
                .forEntitiesAtRevision(TransFieldMapping.class, revId)
                .add(AuditEntity.property("ruleId").eq(ruleId))
                .getResultList();
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
