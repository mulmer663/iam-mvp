package com.iam.core.adapter.web.controller.scim;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iam.core.application.common.ScimResourceTypeDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ScimDynamicEndpointIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("새로운 리소스 타입을 등록하고 해당 엔드포인트에서 CRUD 라이프사이클을 수행한다 (Logical ID 사용)")
    void testDynamicResourceCrudLifecycle() throws Exception {
        // 1. 새로운 리소스 타입 (Device) 정의 및 등록
        ScimResourceTypeDto deviceType = new ScimResourceTypeDto(
                "Device",
                "Device",
                "Mobile or Laptop Device",
                "/Devices",
                "urn:ietf:params:scim:schemas:extension:device:2.0:Device",
                List.of());

        mockMvc.perform(post("/api/resource-types")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(deviceType)))
                .andExpect(status().isOk());

        // 2. 리소스 생성 (POST) - Logical ID (scimId) 지정
        String logicalId = "laptop-001";
        Map<String, Object> devicePayload = Map.of(
                "id", logicalId,
                "schemas", List.of("urn:ietf:params:scim:schemas:extension:device:2.0:Device"),
                "displayName", "Manager's Laptop",
                "serialNumber", "SN-9999",
                "externalId", "EXT-DEV-002");

        mockMvc.perform(post("/scim/v2/Devices")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(devicePayload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(logicalId)))
                .andExpect(jsonPath("$.displayName", is("Manager's Laptop")));

        // 3. 리소스 조회 (GET) - Logical ID 로 조회
        mockMvc.perform(get("/scim/v2/Devices/" + logicalId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName", is("Manager's Laptop")))
                .andExpect(jsonPath("$.meta.resourceType", is("Device")));

        // 4. 리소스 수정 (PUT)
        Map<String, Object> updatePayload = new java.util.HashMap<>(devicePayload);
        updatePayload.put("displayName", "Manager's MacBook Pro");

        mockMvc.perform(put("/scim/v2/Devices/" + logicalId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatePayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName", is("Manager's MacBook Pro")));

        // 5. 리소스 삭제 (DELETE)
        mockMvc.perform(delete("/scim/v2/Devices/" + logicalId))
                .andExpect(status().isNoContent());

        // 6. 삭제 확인 (GET -> 404)
        mockMvc.perform(get("/scim/v2/Devices/" + logicalId))
                .andExpect(status().isNotFound());

        // 7. 리소스 타입 삭제 (Cleanup)
        mockMvc.perform(delete("/api/resource-types/Device"))
                .andExpect(status().isOk());
    }
}
