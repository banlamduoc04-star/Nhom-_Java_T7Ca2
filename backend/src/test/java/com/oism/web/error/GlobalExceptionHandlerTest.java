package com.oism.web.error;

import com.oism.domain.exception.BusinessException;
import com.oism.domain.exception.ErrorCode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Kiem tra dinh dang loi thong nhat {errorCode, message, details} (khong can khoi dong Spring/DB). */
class GlobalExceptionHandlerTest {

    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new Probe())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void businessExceptionUsesItsCodeAndHttpStatus() throws Exception {
        mvc.perform(get("/api/v1/probe/conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("AU40901"))
                .andExpect(jsonPath("$.message").value("Email đã tồn tại"))
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    void accessDeniedBecomesAU40301() throws Exception {
        mvc.perform(get("/api/v1/probe/denied"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("AU40301"));
    }

    @Test
    void unexpectedErrorHidesInternalsAndReturnsSY50001() throws Exception {
        mvc.perform(get("/api/v1/probe/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorCode").value("SY50001"))
                .andExpect(jsonPath("$.message").value("Lỗi hệ thống"));
    }

    @Test
    void validationErrorListsFieldsInDetails() throws Exception {
        mvc.perform(post("/api/v1/auth/probe").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("AU40001"))
                .andExpect(jsonPath("$.details[0].field").value("name"));
    }

    @Test
    void validationErrorUsesProductPrefixForProductEndpoints() throws Exception {
        mvc.perform(post("/api/v1/products/probe").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("PR40001"));
    }

    @Test
    void malformedJsonIsReportedInUnifiedFormat() throws Exception {
        mvc.perform(post("/api/v1/auth/probe").contentType(MediaType.APPLICATION_JSON).content("{oops"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("AU40001"))
                .andExpect(jsonPath("$.details").isArray());
    }

    record Body(@NotBlank(message = "Tên không được để trống") String name) {
    }

    @RestController
    @RequestMapping("/api/v1")
    static class Probe {

        @GetMapping("/probe/conflict")
        String conflict() {
            throw new BusinessException(ErrorCode.AU40901, "Email đã tồn tại");
        }

        @GetMapping("/probe/denied")
        String denied() {
            throw new AccessDeniedException("no");
        }

        @GetMapping("/probe/boom")
        String boom() {
            throw new IllegalStateException("chi tiet noi bo");
        }

        @PostMapping({"/auth/probe", "/products/probe"})
        String create(@Valid @RequestBody Body body) {
            return body.name();
        }
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({
            "/api/v1/products, PR40001", "/api/v1/skus/1, PR40001",
            "/api/v1/orders, OD40001", "/api/v1/webhooks/shopee, OD40001",
            "/api/v1/pos/checkout, OD40001", "/api/v1/simulator/orders, OD40001",
            "/api/v1/reports/profit, RP40001",
            "/api/v1/purchase-receipts, IV40001", "/api/v1/alerts/stock/1/read, IV40001",
            "/api/v1/users, AU40001", "/api/v1/branches, AU40001", "/api/v1/settings, AU40001",
            "/api/v1/auth/login, AU40001"
    })
    void validationCodeFollowsModuleOfPath(String path, String expected) {
        org.springframework.mock.web.MockHttpServletRequest request =
                new org.springframework.mock.web.MockHttpServletRequest("POST", path);
        org.junit.jupiter.api.Assertions.assertEquals(expected, GlobalExceptionHandler.validationCode(
                new org.springframework.web.context.request.ServletWebRequest(request)));
    }
}
