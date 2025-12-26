package com.iam.core.adapter.web.controller;

import com.iam.core.application.dto.ScimListResponse;
import com.iam.core.application.dto.ScimUserResponse;
import com.iam.core.application.service.UserQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/scim/v2/Users")
@RequiredArgsConstructor
public class ScimUserController {

    private final UserQueryService userQueryService;

    @GetMapping
    public ScimListResponse<ScimUserResponse> getUsers() {
        return userQueryService.getAllUsers();
    }

    @GetMapping("/{id}")
    public ScimUserResponse getUser(@PathVariable Long id) {
        return userQueryService.getUserById(id);
    }
}
