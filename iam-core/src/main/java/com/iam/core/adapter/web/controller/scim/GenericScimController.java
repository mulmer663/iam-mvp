package com.iam.core.adapter.web.controller.scim;

import com.iam.core.application.scim.ScimDynamicResourceService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
public class GenericScimController {

    private final ScimDynamicResourceService resourceService;

    public ResponseEntity<?> handleRequest(HttpServletRequest request,
            @RequestBody(required = false) Map<String, Object> body) {
        String method = request.getMethod();
        String uri = request.getRequestURI();

        // Extract resourceType and ID from URI
        // Example: /scim/v2/Devices -> type: Device, id: null
        // Example: /scim/v2/Devices/123 -> type: Device, id: 123
        String pluralType = extractPluralType(uri);
        String resourceType = toSingular(pluralType);
        String id = extractId(uri, pluralType);

        log.info("Dynamic SCIM Request: {} {} (Type: {}, ID: {})", method, uri, resourceType, id);

        return switch (method) {
            case "GET" -> id == null ? ResponseEntity.ok(Map.of("totalResults", 0, "Resources", java.util.List.of()))
                    : ResponseEntity.ok(resourceService.getResource(resourceType, id));
            case "POST" -> ResponseEntity.status(201).body(resourceService.createResource(resourceType, body));
            case "PUT" -> ResponseEntity.ok(resourceService.updateResource(resourceType, id, body));
            case "DELETE" -> {
                resourceService.deleteResource(resourceType, id);
                yield ResponseEntity.noContent().build();
            }
            default -> ResponseEntity.status(405).build();
        };
    }

    private String extractPluralType(String uri) {
        if (uri == null || !uri.contains("/scim/v2/"))
            return "Unknown";
        String afterPrefix = uri.substring(uri.indexOf("/scim/v2/") + 9);
        int nextSlash = afterPrefix.indexOf("/");
        return nextSlash == -1 ? afterPrefix : afterPrefix.substring(0, nextSlash);
    }

    private String toSingular(String plural) {
        if (plural.endsWith("ies"))
            return plural.substring(0, plural.length() - 3) + "y";
        if (plural.endsWith("s"))
            return plural.substring(0, plural.length() - 1);
        return plural;
    }

    private String extractId(String uri, String pluralType) {
        String pattern = "/scim/v2/" + pluralType + "/";
        int index = uri.indexOf(pattern);
        if (index == -1)
            return null;
        return uri.substring(index + pattern.length());
    }
}
