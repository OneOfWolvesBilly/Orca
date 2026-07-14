package io.github.oneofwolvesbilly.orca.auth.web;

import io.github.oneofwolvesbilly.orca.OrcaApplication;
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
import java.sql.Timestamp;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = OrcaApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class LogoutSessionWebIntegrationTest {

    private static final String ACTIVE_SESSION_ID = "active-session";
    private static final String EXPIRED_SESSION_ID = "expired-session";
    private static final String REVOKED_SESSION_ID = "revoked-session";

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final HttpClient client = HttpClient.newHttpClient();

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM auth_authenticated_sessions");
        jdbcTemplate.update("DELETE FROM auth_login_credentials");
        jdbcTemplate.update("DELETE FROM auth_system_role_assignments");
        jdbcTemplate.update("DELETE FROM auth_registered_users");

        jdbcTemplate.update("INSERT INTO auth_registered_users (user_id) VALUES (?)", "user-1");
        createSession(ACTIVE_SESSION_ID, "2026-05-29 00:00:00", "2999-01-01 00:00:00", null);
        createSession(EXPIRED_SESSION_ID, "1999-01-01 00:00:00", "2000-01-01 00:00:00", null);
        createSession(REVOKED_SESSION_ID, "2026-05-29 00:00:00", "2999-01-01 00:00:00", "2026-05-29 01:00:00");
    }

    @Test
    void logout_revokes_only_the_presented_active_session_and_protected_commands_reject_it() throws Exception {
        HttpResponse<String> logoutResponse = postLogout(ACTIVE_SESSION_ID);

        assertSafeLogoutResponse(logoutResponse);
        Timestamp revokedAt = jdbcTemplate.queryForObject(
                "SELECT revoked_at FROM auth_authenticated_sessions WHERE session_id = ?",
                Timestamp.class,
                ACTIVE_SESSION_ID
        );
        assertNotNull(revokedAt);
        assertNull(jdbcTemplate.queryForObject(
                "SELECT revoked_at FROM auth_authenticated_sessions WHERE session_id = ?",
                Timestamp.class,
                EXPIRED_SESSION_ID
        ));
        assertEquals(3, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM auth_authenticated_sessions",
                Integer.class
        ));

        HttpResponse<String> protectedCommandResponse = postProtectedCommand(ACTIVE_SESSION_ID);

        assertEquals(401, protectedCommandResponse.statusCode());
        assertFalse(protectedCommandResponse.body().contains(ACTIVE_SESSION_ID));
        assertFalse(protectedCommandResponse.body().contains("revoked"));
    }

    @Test
    void logout_returns_the_same_safe_response_for_no_active_session_conditions() throws Exception {
        HttpResponse<String> missing = postLogout(null);
        HttpResponse<String> blank = postLogout("");
        HttpResponse<String> unknown = postLogout("unknown-session");
        HttpResponse<String> expired = postLogout(EXPIRED_SESSION_ID);
        HttpResponse<String> revoked = postLogout(REVOKED_SESSION_ID);

        assertSafeLogoutResponse(missing);
        assertSafeLogoutResponse(blank);
        assertSafeLogoutResponse(unknown);
        assertSafeLogoutResponse(expired);
        assertSafeLogoutResponse(revoked);
        assertEquals(missing.statusCode(), blank.statusCode());
        assertEquals(missing.statusCode(), unknown.statusCode());
        assertEquals(missing.statusCode(), expired.statusCode());
        assertEquals(missing.statusCode(), revoked.statusCode());
        assertEquals(missing.body(), blank.body());
        assertEquals(missing.body(), unknown.body());
        assertEquals(missing.body(), expired.body());
        assertEquals(missing.body(), revoked.body());
        assertNull(jdbcTemplate.queryForObject(
                "SELECT revoked_at FROM auth_authenticated_sessions WHERE session_id = ?",
                Timestamp.class,
                ACTIVE_SESSION_ID
        ));
        assertNull(jdbcTemplate.queryForObject(
                "SELECT revoked_at FROM auth_authenticated_sessions WHERE session_id = ?",
                Timestamp.class,
                EXPIRED_SESSION_ID
        ));
    }

    @Test
    void logout_is_not_exposed_as_get() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(logoutUri())
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(405, response.statusCode());
    }

    @Test
    void logout_rejects_a_session_id_in_the_request_body_without_revoking_a_session() throws Exception {
        HttpResponse<String> response = postLogout(null, """
                {
                  "sessionId": "active-session"
                }
                """);

        assertEquals(400, response.statusCode());
        assertFalse(response.body().contains(ACTIVE_SESSION_ID));
        assertNull(jdbcTemplate.queryForObject(
                "SELECT revoked_at FROM auth_authenticated_sessions WHERE session_id = ?",
                Timestamp.class,
                ACTIVE_SESSION_ID
        ));
    }

    private void createSession(String sessionId, String createdAt, String expiresAt, String revokedAt) {
        jdbcTemplate.update(
                """
                INSERT INTO auth_authenticated_sessions (session_id, user_id, created_at, expires_at, revoked_at)
                VALUES (?, ?, ?, ?, ?)
                """,
                sessionId,
                "user-1",
                Timestamp.valueOf(createdAt),
                Timestamp.valueOf(expiresAt),
                revokedAt == null ? null : Timestamp.valueOf(revokedAt)
        );
    }

    private HttpResponse<String> postLogout(String sessionId) throws Exception {
        return postLogout(sessionId, "{}");
    }

    private HttpResponse<String> postLogout(String sessionId, String body) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(logoutUri())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body));
        if (sessionId != null) {
            request.header("Cookie", "ORCA_SESSION=" + sessionId);
        }
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> postProtectedCommand(String sessionId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:%d/api/groups".formatted(port)))
                .header("Content-Type", "application/json")
                .header("Cookie", "ORCA_SESSION=" + sessionId)
                .POST(HttpRequest.BodyPublishers.ofString("""
                        {
                          "name": "Core Team"
                        }
                        """))
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private URI logoutUri() {
        return URI.create("http://localhost:%d/api/auth/logout".formatted(port));
    }

    private static void assertSafeLogoutResponse(HttpResponse<String> response) {
        assertEquals(204, response.statusCode());
        assertEquals("", response.body());
        List<String> setCookies = response.headers().allValues("Set-Cookie");
        assertTrue(setCookies.isEmpty());
    }
}
