package com.persistent.booking_platform.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.persistent.booking_platform.exception.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/** Returns 401 and 403 in the same JSON error format as the rest of the API. */
@Component
public class JsonSecurityHandlers implements AuthenticationEntryPoint, AccessDeniedHandler {
    private final ObjectMapper objectMapper;

    public JsonSecurityHandlers(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex)
            throws IOException {
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        write(response, HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "Please sign in to continue.");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex)
            throws IOException {
        write(response, HttpStatus.FORBIDDEN, "ACCESS_DENIED", "You do not have permission to do this.");
    }

    private void write(HttpServletResponse response, HttpStatus status, String code, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), ApiError.of(status.value(), code, message, List.of()));
    }
}
