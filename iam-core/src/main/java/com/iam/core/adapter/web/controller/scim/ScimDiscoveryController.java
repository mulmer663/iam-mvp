package com.iam.core.adapter.web.controller.scim;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/scim/v2")
public class ScimDiscoveryController {

    @GetMapping("/ServiceProviderConfig")
    public Map<String, Object> getServiceProviderConfig() {
        return Map.of(
                "schemas", List.of("urn:ietf:params:scim:schemas:core:2.0:ServiceProviderConfig"),
                "patch", Map.of("supported", true),
                "bulk", Map.of("supported", false, "maxOperations", 1000, "maxPayloadSize", 1048576),
                "filter", Map.of("supported", true, "maxResults", 200),
                "changePassword", Map.of("supported", false),
                "sort", Map.of("supported", false),
                "etag", Map.of("supported", false),
                "authenticationSchemes", List.of(
                        Map.of(
                                "name", "HTTP Basic",
                                "description", "Authentication scheme using HTTP Basic",
                                "specUri", "http://www.rfc-editor.org/info/rfc2617",
                                "type", "httpbasic",
                                "primary", true)));
    }
}
