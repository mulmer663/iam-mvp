package com.iam.registry.interfaces.rest.scim;

import com.iam.registry.application.scim.ScimDynamicResourceService;
import com.iam.registry.application.scim.ScimSearchRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/scim/v2/Groups")
@RequiredArgsConstructor
public class ScimGroupController {

    private static final String RESOURCE_TYPE = "Group";

    private final ScimDynamicResourceService resourceService;

    @GetMapping
    public Map<String, Object> getGroups(
            @RequestParam(required = false) String filter,
            @RequestParam(required = false) Integer startIndex,
            @RequestParam(required = false) Integer count) {
        return resourceService.listResources(RESOURCE_TYPE, ScimSearchRequest.of(filter, startIndex, count));
    }

    @GetMapping("/{id}")
    public Map<String, Object> getGroup(@PathVariable String id) {
        return resourceService.getResource(RESOURCE_TYPE, id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> createGroup(@RequestBody Map<String, Object> body) {
        return resourceService.createResource(RESOURCE_TYPE, body);
    }

    @PutMapping("/{id}")
    public Map<String, Object> updateGroup(@PathVariable String id, @RequestBody Map<String, Object> body) {
        return resourceService.updateResource(RESOURCE_TYPE, id, body);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteGroup(@PathVariable String id) {
        resourceService.deleteResource(RESOURCE_TYPE, id);
    }
}
