package com.sami.app.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sami.app.common.api.ApiResponse;
import com.sami.app.common.api.ApiResponse.ApiError;
import com.sami.app.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** Returns a 403 in the standard envelope when an authenticated user lacks the required role. */
@Component
@RequiredArgsConstructor
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        response.setStatus(ErrorCode.ACCESS_DENIED.status().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiError error = new ApiError(ErrorCode.ACCESS_DENIED.name(),
                ErrorCode.ACCESS_DENIED.defaultMessage(), null);
        objectMapper.writeValue(response.getOutputStream(), ApiResponse.error(error));
    }
}
