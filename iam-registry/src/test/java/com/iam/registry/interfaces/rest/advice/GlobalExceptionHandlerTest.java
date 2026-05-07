package com.iam.registry.interfaces.rest.advice;

import com.iam.registry.domain.common.exception.ErrorCode;
import com.iam.registry.domain.common.exception.IamBusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * GlobalExceptionHandler를 포함한 슬라이스 테스트.
 * StubController로 각 예외 경로를 직접 유발.
 */
@WebMvcTest({GlobalExceptionHandler.class, GlobalExceptionHandlerTest.StubController.class})
@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    @Autowired
    MockMvc mockMvc;

    // ── 테스트용 컨트롤러 ──────────────────────────────────────────────────────

    @RestController
    static class StubController {

        @GetMapping("/scim/v2/stub/business-error")
        public void scimBusinessError() {
            throw new IamBusinessException(ErrorCode.INVALID_SCIM_FILTER, "T-001", "잘못된 필터 표현식");
        }

        @GetMapping("/scim/v2/stub/not-found")
        public void scimNotFound() {
            throw new IamBusinessException(ErrorCode.RESOURCE_NOT_FOUND, "T-002", "리소스 없음");
        }

        @GetMapping("/scim/v2/stub/conflict")
        public void scimConflict() {
            throw new IamBusinessException(ErrorCode.USER_ALREADY_EXISTS, "T-003", "이미 존재하는 사용자");
        }

        @GetMapping("/scim/v2/stub/server-error")
        public void scimServerError() {
            throw new RuntimeException("DB 연결 실패 (내부 상세)");
        }

        @PostMapping("/scim/v2/stub/validation")
        public void scimValidation(@Valid @RequestBody StubRequest body) {
        }

        @GetMapping("/api/business-error")
        public void apiBusinessError() {
            throw new IamBusinessException(ErrorCode.USER_NOT_FOUND, "T-010", "사용자 없음");
        }

        @GetMapping("/api/server-error")
        public void apiServerError() {
            throw new RuntimeException("예상치 못한 오류");
        }

        record StubRequest(@NotBlank String name) {
        }
    }

    // ── SCIM 경로 — 성공 케이스 ───────────────────────────────────────────────

    @Nested
    @DisplayName("SCIM 경로 (/scim/v2/**) — RFC 7644 §3.12 포맷")
    class ScimPath {

        @Test
        @DisplayName("IamBusinessException (INVALID_SCIM_FILTER) → 400 + scimType=invalidFilter")
        void scim_businessError_invalidFilter_returns400() throws Exception {
            mockMvc.perform(get("/scim/v2/stub/business-error"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.schemas[0]")
                            .value("urn:ietf:params:scim:api:messages:2.0:Error"))
                    .andExpect(jsonPath("$.scimType").value("invalidFilter"))
                    .andExpect(jsonPath("$.status").value("400"))
                    .andExpect(jsonPath("$.detail").value("잘못된 필터 표현식"));
        }

        @Test
        @DisplayName("IamBusinessException (RESOURCE_NOT_FOUND) → 404 + scimType 미포함 (RFC 7644 §3.12: noTarget은 PATCH 전용)")
        void scim_notFound_returns404() throws Exception {
            mockMvc.perform(get("/scim/v2/stub/not-found"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.schemas[0]")
                            .value("urn:ietf:params:scim:api:messages:2.0:Error"))
                    .andExpect(jsonPath("$.scimType").doesNotExist())
                    .andExpect(jsonPath("$.status").value("404"));
        }

        @Test
        @DisplayName("IamBusinessException (USER_ALREADY_EXISTS) → 409 + scimType=uniqueness")
        void scim_conflict_returns409() throws Exception {
            mockMvc.perform(get("/scim/v2/stub/conflict"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.scimType").value("uniqueness"))
                    .andExpect(jsonPath("$.status").value("409"));
        }

        @Test
        @DisplayName("검증 실패 (@Valid) → 400 + scimType=invalidValue")
        void scim_validationFailed_returns400() throws Exception {
            mockMvc.perform(post("/scim/v2/stub/validation")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.scimType").value("invalidValue"))
                    .andExpect(jsonPath("$.status").value("400"));
        }

        @Test
        @DisplayName("JSON 파싱 실패 → 400 + scimType=invalidSyntax")
        void scim_malformedJson_returns400() throws Exception {
            mockMvc.perform(post("/scim/v2/stub/validation")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("NOT_JSON"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.scimType").value("invalidSyntax"))
                    .andExpect(jsonPath("$.status").value("400"));
        }

        @Test
        @DisplayName("허용되지 않는 메서드 → 405 + schemas 포함")
        void scim_methodNotAllowed_returns405() throws Exception {
            mockMvc.perform(delete("/scim/v2/stub/business-error"))
                    .andExpect(status().isMethodNotAllowed())
                    .andExpect(jsonPath("$.schemas[0]")
                            .value("urn:ietf:params:scim:api:messages:2.0:Error"))
                    .andExpect(jsonPath("$.status").value("405"));
        }

        @Test
        @DisplayName("예상치 못한 예외 → 500 + detail에 traceId 포함, 내부 상세 노출 안함")
        void scim_unexpectedError_returns500_noInternalDetail() throws Exception {
            mockMvc.perform(get("/scim/v2/stub/server-error"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.schemas[0]")
                            .value("urn:ietf:params:scim:api:messages:2.0:Error"))
                    .andExpect(jsonPath("$.status").value("500"))
                    // DB 연결 실패 메시지가 클라이언트에 노출되지 않아야 함
                    .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.not(
                            org.hamcrest.Matchers.containsString("DB 연결 실패"))));
        }
    }

    // ── 비-SCIM 경로 — ErrorResponse 포맷 ───────────────────────────────────

    @Nested
    @DisplayName("비-SCIM 경로 (/api/**) — ErrorResponse 포맷")
    class NonScimPath {

        @Test
        @DisplayName("IamBusinessException → 404 + errorCode + message 포함")
        void api_businessError_returnsErrorResponse() throws Exception {
            mockMvc.perform(get("/api/business-error"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorCode").value("IAM-4100"))
                    .andExpect(jsonPath("$.message").exists())
                    .andExpect(jsonPath("$.traceId").exists())
                    .andExpect(jsonPath("$.path").value("/api/business-error"))
                    .andExpect(jsonPath("$.status").value(404));
        }

        @Test
        @DisplayName("예상치 못한 예외 → 500 + errorCode=IAM-5000")
        void api_unexpectedError_returns500() throws Exception {
            mockMvc.perform(get("/api/server-error"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.errorCode").value("IAM-5000"))
                    .andExpect(jsonPath("$.traceId").exists());
        }
    }

    // ── 경계선 케이스 ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("경계선 케이스")
    class Boundary {

        @Test
        @DisplayName("SCIM 에러 응답에 schemas 배열이 정확히 1개 원소")
        void scim_errorResponse_schemasHasExactlyOneEntry() throws Exception {
            mockMvc.perform(get("/scim/v2/stub/business-error"))
                    .andExpect(jsonPath("$.schemas").isArray())
                    .andExpect(jsonPath("$.schemas.length()").value(1));
        }

        @Test
        @DisplayName("비-SCIM 에러 응답에 schemas 필드 없음")
        void nonScim_errorResponse_noSchemasField() throws Exception {
            mockMvc.perform(get("/api/business-error"))
                    .andExpect(jsonPath("$.schemas").doesNotExist());
        }

        @Test
        @DisplayName("SCIM 5xx 응답 status 필드는 문자열 '500'")
        void scim_5xx_statusIsString() throws Exception {
            mockMvc.perform(get("/scim/v2/stub/server-error"))
                    // RFC 7644 §3.12: status는 문자열 타입
                    .andExpect(jsonPath("$.status").value("500"));
        }
    }
}
