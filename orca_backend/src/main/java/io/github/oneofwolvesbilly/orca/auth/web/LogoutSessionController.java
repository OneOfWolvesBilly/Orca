package io.github.oneofwolvesbilly.orca.auth.web;

import io.github.oneofwolvesbilly.orca.auth.application.LogoutSessionCommand;
import io.github.oneofwolvesbilly.orca.auth.application.LogoutSessionUseCase;
import io.github.oneofwolvesbilly.orca.referencecore.web.RequestValidationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/auth")
final class LogoutSessionController {

    private final LogoutSessionUseCase logoutSessionUseCase;

    LogoutSessionController(LogoutSessionUseCase logoutSessionUseCase) {
        this.logoutSessionUseCase = Objects.requireNonNull(logoutSessionUseCase, "logoutSessionUseCase");
    }

    @PostMapping("/logout")
    ResponseEntity<Void> logout(
            @CookieValue(name = PasswordLoginController.SESSION_COOKIE_NAME, required = false) String sessionId,
            @RequestBody Map<String, Object> request
    ) {
        if (!request.isEmpty()) {
            throw new RequestValidationException("logout request body must be empty");
        }
        logoutSessionUseCase.handle(new LogoutSessionCommand(sessionId));
        return ResponseEntity.noContent().build();
    }
}
