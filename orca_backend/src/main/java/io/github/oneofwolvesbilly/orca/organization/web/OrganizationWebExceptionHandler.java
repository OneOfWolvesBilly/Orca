package io.github.oneofwolvesbilly.orca.organization.web;

import io.github.oneofwolvesbilly.orca.organization.domain.DomainError;
import io.github.oneofwolvesbilly.orca.organization.domain.DomainException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = OrganizationCommandController.class)
final class OrganizationWebExceptionHandler {

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ProblemDetail> malformedJson(HttpMessageNotReadableException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Malformed request body");
    }

    @ExceptionHandler(DomainException.class)
    ResponseEntity<ProblemDetail> domain(DomainException ex) {
        if (isForbidden(ex.error())) {
            return problem(HttpStatus.FORBIDDEN, ex.getMessage());
        }
        if (ex.error() == DomainError.INVITATION_NOT_FOUND) {
            return problem(HttpStatus.NOT_FOUND, ex.getMessage());
        }
        return problem(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ProblemDetail> illegalArgument(IllegalArgumentException ex) {
        if (ex.getMessage() != null && ex.getMessage().startsWith("Group not found")) {
            return problem(HttpStatus.NOT_FOUND, ex.getMessage());
        }
        return problem(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    private static boolean isForbidden(DomainError error) {
        return error == DomainError.INVITER_NOT_GROUP_ADMIN
                || error == DomainError.INVITATION_ACCEPTOR_MISMATCH
                || error == DomainError.INVITATION_REJECTOR_MISMATCH;
    }

    private static ResponseEntity<ProblemDetail> problem(HttpStatus status, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        return ResponseEntity.status(status).body(problem);
    }
}
