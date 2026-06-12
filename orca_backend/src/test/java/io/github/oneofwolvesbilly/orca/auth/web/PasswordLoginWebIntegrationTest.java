package io.github.oneofwolvesbilly.orca.auth.web;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.oneofwolvesbilly.orca.OrcaApplication;
import io.github.oneofwolvesbilly.orca.auth.infrastructure.persistence.JdbcLoginCredentialVerifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = OrcaApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class PasswordLoginWebIntegrationTest {

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

        jdbcTemplate.update("INSERT INTO auth_registered_users (user_id) VALUES (?)", "user-1");
        jdbcTemplate.update(
                "INSERT INTO auth_login_credentials (login_identifier, password_hash, user_id) VALUES (?, ?, ?)",
                "employee-login-001",
                JdbcLoginCredentialVerifier.hashPasswordForStorage("correct-password"),
                "user-1"
        );
    }

    @Test
    void login_returns_http_only_secure_same_site_session_cookie_without_user_details() throws Exception {
        HttpResponse<String> response = postLogin("""
                {
                  "loginIdentifier": "employee-login-001",
                  "password": "correct-password"
                }
                """);

        assertEquals(204, response.statusCode());
        assertEquals("", response.body());

        List<String> setCookies = response.headers().allValues("Set-Cookie");
        assertEquals(1, setCookies.size());
        String cookie = setCookies.getFirst();
        assertTrue(cookie.startsWith("ORCA_SESSION="));
        assertTrue(cookie.contains("HttpOnly"));
        assertTrue(cookie.contains("Secure"));
        assertTrue(cookie.contains("SameSite=Lax"));
        assertTrue(cookie.contains("Max-Age="));
        assertFalse(cookie.contains("user-1"));
        assertFalse(cookie.contains("employee-login-001"));

        Integer sessionCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM auth_authenticated_sessions WHERE user_id = ?",
                Integer.class,
                "user-1"
        );
        assertEquals(1, sessionCount);

        Integer failureAuditCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM auth_login_failure_audits",
                Integer.class
        );
        assertEquals(0, failureAuditCount);
    }

    @Test
    void login_failure_returns_same_unauthorized_response_with_opaque_reference_without_session_cookie() throws Exception {
        HttpResponse<String> unknownIdentifier = postLogin("""
                {
                  "loginIdentifier": "missing-login",
                  "password": "correct-password"
                }
                """);
        HttpResponse<String> wrongPassword = postLogin("""
                {
                  "loginIdentifier": "employee-login-001",
                  "password": "wrong-password"
                }
                """);
        HttpResponse<String> blankIdentifier = postLogin("""
                {
                  "loginIdentifier": "   ",
                  "password": "correct-password"
                }
                """);

        assertLoginRejectedWithoutCookie(unknownIdentifier);
        assertLoginRejectedWithoutCookie(wrongPassword);
        assertLoginRejectedWithoutCookie(blankIdentifier);

        Map<String, Object> unknownBody = responseBody(unknownIdentifier);
        Map<String, Object> wrongPasswordBody = responseBody(wrongPassword);
        Map<String, Object> blankIdentifierBody = responseBody(blankIdentifier);

        assertStableLoginRejectedError(unknownBody);
        assertStableLoginRejectedError(wrongPasswordBody);
        assertStableLoginRejectedError(blankIdentifierBody);
        assertOpaqueReference(unknownBody);
        assertOpaqueReference(wrongPasswordBody);
        assertOpaqueReference(blankIdentifierBody);

        assertEquals(referenceAgnosticShape(unknownBody), referenceAgnosticShape(wrongPasswordBody));
        assertEquals(referenceAgnosticShape(unknownBody), referenceAgnosticShape(blankIdentifierBody));

        Integer sessionCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM auth_authenticated_sessions",
                Integer.class
        );
        assertEquals(0, sessionCount);

        Integer failureAuditCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM auth_login_failure_audits",
                Integer.class
        );
        assertEquals(3, failureAuditCount);
    }

    @Test
    void protected_commands_reject_missing_session_cookie() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:%d/api/groups".formatted(port)))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("""
                        {
                          "name": "Core Team"
                        }
                        """))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(401, response.statusCode());
    }

    private HttpResponse<String> postLogin(String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:%d/api/auth/login".formatted(port)))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static void assertLoginRejectedWithoutCookie(HttpResponse<String> response) {
        assertEquals(401, response.statusCode());
        assertTrue(response.headers().allValues("Set-Cookie").isEmpty());
        assertFalse(response.body().contains("missing-login"));
        assertFalse(response.body().contains("wrong-password"));
        assertFalse(response.body().contains("employee-login-001"));
        assertFalse(response.body().contains("INVALID_INPUT"));
        assertFalse(response.body().contains("INVALID_CREDENTIALS"));
    }

    private Map<String, Object> responseBody(HttpResponse<String> response) throws Exception {
        return objectMapper.readValue(response.body(), new TypeReference<>() {
        });
    }

    private static void assertOpaqueReference(Map<String, Object> body) {
        Object reference = body.get("loginFailureReferenceId");
        assertTrue(reference instanceof String);
        assertFalse(((String) reference).isBlank());
        assertFalse(((String) reference).contains("missing-login"));
        assertFalse(((String) reference).contains("employee-login-001"));
        assertFalse(((String) reference).contains("wrong-password"));
        assertFalse(((String) reference).contains("INVALID"));
    }

    private static void assertStableLoginRejectedError(Map<String, Object> body) {
        assertEquals(401, body.get("status"));
        assertEquals("LOGIN_REJECTED", body.get("code"));
        assertEquals("Login was rejected", body.get("message"));
        assertEquals(4, body.size());
    }

    private static Map<String, Object> referenceAgnosticShape(Map<String, Object> body) {
        Map<String, Object> shape = new LinkedHashMap<>(body);
        shape.put("loginFailureReferenceId", "<opaque-reference>");
        return shape;
    }
}
