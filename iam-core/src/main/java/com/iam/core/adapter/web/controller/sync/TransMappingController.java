package com.iam.core.adapter.web.controller.sync;

import com.iam.core.application.sync.TransMappingService;
import com.iam.core.domain.sync.TransFieldMapping;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/rules")
@RequiredArgsConstructor
public class TransMappingController {

    private final TransMappingService mappingService;

    @GetMapping("/{ruleId}/mappings")
    public ResponseEntity<List<TransFieldMappingDto>> getMappings(@PathVariable String ruleId) {
        List<TransFieldMapping> mappings = mappingService.getMappings(ruleId);
        return ResponseEntity.ok(mappings.stream()
                .map(this::toDto)
                .toList());
    }

    @PostMapping("/{ruleId}/mappings")
    public ResponseEntity<TransFieldMappingDto> saveMapping(
            @PathVariable String ruleId,
            @Valid @RequestBody TransFieldMappingDto dto) {

        TransFieldMapping entity = toEntity(ruleId, dto);
        TransFieldMapping saved = mappingService.saveMapping(entity);
        return ResponseEntity.ok(toDto(saved));
    }

    @PostMapping("/{ruleId}/mappings/batch")
    public ResponseEntity<List<TransFieldMappingDto>> saveMappings(
            @PathVariable String ruleId,
            @Valid @RequestBody List<TransFieldMappingDto> dtos) {

        List<TransFieldMapping> entities = dtos.stream()
                .map(dto -> toEntity(ruleId, dto))
                .toList();
        List<TransFieldMapping> saved = mappingService.saveMappings(ruleId, entities);
        return ResponseEntity.ok(saved.stream()
                .map(this::toDto)
                .toList());
    }

    @GetMapping("/history")
    public ResponseEntity<List<TransFieldMappingDto>> getMappingsHistory(
            @RequestParam String systemId,
            @RequestParam Long revId) {

        // 서비스에서 해당 리비전의 엔티티 리스트를 가져옴
        List<TransFieldMapping> historyMappings = mappingService.getMappingsAtRevision(systemId, revId);

        // 기존 toDto 메서드를 사용하여 DTO 리스트로 변환하여 반환
        return ResponseEntity.ok(historyMappings.stream()
                .map(this::toDto)
                .toList());
    }

    @DeleteMapping("/mappings/{mappingId}")
    public ResponseEntity<Void> deleteMapping(@PathVariable Long mappingId) {
        mappingService.deleteMapping(mappingId);
        return ResponseEntity.noContent().build();
    }

    private TransFieldMappingDto toDto(TransFieldMapping entity) {
        return new TransFieldMappingDto(
                entity.getId(),
                entity.getSourceField(),
                entity.getTargetField(),
                entity.getIsRequired(),
                entity.getMinLength(),
                entity.getMaxLength(),
                entity.getTransformType(),
                entity.getTransformParams(),
                entity.getCodeGroupId(),
                entity.getDefaultValue(),
                entity.getTransformScript());
    }

    private TransFieldMapping toEntity(String ruleId, TransFieldMappingDto dto) {
        return TransFieldMapping.builder()
                .id(dto.id())
                .ruleId(ruleId)
                .sourceField(dto.sourceField())
                .targetField(dto.targetField())
                .isRequired(dto.isRequired())
                .minLength(dto.minLength())
                .maxLength(dto.maxLength())
                .transformType(dto.transformType() != null ? dto.transformType() : "DIRECT")
                .transformParams(dto.transformParams())
                .codeGroupId(dto.codeGroupId())
                .defaultValue(dto.defaultValue())
                .transformScript(dto.transformScript())
                .build();
    }

    public record TransFieldMappingDto(
            Long id,
            String sourceField,
            String targetField,
            Boolean isRequired,
            Integer minLength,
            Integer maxLength,
            String transformType,
            String transformParams,
            String codeGroupId,
            String defaultValue,
            String transformScript) {
    }
}
