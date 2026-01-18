package com.iam.core.adapter.web.controller;

import com.iam.core.application.dto.ScimSchemaDto;
import com.iam.core.application.service.ScimSchemaService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/schemas")
@RequiredArgsConstructor
public class ScimSchemaController {

    private final ScimSchemaService service;

    @GetMapping
    public List<ScimSchemaDto> getAllSchemas() {
        return service.getAllSchemas();
    }

    @GetMapping("/{uri}")
    public ScimSchemaDto getSchema(@PathVariable String uri) {
        return service.getSchema(uri);
    }

    @PostMapping
    public ScimSchemaDto createSchema(@RequestBody ScimSchemaDto dto) {
        return service.createSchema(dto);
    }

    @PutMapping("/{uri}")
    public ScimSchemaDto updateSchema(@PathVariable String uri, @RequestBody ScimSchemaDto dto) {
        return service.updateSchema(uri, dto);
    }

    @DeleteMapping("/{uri}")
    public void deleteSchema(@PathVariable String uri) {
        service.deleteSchema(uri);
    }
}
