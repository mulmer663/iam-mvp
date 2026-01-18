package com.iam.core.adapter.web.controller;

import com.iam.core.application.dto.IamAttributeMetaDto;
import com.iam.core.application.service.IamAttributeMetaService;
import com.iam.core.domain.entity.IamAttributeMeta;
import com.iam.core.domain.enums.AttributeTargetDomain;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/attributes")
@RequiredArgsConstructor
public class IamAttributeMetaController {
    private final IamAttributeMetaService service;

    @GetMapping
    public List<IamAttributeMetaDto> getAttributes(
            @RequestParam(required = false) AttributeTargetDomain domain) {
        return service.getAttributes(domain);
    }

    @PostMapping
    public IamAttributeMetaDto createAttribute(@RequestBody IamAttributeMetaDto dto) {
        // Converting DTO to Entity for creation (or can move mapping logic to service)
        // For simplicity here, assuming manually mapping or adding helper method.
        // Ideally service accepts DTO or we map here.
        // Let's modify Service to accept DTO or map here.
        IamAttributeMeta entity = new IamAttributeMeta(
                dto.code(),
                dto.targetDomain(),
                dto.category(),
                dto.displayName(),
                dto.dataType(),
                dto.scimSchemaUri(),
                dto.description(),
                dto.required(),
                dto.mutability(),
                dto.adminOnly(),
                dto.viewLevel(),
                dto.editLevel(),
                dto.encrypted(),
                dto.uiComponent());
        return service.createAttribute(entity);
    }

    @PutMapping("/{code}")
    public IamAttributeMetaDto updateAttribute(@PathVariable String code, @RequestBody IamAttributeMetaDto dto) {
        return service.updateAttribute(code, dto);
    }

    @DeleteMapping("/{code}")
    public void deleteAttribute(@PathVariable String code) {
        service.deleteAttribute(code);
    }
}
