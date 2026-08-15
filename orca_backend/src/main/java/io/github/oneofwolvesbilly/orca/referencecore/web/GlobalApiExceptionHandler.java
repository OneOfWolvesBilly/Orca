package io.github.oneofwolvesbilly.orca.referencecore.web;

import io.github.oneofwolvesbilly.orca.auth.application.LoginRejectedException;
import io.github.oneofwolvesbilly.orca.auth.web.UnauthenticatedHttpRequestException;
import io.github.oneofwolvesbilly.orca.organization.application.OrganizationApplicationFailure;
import io.github.oneofwolvesbilly.orca.referencecore.application.ClientDiagnosticForbiddenException;
import io.github.oneofwolvesbilly.orca.referencecore.application.ClientDiagnosticNotFoundException;
import io.github.oneofwolvesbilly.orca.referencecore.application.ClientDiagnosticValidationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
final class GlobalApiExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(UnauthenticatedHttpRequestException.class)
    ResponseEntity<ApiErrorResponse> unauthenticated() {
        return error(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "Authentication is required");
    }

    @ExceptionHandler(LoginRejectedException.class)
    ResponseEntity<ApiErrorResponse> loginRejected(LoginRejectedException ex) {
        String referenceId = ex.loginFailureReferenceId() == null
                ? null
                : ex.loginFailureReferenceId().value();
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiErrorResponse.loginRejected(referenceId));
    }

    @ExceptionHandler(ClientDiagnosticForbiddenException.class)
    ResponseEntity<ApiErrorResponse> clientDiagnosticForbidden() {
        return error(HttpStatus.FORBIDDEN, "FORBIDDEN", "Operation is forbidden");
    }

    @ExceptionHandler(ClientDiagnosticNotFoundException.class)
    ResponseEntity<ApiErrorResponse> clientDiagnosticNotFound() {
        return error(HttpStatus.NOT_FOUND, "NOT_FOUND", "Requested resource was not found");
    }

    @ExceptionHandler(OrganizationApplicationFailure.class)
    ResponseEntity<ApiErrorResponse> organizationFailure(OrganizationApplicationFailure ex) {
        return switch (ex.category()) {
            case NOT_FOUND -> error(HttpStatus.NOT_FOUND, "NOT_FOUND", "Requested resource was not found");
            case FORBIDDEN -> error(HttpStatus.FORBIDDEN, "FORBIDDEN", "Operation is forbidden");
            case APPLICATION_REJECTED ->
                    error(HttpStatus.BAD_REQUEST, "APPLICATION_REJECTED", "Request was rejected");
        };
    }

    @ExceptionHandler({RequestValidationException.class, ClientDiagnosticValidationException.class})
    ResponseEntity<ApiErrorResponse> validation() {
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Request validation failed");
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiErrorResponse> unexpected() {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An unexpected server error occurred");
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        return frameworkError(headers, HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Request validation failed");
    }

    @Override
    protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(
            HttpRequestMethodNotSupportedException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        return frameworkError(headers, HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", "HTTP method is not allowed");
    }

    @Override
    protected ResponseEntity<Object> handleNoResourceFoundException(
            NoResourceFoundException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        return frameworkError(headers, HttpStatus.NOT_FOUND, "NOT_FOUND", "Requested resource was not found");
    }

    private static ResponseEntity<ApiErrorResponse> error(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(ApiErrorResponse.of(status.value(), code, message));
    }

    private static ResponseEntity<Object> frameworkError(
            HttpHeaders headers,
            HttpStatus status,
            String code,
            String message
    ) {
        return new ResponseEntity<>(ApiErrorResponse.of(status.value(), code, message), headers, status);
    }
}
