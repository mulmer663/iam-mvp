package com.iam.registry.interfaces.rest;

import com.iam.registry.application.common.IamAttributeMetaDto;
import com.iam.registry.application.scim.IamAttributeMetaService;
import com.iam.registry.domain.common.enums.AttributeTargetDomain;
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

    // PK is composite (domain, name); the path mirrors that.
    @PutMapping("/{domain}/{name}")
    public IamAttributeMetaDto updateAttribute(@PathVariable AttributeTargetDomain domain,
                                                @PathVariable String name,
                                                @RequestBody IamAttributeMetaDto dto) {
        return service.updateAttribute(name, domain, dto);
    }

    @DeleteMapping("/{domain}/{name}")
    public void deleteAttribute(@PathVariable AttributeTargetDomain domain,
                                 @PathVariable String name) {
        service.deleteAttribute(name, domain);
    }
}
