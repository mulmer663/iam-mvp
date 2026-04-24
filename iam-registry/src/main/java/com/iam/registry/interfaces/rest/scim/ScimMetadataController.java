package com.iam.registry.interfaces.rest.scim;

import com.iam.registry.application.common.ScimListResponse;
import com.iam.registry.application.common.ScimResourceTypeResponse;
import com.iam.registry.application.common.ScimSchemaResponse;
import com.iam.registry.application.scim.ScimMetadataService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/scim/v2")
@RequiredArgsConstructor
public class ScimMetadataController {

    private final ScimMetadataService scimMetadataService;

    @GetMapping("/Schemas")
    public ScimListResponse<ScimSchemaResponse> getSchemas() {
        return new ScimListResponse<>(scimMetadataService.getAllSchemas());
    }

    @GetMapping("/Schemas/{uri}")
    public ScimSchemaResponse getSchema(@PathVariable String uri) {
        return scimMetadataService.getSchema(uri);
    }

    @GetMapping("/ResourceTypes")
    public ScimListResponse<ScimResourceTypeResponse> getResourceTypes() {
        return new ScimListResponse<>(scimMetadataService.getAllResourceTypes());
    }

    @GetMapping("/ResourceTypes/{id}")
    public ScimResourceTypeResponse getResourceType(@PathVariable String id) {
        return scimMetadataService.getResourceType(id);
    }
}
