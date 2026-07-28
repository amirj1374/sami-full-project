package com.sami.app.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sami.app.common.api.ApiResponse;
import com.sami.app.common.api.ApiResponse.ApiError;
import com.sami.app.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** Returns a 401 in the standard envelope when an unauthenticated user hits a protected endpoint. */
@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        write(response, ErrorCode.UNAUTHENTICATED);
    }

    private void write(HttpServletResponse response, ErrorCode code) throws IOException {
        response.setStatus(code.status().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiError error = new ApiError(code.name(), code.defaultMessage(), null);
        objectMapper.writeValue(response.getOutputStream(), ApiResponse.error(error));
    }
}
