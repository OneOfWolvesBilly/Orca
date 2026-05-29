package io.github.oneofwolvesbilly.orca.auth.web;

import io.github.oneofwolvesbilly.orca.auth.application.LoginRejectedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
final class AuthWebExceptionHandler {

    @ExceptionHandler(UnauthenticatedHttpRequestException.class)
    ResponseEntity<ProblemDetail> unauthenticated(UnauthenticatedHttpRequestException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Unauthenticated");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problem);
    }

    @ExceptionHandler(LoginRejectedException.class)
    ResponseEntity<ProblemDetail> loginRejected(LoginRejectedException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Invalid login credentials");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problem);
    }
}
