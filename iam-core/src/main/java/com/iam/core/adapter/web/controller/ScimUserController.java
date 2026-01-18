package com.iam.core.adapter.web.controller;

import com.iam.core.application.dto.ScimListResponse;
import com.iam.core.application.dto.ScimUserResponse;
import com.iam.core.application.service.ScimResourceService;
import com.iam.core.application.service.UserQueryService;
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

    @GetMapping
    public ScimListResponse<ScimUserResponse> getUsers() {
        return userQueryService.getAllUsers();
    }

    @GetMapping("/{id}")
    public ScimUserResponse getUser(@PathVariable Long id) {
        return userQueryService.getUserById(id);
    }

    @PostMapping
    public ScimUserResponse createUser(@RequestBody Map<String, Object> scimUser) {
        return scimResourceService.createUser(scimUser);
    }

    @PutMapping("/{id}")
    public ScimUserResponse updateUser(@PathVariable Long id, @RequestBody Map<String, Object> scimUser) {
        return scimResourceService.updateUser(id, scimUser);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable Long id) {
        scimResourceService.deleteUser(id);
    }
}
