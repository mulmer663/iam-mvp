package com.iam.registry.interfaces.rest;

import com.iam.registry.domain.common.constant.ScimEndpointConstants;
import com.iam.registry.interfaces.rest.scim.GenericScimController;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScimEndpointManager {

    private final RequestMappingHandlerMapping handlerMapping;
    private final GenericScimController genericScimController;
    private final Map<String, RequestMappingInfo> registeredMappings = new ConcurrentHashMap<>();

    public void register(String resourceType, String endpoint) {
        if (ScimEndpointConstants.isCoreType(resourceType)) {
            log.info("Skipping dynamic registration for core resource type: {}", resourceType);
            return;
        }

        String fullPath = "/scim/v2" + endpoint;
        if (registeredMappings.containsKey(fullPath)) {
            log.debug("Endpoint {} is already registered", fullPath);
            return;
        }

        try {
            Method handleMethod = GenericScimController.class.getMethod("handleRequest", HttpServletRequest.class, Map.class);

            RequestMappingInfo mappingInfo = RequestMappingInfo
                    .paths(fullPath, fullPath + "/{id}", fullPath + "/**")
                    .methods(RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.PATCH)
                    .build();

            handlerMapping.registerMapping(mappingInfo, genericScimController, handleMethod);
            registeredMappings.put(fullPath, mappingInfo);

            log.info("Successfully registered dynamic SCIM endpoint: {} for type: {}", fullPath, resourceType);
        } catch (NoSuchMethodException e) {
            log.error("Failed to find handleRequest method in GenericScimController", e);
        } catch (Exception e) {
            log.error("Failed to register dynamic endpoint: {}", fullPath, e);
        }
    }

    public void unregister(String endpoint) {
        String fullPath = "/scim/v2" + endpoint;
        RequestMappingInfo mappingInfo = registeredMappings.remove(fullPath);
        if (mappingInfo != null) {
            handlerMapping.unregisterMapping(mappingInfo);
            log.info("Unregistered dynamic SCIM endpoint: {}", fullPath);
        }
    }
}
