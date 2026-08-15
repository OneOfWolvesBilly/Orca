package io.github.oneofwolvesbilly.orca.referencecore.web;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.oneofwolvesbilly.orca.OrcaApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = OrcaApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(StableApiErrorContractIntegrationTest.UnexpectedErrorController.class)
class StableApiErrorContractIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM auth_login_failure_audits");
        jdbcTemplate.update("DELETE FROM auth_authenticated_sessions");
        jdbcTemplate.update("DELETE FROM auth_login_credentials");
        jdbcTemplate.update("DELETE FROM auth_system_role_assignments");
        jdbcTemplate.update("DELETE FROM auth_registered_users");
    }

    @Test
    void malformed_request_body_uses_stable_validation_error() throws Exception {
        HttpResponse<String> response = post("/api/auth/login", "{");

        assertError(response, 400, "VALIDATION_ERROR", "Request validation failed");
        assertFalse(response.body().contains("JSON"));
        assertFalse(response.body().contains("HttpMessageNotReadableException"));
    }

    @Test
    void protected_request_without_session_uses_stable_unauthenticated_error() throws Exception {
        HttpResponse<String> response = post("/api/groups", """
                {
                  "name": "Core Team"
                }
                """);

        assertError(response, 401, "UNAUTHENTICATED", "Authentication is required");
    }

    @Test
    void unmapped_api_route_uses_stable_not_found_error() throws Exception {
        HttpResponse<String> response = get("/api/unmapped-route");

        assertError(response, 404, "NOT_FOUND", "Requested resource was not found");
    }

    @Test
    void unsupported_method_uses_stable_method_not_allowed_error() throws Exception {
        HttpResponse<String> response = get("/api/auth/login");

        assertError(response, 405, "METHOD_NOT_ALLOWED", "HTTP method is not allowed");
    }

    @Test
    void unexpected_exception_uses_safe_internal_error() throws Exception {
        HttpResponse<String> response = get("/api/test/unexpected-error");

        assertError(response, 500, "INTERNAL_ERROR", "An unexpected server error occurred");
        assertFalse(response.body().contains("sensitive-internal-detail"));
        assertFalse(response.body().contains("IllegalStateException"));
        assertFalse(response.body().contains("stackTrace"));
    }

    @Test
    void unexpected_illegal_argument_exception_uses_safe_internal_error() throws Exception {
        HttpResponse<String> response = get("/api/test/unexpected-illegal-argument-error");

        assertError(response, 500, "INTERNAL_ERROR", "An unexpected server error occurred");
        assertFalse(response.body().contains("sensitive-illegal-argument-detail"));
        assertFalse(response.body().contains("IllegalArgumentException"));
        assertFalse(response.body().contains("stackTrace"));
    }

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri(path)).GET().build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String path, String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri(path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) {
        return URI.create("http://localhost:%d%s".formatted(port, path));
    }

    private void assertError(HttpResponse<String> response, int status, String code, String message) throws Exception {
        assertEquals(status, response.statusCode());
        Map<String, Object> body = objectMapper.readValue(response.body(), new TypeReference<>() {
        });
        assertEquals(status, body.get("status"));
        assertEquals(code, body.get("code"));
        assertEquals(message, body.get("message"));
        assertEquals(3, body.size());
        assertTrue(response.headers().firstValue("Content-Type").orElse("").contains("application/json"));
    }

    @RestController
    static class UnexpectedErrorController {

        @GetMapping("/api/test/unexpected-error")
        void unexpectedError() {
            throw new IllegalStateException("sensitive-internal-detail");
        }

        @GetMapping("/api/test/unexpected-illegal-argument-error")
        void unexpectedIllegalArgumentError() {
            throw new IllegalArgumentException("sensitive-illegal-argument-detail");
        }
    }
}
