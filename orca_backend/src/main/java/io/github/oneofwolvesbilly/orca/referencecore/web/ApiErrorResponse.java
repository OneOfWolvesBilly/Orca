package io.github.oneofwolvesbilly.orca.referencecore.web;

import com.fasterxml.jackson.annotation.JsonInclude;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonInclude(NON_NULL)
record ApiErrorResponse(
        int status,
        String code,
        String message,
        String loginFailureReferenceId
) {

    static ApiErrorResponse of(int status, String code, String message) {
        return new ApiErrorResponse(status, code, message, null);
    }

    static ApiErrorResponse loginRejected(String loginFailureReferenceId) {
        return new ApiErrorResponse(401, "LOGIN_REJECTED", "Login was rejected", loginFailureReferenceId);
    }
}
