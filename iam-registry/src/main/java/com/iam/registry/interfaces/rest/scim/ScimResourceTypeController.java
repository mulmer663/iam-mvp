package com.iam.registry.interfaces.rest.scim;

import com.iam.registry.application.common.ScimResourceTypeDto;
import com.iam.registry.application.scim.ScimResourceTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/resource-types")
@RequiredArgsConstructor
public class ScimResourceTypeController {

    private final ScimResourceTypeService service;

    @GetMapping
    public List<ScimResourceTypeDto> getAllResourceTypes() {
        return service.getAllResourceTypes();
    }

    @GetMapping("/{id}")
    public ScimResourceTypeDto getResourceType(@PathVariable String id) {
        return service.getResourceType(id);
    }

    @PostMapping
    public ScimResourceTypeDto createResourceType(@RequestBody ScimResourceTypeDto dto) {
        return service.createResourceType(dto);
    }

    @PutMapping("/{id}")
    public ScimResourceTypeDto updateResourceType(@PathVariable String id, @RequestBody ScimResourceTypeDto dto) {
        return service.updateResourceType(id, dto);
    }

    @DeleteMapping("/{id}")
    public void deleteResourceType(@PathVariable String id) {
        service.deleteResourceType(id);
    }
}
