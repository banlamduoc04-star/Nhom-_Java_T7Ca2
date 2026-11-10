package com.oism.web.error;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oism.domain.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** Chua dang nhap / token sai hoac het han -> 401 AU40101 (JSON, khong phai trang HTML). */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final String WEBHOOK_PATH_PREFIX = "/api/v1/webhooks/";

    private final ObjectMapper objectMapper;

    public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        // Webhook san xac thuc bang API key -> OD40101 (Bang ma loi); con lai la token dang nhap -> AU40101
        boolean webhook = request.getRequestURI().startsWith(request.getContextPath() + WEBHOOK_PATH_PREFIX);
        ErrorCode code = webhook ? ErrorCode.OD40101 : ErrorCode.AU40101;
        String message = webhook ? "API key webhook thiếu hoặc không hợp lệ" : code.defaultMessage();
        response.setStatus(code.httpStatus());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), ApiError.of(code.name(), message));
    }
}
