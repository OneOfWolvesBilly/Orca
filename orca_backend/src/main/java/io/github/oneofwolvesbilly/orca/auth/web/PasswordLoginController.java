package io.github.oneofwolvesbilly.orca.auth.web;

import io.github.oneofwolvesbilly.orca.auth.application.PasswordLoginCommand;
import io.github.oneofwolvesbilly.orca.auth.application.PasswordLoginResult;
import io.github.oneofwolvesbilly.orca.auth.application.PasswordLoginUseCase;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Objects;

@RestController
@RequestMapping("/api/auth")
final class PasswordLoginController {

    static final String SESSION_COOKIE_NAME = "ORCA_SESSION";

    private final PasswordLoginUseCase passwordLoginUseCase;
    private final Duration sessionLifetime;

    PasswordLoginController(PasswordLoginUseCase passwordLoginUseCase, Duration sessionLifetime) {
        this.passwordLoginUseCase = Objects.requireNonNull(passwordLoginUseCase, "passwordLoginUseCase");
        this.sessionLifetime = Objects.requireNonNull(sessionLifetime, "sessionLifetime");
    }

    @PostMapping("/login")
    ResponseEntity<Void> login(@RequestBody PasswordLoginRequest request) {
        PasswordLoginResult result = passwordLoginUseCase.handle(new PasswordLoginCommand(
                request.loginIdentifier(),
                request.password()
        ));
        ResponseCookie cookie = ResponseCookie.from(SESSION_COOKIE_NAME, result.sessionId().value())
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .path("/")
                .maxAge(sessionLifetime)
                .build();
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .build();
    }

    record PasswordLoginRequest(String loginIdentifier, String password) {
    }
}
