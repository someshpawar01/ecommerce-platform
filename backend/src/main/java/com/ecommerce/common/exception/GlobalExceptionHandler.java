package com.ecommerce.common.exception;

import com.ecommerce.common.constants.DatabaseErrorCodes;
import com.ecommerce.common.constants.ErrorCodes;
import com.ecommerce.customer.exception.CustomerAddressNotFoundException;
import com.ecommerce.customer.exception.CustomerProfileNotFoundException;
import com.ecommerce.customer.exception.InvalidCustomerAddressException;
import com.ecommerce.customer.exception.InvalidCustomerProfileException;
import com.ecommerce.identity.constants.IdentityConstraintNames;
import com.ecommerce.identity.exception.*;
import com.ecommerce.identity.exception.passwordreset.InvalidPasswordResetTokenException;
import com.ecommerce.identity.exception.ratelimit.AccountRateLimitExceededException;
import com.ecommerce.identity.exception.ratelimit.RateLimitUnavailableException;
import com.ecommerce.product.exception.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ProblemDetail> handleDuplicateEmail(
        EmailAlreadyExistsException ex, HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, ErrorCodes.EMAIL_ALREADY_EXISTS, ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidPasswordException.class)
    public ResponseEntity<ProblemDetail> handleInvalidPassword(
        InvalidPasswordException ex, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, ErrorCodes.INVALID_PASSWORD, ex.getMessage(), request);
    }

    @ExceptionHandler(IdentityConfigurationException.class)
    public ResponseEntity<ProblemDetail> handleConfigurationError(
        IdentityConfigurationException ex, HttpServletRequest request) {
        log.error("Identity configuration error", ex);

        return problem(
            HttpStatus.INTERNAL_SERVER_ERROR,
            ErrorCodes.INTERNAL_ERROR,
            "An internal server error occurred",
            request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> handleDataIntegrityViolation(
        DataIntegrityViolationException ex, HttpServletRequest request) {
        SQLException sqlException = findSqlException(ex);

        if (sqlException != null
            && sqlException.getErrorCode() == DatabaseErrorCodes.MYSQL_DUPLICATE_KEY) {

            String databaseMessage = sqlException.getMessage();

            if (databaseMessage != null
                && databaseMessage.contains(IdentityConstraintNames.UK_EMAIL_NORMALIZED_UNIQUE)) {

                return problem(
                    HttpStatus.CONFLICT,
                    ErrorCodes.EMAIL_ALREADY_EXISTS,
                    "An account with this email already exists",
                    request);
            }

            return problem(
                HttpStatus.CONFLICT,
                ErrorCodes.DATA_CONFLICT,
                "The requested operation conflicts with existing data",
                request);
        }

        log.error("Unexpected database integrity violation", ex);

        return problem(
            HttpStatus.INTERNAL_SERVER_ERROR,
            ErrorCodes.INTERNAL_ERROR,
            "An internal server error occurred",
            request);
    }

    @Override
    protected @Nullable ResponseEntity<Object> handleMethodArgumentNotValid(
        MethodArgumentNotValidException ex,
        @NonNull HttpHeaders headers,
        @NonNull HttpStatusCode status,
        @NonNull WebRequest request) {

        Map<String, String> fieldErrors = new LinkedHashMap<>();

        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage());
        }

        ProblemDetail problem =
            ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request validation failed");

        problem.setTitle("Validation Failed");
        problem.setProperty("code", ErrorCodes.VALIDATION_ERROR);
        problem.setProperty("errors", fieldErrors);

        return handleExceptionInternal(ex, problem, headers, status, request);
    }

    @ExceptionHandler(InvalidVerificationTokenException.class)
    public ResponseEntity<ProblemDetail> handleInvalidVerificationToken(
        InvalidVerificationTokenException ex, HttpServletRequest request) {

        return problem(
            HttpStatus.BAD_REQUEST, ErrorCodes.INVALID_VERIFICATION_TOKEN, ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidAccountStateException.class)
    public ResponseEntity<ProblemDetail> handleInvalidAccountState(
        InvalidAccountStateException ex, HttpServletRequest request) {

        return problem(HttpStatus.CONFLICT, ErrorCodes.INVALID_ACCOUNT_STATE, ex.getMessage(), request);
    }

    @ExceptionHandler(AuthenticationFailureException.class)
    public ResponseEntity<ProblemDetail> handleAuthenticationFailure(
        AuthenticationFailureException ex, HttpServletRequest request) {

        return problem(
            HttpStatus.UNAUTHORIZED,
            ErrorCodes.INVALID_CREDENTIALS,
            "Authentication failed",
            request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDenied(
        AccessDeniedException ex,
        HttpServletRequest request) {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.FORBIDDEN,
            "You do not have permission to perform this operation"
        );

        problem.setTitle("Forbidden");
        problem.setProperty("code", "ACCESS_DENIED");
        problem.setInstance(URI.create(request.getRequestURI()));

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problem);
    }

    @ExceptionHandler(InvalidPasswordResetTokenException.class)
    public ResponseEntity<ProblemDetail> handleInvalidPasswordResetToken(
        InvalidPasswordResetTokenException ex,
        HttpServletRequest request) {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            "Invalid or expired password reset link"
        );

        problem.setTitle("Bad Request");
        problem.setProperty("code", "INVALID_RESET_TOKEN");
        problem.setInstance(URI.create(request.getRequestURI()));

        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(AccountRateLimitExceededException.class)
    public ResponseEntity<ProblemDetail> handleRateLimitExceeded(
        AccountRateLimitExceededException exception, HttpServletRequest request) {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.TOO_MANY_REQUESTS,
            "Too many requests. Please try again later.");

        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", "RATE_LIMIT_EXCEEDED");

        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(problem);
    }

    @ExceptionHandler(RateLimitUnavailableException.class)
    public ResponseEntity<ProblemDetail> handleRateLimitUnavailable(
        RateLimitUnavailableException exception, HttpServletRequest request) {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.SERVICE_UNAVAILABLE,
            "The service is temporarily unavailable. Please try again later.");

        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", "SERVICE_UNAVAILABLE");

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(problem);
    }

    @ExceptionHandler(CustomerProfileNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleCustomerProfileNotFound(
        CustomerProfileNotFoundException exception, HttpServletRequest request) {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", "CUSTOMER_PROFILE_NOT_FOUND");

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    @ExceptionHandler(InvalidCustomerProfileException.class)
    public ResponseEntity<ProblemDetail> handleInvalidCustomerProfile(
        InvalidCustomerProfileException exception, HttpServletRequest request) {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", "INVALID_CUSTOMER_PROFILE");

        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(CustomerAddressNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleCustomerAddressNotFound(
        CustomerAddressNotFoundException exception, HttpServletRequest request) {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", "CUSTOMER_ADDRESS_NOT_FOUND");

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    @ExceptionHandler(InvalidCustomerAddressException.class)
    public ResponseEntity<ProblemDetail> handleInvalidCustomerAddress(
        InvalidCustomerAddressException exception, HttpServletRequest request) {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", "INVALID_CUSTOMER_ADDRESS");

        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(CategoryNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleCategoryNotFound(
        CategoryNotFoundException exception, HttpServletRequest request) {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", "CATEGORY_NOT_FOUND");

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    @ExceptionHandler(InvalidCategoryException.class)
    public ResponseEntity<ProblemDetail> handleInvalidCategory(
        InvalidCategoryException exception, HttpServletRequest request) {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", "INVALID_CATEGORY");

        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(InvalidCategoryOrderException.class)
    public ResponseEntity<ProblemDetail> handleInvalidCategoryOrder(
        InvalidCategoryException exception, HttpServletRequest request) {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", "INVALID_CATEGORY_ORDER");

        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(BrandNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleBrandNotFound(
        BrandNotFoundException exception, HttpServletRequest request) {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", "BRAND_NOT_FOUND");

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    @ExceptionHandler(DuplicateBrandException.class)
    public ResponseEntity<ProblemDetail> handleDuplicateBrand(
        DuplicateBrandException ex, HttpServletRequest request) {

        return problem(HttpStatus.CONFLICT, "DUPLICATE_BRAND", ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidBrandException.class)
    public ResponseEntity<ProblemDetail> handleInvalidBrand(
        InvalidBrandException exception, HttpServletRequest request) {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", "INVALID_BRAND");

        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ProblemDetail> handleOptimisticLocking(
        ObjectOptimisticLockingFailureException exception, HttpServletRequest request) {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.CONFLICT,
            "The profile was modified concurrently. Please reload and try again.");

        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", "CONCURRENT_MODIFICATION");

        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpectedException(
        Exception ex, HttpServletRequest request) {
        log.error("Unexpected application error", ex);

        return problem(
            HttpStatus.INTERNAL_SERVER_ERROR,
            ErrorCodes.INTERNAL_ERROR,
            "An internal server error occurred",
            request);
    }

    private SQLException findSqlException(Throwable throwable) {
        Throwable current = throwable;

        while (current != null) {
            if (current instanceof SQLException sqlException) {
                return sqlException;
            }
            current = current.getCause();
        }
        return null;
    }

    private ResponseEntity<ProblemDetail> problem(
        HttpStatus status, String code, String detail, HttpServletRequest request) {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);

        problem.setTitle(status.getReasonPhrase());
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", code);

        return ResponseEntity.status(status).body(problem);
    }
}
