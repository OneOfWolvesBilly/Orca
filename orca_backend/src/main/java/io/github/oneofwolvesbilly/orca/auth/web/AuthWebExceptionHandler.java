package io.github.oneofwolvesbilly.orca.auth.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
final class AuthWebExceptionHandler {

    @ExceptionHandler(UnauthenticatedHttpRequestException.class)
    ResponseEntity<ProblemDetail> unauthenticated(UnauthenticatedHttpRequestException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Missing authenticated user");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problem);
    }
}
