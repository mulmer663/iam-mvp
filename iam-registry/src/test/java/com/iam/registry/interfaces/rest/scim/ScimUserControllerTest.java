package com.iam.registry.interfaces.rest.scim;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iam.registry.application.common.ScimListResponse;
import com.iam.registry.application.common.ScimUserResponse;
import com.iam.registry.application.scim.ScimPatchService;
import com.iam.registry.application.scim.ScimResourceService;
import com.iam.registry.application.scim.ScimSearchRequest;
import com.iam.registry.application.user.UserQueryService;
import com.iam.registry.domain.common.exception.ErrorCode;
import com.iam.registry.domain.common.exception.IamBusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ScimUserController.class)
@DisplayName("ScimUserController")
class ScimUserControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    UserQueryService userQueryService;

    @MockitoBean
    ScimResourceService scimResourceService;

    @MockitoBean
    ScimPatchService scimPatchService;

    private static final String BASE_URL = "/scim/v2/Users";

    @Nested
    @DisplayName("성공 케이스")
    class Success {

        @Test
        @DisplayName("GET /scim/v2/Users → 200 + ListResponse 스키마")
        void getUsers_noParams_returns200() throws Exception {
            ScimListResponse<ScimUserResponse> response =
                    ScimListResponse.paged(List.of(), 0, 1);
            when(userQueryService.getUsers(any())).thenReturn(response);

            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.schemas[0]")
                            .value("urn:ietf:params:scim:api:messages:2.0:ListResponse"))
                    .andExpect(jsonPath("$.totalResults").value(0))
                    .andExpect(jsonPath("$.startIndex").value(1));
        }

        @Test
        @DisplayName("GET /scim/v2/Users?filter=... → filter 파라미터가 서비스로 전달")
        void getUsers_withFilter_forwardsToService() throws Exception {
            when(userQueryService.getUsers(any()))
                    .thenReturn(ScimListResponse.paged(List.of(), 0, 1));

            mockMvc.perform(get(BASE_URL).param("filter", "userName eq \"john\""))
                    .andExpect(status().isOk());

            ArgumentCaptor<ScimSearchRequest> captor = ArgumentCaptor.forClass(ScimSearchRequest.class);
            verify(userQueryService).getUsers(captor.capture());
            assertThat(captor.getValue().filter()).isEqualTo("userName eq \"john\"");
        }

        @Test
        @DisplayName("GET /scim/v2/Users?startIndex=5&count=10 → startIndex/count 서비스로 전달")
        void getUsers_withPaging_forwardsToService() throws Exception {
            when(userQueryService.getUsers(any()))
                    .thenReturn(ScimListResponse.paged(List.of(), 0, 5));

            mockMvc.perform(get(BASE_URL)
                            .param("startIndex", "5")
                            .param("count", "10"))
                    .andExpect(status().isOk());

            ArgumentCaptor<ScimSearchRequest> captor = ArgumentCaptor.forClass(ScimSearchRequest.class);
            verify(userQueryService).getUsers(captor.capture());
            assertThat(captor.getValue().startIndex()).isEqualTo(5);
            assertThat(captor.getValue().count()).isEqualTo(10);
        }

        @Test
        @DisplayName("GET /scim/v2/Users?count=0 → isCountOnly 요청 전달")
        void getUsers_countZero_forwardedToService() throws Exception {
            when(userQueryService.getUsers(any()))
                    .thenReturn(ScimListResponse.countOnly(42, 1));

            mockMvc.perform(get(BASE_URL).param("count", "0"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalResults").value(42))
                    .andExpect(jsonPath("$.itemsPerPage").value(0));

            ArgumentCaptor<ScimSearchRequest> captor = ArgumentCaptor.forClass(ScimSearchRequest.class);
            verify(userQueryService).getUsers(captor.capture());
            assertThat(captor.getValue().isCountOnly()).isTrue();
        }
    }

    @Nested
    @DisplayName("경계선 케이스")
    class Boundary {

        @Test
        @DisplayName("파라미터 없음 → startIndex=1, count=100 기본값 적용")
        void getUsers_noParams_defaultsApplied() throws Exception {
            when(userQueryService.getUsers(any()))
                    .thenReturn(ScimListResponse.paged(List.of(), 0, 1));

            mockMvc.perform(get(BASE_URL)).andExpect(status().isOk());

            ArgumentCaptor<ScimSearchRequest> captor = ArgumentCaptor.forClass(ScimSearchRequest.class);
            verify(userQueryService).getUsers(captor.capture());
            ScimSearchRequest req = captor.getValue();
            assertThat(req.startIndex()).isEqualTo(1);
            assertThat(req.count()).isEqualTo(100);
        }

        @Test
        @DisplayName("count=201 → 200으로 클램핑되어 서비스 전달")
        void getUsers_countOver200_clamped() throws Exception {
            when(userQueryService.getUsers(any()))
                    .thenReturn(ScimListResponse.paged(List.of(), 0, 1));

            mockMvc.perform(get(BASE_URL).param("count", "201")).andExpect(status().isOk());

            ArgumentCaptor<ScimSearchRequest> captor = ArgumentCaptor.forClass(ScimSearchRequest.class);
            verify(userQueryService).getUsers(captor.capture());
            assertThat(captor.getValue().count()).isEqualTo(200);
        }

        @Test
        @DisplayName("startIndex=0 → 1로 정규화되어 서비스 전달")
        void getUsers_startIndexZero_normalized() throws Exception {
            when(userQueryService.getUsers(any()))
                    .thenReturn(ScimListResponse.paged(List.of(), 0, 1));

            mockMvc.perform(get(BASE_URL).param("startIndex", "0")).andExpect(status().isOk());

            ArgumentCaptor<ScimSearchRequest> captor = ArgumentCaptor.forClass(ScimSearchRequest.class);
            verify(userQueryService).getUsers(captor.capture());
            assertThat(captor.getValue().startIndex()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("비즈니스 Fail 케이스")
    class Fail {

        @Test
        @DisplayName("잘못된 SCIM filter → 400 + RFC 7644 §3.12 에러 형식")
        void getUsers_invalidFilter_returns400ScimError() throws Exception {
            when(userQueryService.getUsers(any()))
                    .thenThrow(new IamBusinessException(
                            ErrorCode.INVALID_SCIM_FILTER, "TEST", "잘못된 필터"));

            mockMvc.perform(get(BASE_URL).param("filter", "INVALID_FILTER"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.schemas[0]")
                            .value("urn:ietf:params:scim:api:messages:2.0:Error"))
                    .andExpect(jsonPath("$.scimType").value("invalidFilter"))
                    .andExpect(jsonPath("$.status").value("400"))
                    .andExpect(jsonPath("$.detail").value("잘못된 필터"));
        }
    }
}
