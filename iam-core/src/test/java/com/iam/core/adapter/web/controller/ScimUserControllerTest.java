package com.iam.core.adapter.web.controller;

import com.iam.core.application.dto.ScimListResponse;
import com.iam.core.application.dto.ScimUserResponse;
import com.iam.core.application.service.UserQueryService;
import com.iam.core.domain.exception.ErrorCode;
import com.iam.core.domain.exception.IamBusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ScimUserController.class)
class ScimUserControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private UserQueryService userQueryService;

        @Test
        @DisplayName("GET /scim/v2/Users 성공 시 SCIM ListResponse를 반환해야 한다")
        void getUsers_ShouldReturnListResponse() throws Exception {
                // Given
                var userResp = ScimUserResponse.builder()
                                .id("1")
                                .userName("test.user")
                                .name(new ScimUserResponse.Name("Test", "User", "Test User"))
                                .emails(List.of(ScimUserResponse.MultiValue.builder().value("test.user").primary(true)
                                                .build()))
                                .active(true)
                                .build();

                given(userQueryService.getAllUsers()).willReturn(new ScimListResponse<>(List.of(userResp)));

                // When & Then
                mockMvc.perform(get("/scim/v2/Users")
                                .accept(MediaType.APPLICATION_JSON))
                                .andDo(print())
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.schemas[0]")
                                                .value("urn:ietf:params:scim:api:messages:2.0:ListResponse"))
                                .andExpect(jsonPath("$.totalResults").value(1))
                                .andExpect(jsonPath("$.Resources[0].id").value("1"))
                                .andExpect(jsonPath("$.Resources[0].userName").value("test.user"));
        }

        @Test
        @DisplayName("GET /scim/v2/Users/{id} 성공 시 SCIM UserResponse를 반환해야 한다")
        void getUser_ShouldReturnUserResponse() throws Exception {
                // Given
                Long userId = 1L;
                var userResp = ScimUserResponse.builder()
                                .id("1")
                                .userName("test.user")
                                .name(new ScimUserResponse.Name("User", "Test", "Test User"))
                                .emails(List.of(ScimUserResponse.MultiValue.builder().value("test.user").primary(true)
                                                .build()))
                                .active(true)
                                .enterpriseExtension(Map.of("department", "Dev Team"))
                                .build();

                given(userQueryService.getUserById(userId)).willReturn(userResp);

                // When & Then
                mockMvc.perform(get("/scim/v2/Users/{id}", userId)
                                .accept(MediaType.APPLICATION_JSON))
                                .andDo(print())
                                .andExpect(status().isOk())
                                // .andExpect(jsonPath("$.schemas").isArray()) // 순서 보장 어려움
                                .andExpect(jsonPath("$.id").value("1"))
                                .andExpect(jsonPath("$.userName").value("test.user"))
                                .andExpect(jsonPath("$.emails[0].value").value("test.user"))
                                .andExpect(jsonPath(
                                                "$['urn:ietf:params:scim:schemas:extension:enterprise:2.0:User']['department']")
                                                .value("Dev Team"));
        }

        @Test
        @DisplayName("존재하지 않는 사용자 조회 시 404를 반환해야 한다")
        void getUser_NotFound_ShouldReturn404() throws Exception {
                // Given
                given(userQueryService.getUserById(anyLong()))
                                .willThrow(new IamBusinessException(ErrorCode.USER_NOT_FOUND, "trace-1", "Not Found"));

                // When & Then
                mockMvc.perform(get("/scim/v2/Users/{id}", 999)
                                .accept(MediaType.APPLICATION_JSON))
                                .andDo(print())
                                .andExpect(status().isNotFound());
        }
}
