package io.github.oneofwolvesbilly.orcafixture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

@SpringBootTest(
        classes = MinimalConsumerFixtureApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
class EmbeddedAuthConsumerContractTest {

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private FixtureActorCommand fixtureActorCommand;

    private final HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    @BeforeEach
    void setUp() throws Exception {
        reset(fixtureActorCommand);
        jdbcTemplate.update("DELETE FROM auth_login_failure_audits");
        jdbcTemplate.update("DELETE FROM auth_authenticated_sessions");
        jdbcTemplate.update("DELETE FROM auth_login_credentials");
        jdbcTemplate.update("DELETE FROM auth_system_role_assignments");
        jdbcTemplate.update("DELETE FROM auth_registered_users");

        jdbcTemplate.update("INSERT INTO auth_registered_users (user_id) VALUES (?)", "user-1");
        jdbcTemplate.update(
                "INSERT INTO auth_login_credentials (login_identifier, password_hash, user_id) VALUES (?, ?, ?)",
                "fixture-login",
                sha256("correct-password"),
                "user-1"
        );
    }

    @Test
    void login_supplies_one_actor_to_fixture_and_logout_prevents_reuse() throws Exception {
        HttpResponse<String> login = post("/api/auth/login", """
                {
                  "loginIdentifier": "fixture-login",
                  "password": "correct-password"
                }
                """, null);

        assertEquals(204, login.statusCode());
        String sessionExpiresAt = login.headers().firstValue("Orca-Session-Expires-At")
                .orElseThrow();
        assertFalse(sessionExpiresAt.isBlank());
        String sessionCookie = login.headers().firstValue("Set-Cookie")
                .orElseThrow()
                .split(";", 2)[0];
        assertFalse(sessionCookie.contains("user-1"));

        HttpResponse<String> protectedCommand = post(
                "/api/fixture/actor-context-check",
                "{}",
                sessionCookie
        );

        assertEquals(204, protectedCommand.statusCode());
        assertEquals("", protectedCommand.body());
        verify(fixtureActorCommand).handle("user-1");

        HttpResponse<String> logout = post("/api/auth/logout", "{}", sessionCookie);
        assertEquals(204, logout.statusCode());

        reset(fixtureActorCommand);
        HttpResponse<String> afterLogout = post(
                "/api/fixture/actor-context-check",
                "{}",
                sessionCookie
        );

        assertUnauthenticated(afterLogout);
        verify(fixtureActorCommand, never()).handle("user-1");
    }

    @Test
    void missing_or_multiple_sessions_and_demo_header_never_execute_fixture() throws Exception {
        HttpResponse<String> missing = post("/api/fixture/actor-context-check", "{}", null);
        assertUnauthenticated(missing);

        HttpResponse<String> demoHeader = postWithHeaders(
                "/api/fixture/actor-context-check",
                "{}",
                "X-User-Id", "user-1"
        );
        assertUnauthenticated(demoHeader);

        HttpResponse<String> multipleCookies = postWithHeaders(
                "/api/fixture/actor-context-check",
                "{}",
                "Cookie", "ORCA_SESSION=first; ORCA_SESSION=second"
        );
        assertUnauthenticated(multipleCookies);

        verify(fixtureActorCommand, never()).handle("user-1");
    }

    @Test
    void non_empty_fixture_body_is_rejected_before_fixture_execution() throws Exception {
        String sessionCookie = loginCookie();

        HttpResponse<String> response = post(
                "/api/fixture/actor-context-check",
                "{\"unexpected\":\"value\"}",
                sessionCookie
        );

        assertEquals(400, response.statusCode());
        assertFalse(response.body().contains("user-1"));
        assertFalse(response.body().contains(sessionCookie));
        verify(fixtureActorCommand, never()).handle("user-1");
    }

    private String loginCookie() throws Exception {
        HttpResponse<String> response = post("/api/auth/login", """
                {
                  "loginIdentifier": "fixture-login",
                  "password": "correct-password"
                }
                """, null);
        assertEquals(204, response.statusCode());
        return response.headers().firstValue("Set-Cookie").orElseThrow().split(";", 2)[0];
    }

    private HttpResponse<String> post(String path, String body, String cookie) throws Exception {
        HttpRequest.Builder request = request(path, body);
        if (cookie != null) {
            request.header("Cookie", cookie);
        }
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> postWithHeaders(
            String path,
            String body,
            String headerName,
            String headerValue
    ) throws Exception {
        return client.send(
                request(path, body).header(headerName, headerValue).build(),
                HttpResponse.BodyHandlers.ofString()
        );
    }

    private HttpRequest.Builder request(String path, String body) {
        return HttpRequest.newBuilder(URI.create("http://localhost:%d%s".formatted(port, path)))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body));
    }

    private static void assertUnauthenticated(HttpResponse<String> response) {
        assertEquals(401, response.statusCode());
        assertFalse(response.body().contains("ORCA_SESSION"));
        assertFalse(response.body().contains("user-1"));
        assertFalse(response.body().contains("revoked"));
        assertFalse(response.body().contains("expired"));
    }

    private static String sha256(String value) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
    }
}
