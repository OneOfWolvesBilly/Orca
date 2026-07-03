package io.github.oneofwolvesbilly.orca.referencecore.web;

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
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(classes = OrcaApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ClientDiagnosticWebIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM reference_core_client_diagnostics");
        jdbcTemplate.update("DELETE FROM auth_login_failure_audits");
        jdbcTemplate.update("DELETE FROM auth_authenticated_sessions");
        jdbcTemplate.update("DELETE FROM auth_login_credentials");
        jdbcTemplate.update("DELETE FROM auth_system_role_assignments");
        jdbcTemplate.update("DELETE FROM auth_registered_users");
    }

    @Test
    void unauthenticated_client_records_safe_diagnostic_and_it_admin_looks_it_up() throws Exception {
        HttpResponse<String> createResponse = post("/api/client-diagnostics", """
                {
                  "category": "MALFORMED_RESPONSE",
                  "operation": "PASSWORD_LOGIN",
                  "clientApplication": "REACT",
                  "responseStatus": 500
                }
                """, null);

        assertEquals(201, createResponse.statusCode());
        Map<String, Object> created = body(createResponse);
        assertEquals(1, created.size());
        String referenceId = (String) created.get("clientFailureReferenceId");
        assertNotNull(UUID.fromString(referenceId));

        String adminCookie = login("admin", true);
        HttpResponse<String> lookupResponse = post("/api/client-diagnostics/lookup", """
                {
                  "clientFailureReferenceId": "%s"
                }
                """.formatted(referenceId), adminCookie);

        assertEquals(200, lookupResponse.statusCode());
        Map<String, Object> diagnostic = body(lookupResponse);
        assertEquals(referenceId, diagnostic.get("clientFailureReferenceId"));
        assertEquals("MALFORMED_RESPONSE", diagnostic.get("category"));
        assertEquals("PASSWORD_LOGIN", diagnostic.get("operation"));
        assertEquals("REACT", diagnostic.get("clientApplication"));
        assertEquals(500, diagnostic.get("responseStatus"));
        assertFalse(diagnostic.containsKey("password"));
        assertFalse(diagnostic.containsKey("loginFailureReferenceId"));
    }

    @Test
    void ingestion_rejects_unknown_fields_without_persisting_record() throws Exception {
        HttpResponse<String> response = post("/api/client-diagnostics", """
                {
                  "category": "MALFORMED_RESPONSE",
                  "operation": "PASSWORD_LOGIN",
                  "clientApplication": "REACT",
                  "rawResponseBody": "must-not-be-stored"
                }
                """, null);

        assertEquals(400, response.statusCode());
        assertStableError(response, "VALIDATION_ERROR");
        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM reference_core_client_diagnostics",
                Integer.class
        ));
        assertFalse(response.body().contains("must-not-be-stored"));
    }

    @Test
    void ingestion_rejects_missing_required_field() throws Exception {
        HttpResponse<String> response = post("/api/client-diagnostics", """
                {
                  "operation": "PASSWORD_LOGIN",
                  "clientApplication": "REACT"
                }
                """, null);

        assertEquals(400, response.statusCode());
        assertStableError(response, "VALIDATION_ERROR");
        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM reference_core_client_diagnostics",
                Integer.class
        ));
    }

    @Test
    void lookup_requires_session_and_it_admin_role() throws Exception {
        String referenceId = createDiagnostic();

        HttpResponse<String> unauthenticated = post("/api/client-diagnostics/lookup", """
                {"clientFailureReferenceId": "%s"}
                """.formatted(referenceId), null);
        assertEquals(401, unauthenticated.statusCode());
        assertStableError(unauthenticated, "UNAUTHENTICATED");

        String userCookie = login("regular-user", false);
        HttpResponse<String> forbidden = post("/api/client-diagnostics/lookup", """
                {"clientFailureReferenceId": "%s"}
                """.formatted(referenceId), userCookie);
        assertEquals(403, forbidden.statusCode());
        assertStableError(forbidden, "FORBIDDEN");
        assertFalse(forbidden.body().contains("MALFORMED_RESPONSE"));
    }

    @Test
    void malformed_and_unknown_references_use_same_not_found_error() throws Exception {
        String adminCookie = login("admin", true);

        HttpResponse<String> malformed = post("/api/client-diagnostics/lookup", """
                {"clientFailureReferenceId": "not-a-uuid"}
                """, adminCookie);
        HttpResponse<String> unknown = post("/api/client-diagnostics/lookup", """
                {"clientFailureReferenceId": "7f1eb30a-86d0-4a3e-89c8-a6ff395ec144"}
                """, adminCookie);

        assertEquals(404, malformed.statusCode());
        assertEquals(404, unknown.statusCode());
        assertEquals(body(malformed), body(unknown));
        assertStableError(malformed, "NOT_FOUND");
    }

    private String createDiagnostic() throws Exception {
        HttpResponse<String> response = post("/api/client-diagnostics", """
                {
                  "category": "MALFORMED_RESPONSE",
                  "operation": "PASSWORD_LOGIN",
                  "clientApplication": "REACT",
                  "responseStatus": 500
                }
                """, null);
        return (String) body(response).get("clientFailureReferenceId");
    }

    private String login(String userId, boolean itAdmin) throws Exception {
        String loginIdentifier = userId + "-login";
        jdbcTemplate.update("INSERT INTO auth_registered_users (user_id) VALUES (?)", userId);
        jdbcTemplate.update(
                "INSERT INTO auth_login_credentials (login_identifier, password_hash, user_id) VALUES (?, ?, ?)",
                loginIdentifier,
                JdbcLoginCredentialVerifier.hashPasswordForStorage("correct-password"),
                userId
        );
        if (itAdmin) {
            jdbcTemplate.update(
                    "INSERT INTO auth_system_role_assignments (user_id, role) VALUES (?, ?)",
                    userId,
                    "IT_ADMIN"
            );
        }
        HttpResponse<String> response = post("/api/auth/login", """
                {
                  "loginIdentifier": "%s",
                  "password": "correct-password"
                }
                """.formatted(loginIdentifier), null);
        assertEquals(204, response.statusCode());
        return response.headers().firstValue("Set-Cookie").orElseThrow().split(";", 2)[0];
    }

    private HttpResponse<String> post(String path, String json, String cookie) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(uri(path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json));
        if (cookie != null) {
            request.header("Cookie", cookie);
        }
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) {
        return URI.create("http://localhost:%d%s".formatted(port, path));
    }

    private Map<String, Object> body(HttpResponse<String> response) throws Exception {
        return objectMapper.readValue(response.body(), new TypeReference<>() {
        });
    }

    private void assertStableError(HttpResponse<String> response, String code) throws Exception {
        Map<String, Object> error = body(response);
        assertEquals(response.statusCode(), error.get("status"));
        assertEquals(code, error.get("code"));
        assertEquals(3, error.size());
    }
}
