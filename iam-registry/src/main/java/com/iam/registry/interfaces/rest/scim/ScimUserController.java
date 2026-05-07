package com.iam.registry.interfaces.rest.scim;

import com.iam.registry.application.common.ScimListResponse;
import com.iam.registry.application.common.ScimPatchRequest;
import com.iam.registry.application.common.ScimUserResponse;
import com.iam.registry.application.scim.ScimPatchService;
import com.iam.registry.application.scim.ScimResourceService;
import com.iam.registry.application.scim.ScimSearchRequest;
import com.iam.registry.application.user.UserQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/scim/v2/Users")
@RequiredArgsConstructor
public class ScimUserController {

    private final UserQueryService userQueryService;
    private final ScimResourceService scimResourceService;
    private final ScimPatchService scimPatchService;

    @GetMapping
    public ScimListResponse<ScimUserResponse> getUsers(
            @RequestParam(required = false) String filter,
            @RequestParam(required = false) Integer startIndex,
            @RequestParam(required = false) Integer count) {
        return userQueryService.getUsers(ScimSearchRequest.of(filter, startIndex, count));
    }

    @GetMapping("/{id}")
    public ScimUserResponse getUser(@PathVariable Long id) {
        return userQueryService.getUserById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ScimUserResponse createUser(@RequestBody Map<String, Object> scimUser) {
        return scimResourceService.createUser(scimUser);
    }

    @PutMapping("/{id}")
    public ScimUserResponse updateUser(@PathVariable Long id, @RequestBody Map<String, Object> scimUser) {
        return scimResourceService.updateUser(id, scimUser);
    }

    @PatchMapping("/{id}")
    public ScimUserResponse patchUser(@PathVariable Long id, @RequestBody ScimPatchRequest patch) {
        return scimPatchService.patch(id, patch);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable Long id) {
        scimResourceService.deleteUser(id);
    }
}
