package com.iam.core.adapter.web.controller;

import com.iam.core.application.dto.IamAttributeMetaDto;
import com.iam.core.application.service.IamAttributeMetaService;
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
        return service.createAttribute(dto);
    }

    @PutMapping("/{name}")
    public IamAttributeMetaDto updateAttribute(@PathVariable String name, @RequestBody IamAttributeMetaDto dto) {
        return service.updateAttribute(name, dto);
    }

    @DeleteMapping("/{name}")
    public void deleteAttribute(@PathVariable String name) {
        service.deleteAttribute(name);
    }
}
