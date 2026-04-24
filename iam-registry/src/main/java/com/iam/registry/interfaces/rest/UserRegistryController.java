package com.iam.registry.interfaces.rest;

import com.iam.registry.application.UserRegistryService;
import com.iam.registry.domain.user.IamUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/api/v1/users")
@RequiredArgsConstructor
public class UserRegistryController {

    private final UserRegistryService userRegistryService;

    @PostMapping
    public ResponseEntity<IamUser> createUser(@RequestBody IamUser user) {
        return ResponseEntity.ok(userRegistryService.saveUser(user));
    }
}
