package com.ecommerce.identity.security;

import com.ecommerce.common.security.SecurityErrorResponseWriter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.InvalidCsrfTokenException;
import org.springframework.security.web.csrf.MissingCsrfTokenException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final SecurityErrorResponseWriter errorResponseWriter;

    @Override
    public void handle(@NonNull HttpServletRequest request,
                       @NonNull HttpServletResponse response,
                       @NonNull AccessDeniedException accessDeniedException) throws IOException {

        boolean csrfFailure = accessDeniedException instanceof MissingCsrfTokenException
            || accessDeniedException instanceof InvalidCsrfTokenException;

        String code = csrfFailure ? "CSRF_VALIDATION_FAILED" : "ACCESS_DENIED";

        String detail = csrfFailure
            ? "Invalid or missing CSRF token"
            : "You do not have permission to perform this operation";

        errorResponseWriter.write(request, response, HttpStatus.FORBIDDEN, code, detail);
    }
}
