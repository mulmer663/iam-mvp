package com.iam.connector.hr.infrastructure.persistence;

import com.iam.connector.hr.application.port.out.HrSourcePort;
import com.iam.connector.hr.domain.model.HrRecord;
import com.iam.connector.hr.infrastructure.persistence.repository.HrRawDataRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Component
@RequiredArgsConstructor
public class JpaHrSourceAdapter implements HrSourcePort {

    private final HrRawDataRepository repository;
    private final ObjectMapper objectMapper;

    @Override
    public List<HrRecord> fetchAll() {
        return repository.findAll().stream()
                .map(entity -> {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> data = objectMapper.convertValue(entity, Map.class);
                    data.put("externalId", entity.getEmpNo()); // Expose as externalId for IAM Core
                    // Standardize for hashing: TreeMap sorts keys
                    Map<String, Object> sortedData = new TreeMap<>(data);
                    String hash = calculateHash(sortedData);
                    return new HrRecord(entity.getEmpNo(), data, hash);
                })
                .toList();
    }

    private String calculateHash(Map<String, Object> data) {
        try {
            String json = objectMapper.writeValueAsString(data);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(json.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (Exception e) {
            throw new RuntimeException("Hash calculation failed", e);
        }
    }
}
