package com.iam.registry.interfaces.rest.scim;

import com.iam.registry.application.scim.ScimDynamicResourceService;
import com.iam.registry.application.scim.ScimSearchRequest;
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

        String pluralType = extractPluralType(uri);
        String resourceType = toSingular(pluralType);
        String id = extractId(uri, pluralType);

        log.info("Dynamic SCIM Request: {} {} (Type: {}, ID: {})", method, uri, resourceType, id);

        return switch (method) {
            case "GET" -> id == null
                    ? ResponseEntity.ok(resourceService.listResources(resourceType, buildSearchRequest(request)))
                    : ResponseEntity.ok(resourceService.getResource(resourceType, id));
            case "POST"   -> ResponseEntity.status(201).body(resourceService.createResource(resourceType, body));
            case "PUT"    -> ResponseEntity.ok(resourceService.updateResource(resourceType, id, body));
            case "DELETE" -> {
                resourceService.deleteResource(resourceType, id);
                yield ResponseEntity.noContent().build();
            }
            default -> ResponseEntity.status(405).build();
        };
    }

    private ScimSearchRequest buildSearchRequest(HttpServletRequest request) {
        String filter = request.getParameter("filter");
        String startIndexParam = request.getParameter("startIndex");
        String countParam = request.getParameter("count");

        Integer startIndex = startIndexParam != null ? parseIntSafe(startIndexParam) : null;
        Integer count = countParam != null ? parseIntSafe(countParam) : null;

        return ScimSearchRequest.of(filter, startIndex, count);
    }

    private Integer parseIntSafe(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
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
