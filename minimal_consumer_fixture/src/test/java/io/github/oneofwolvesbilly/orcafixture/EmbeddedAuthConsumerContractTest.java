package io.github.oneofwolvesbilly.orcafixture;

import io.github.oneofwolvesbilly.orca.auth.api.AuthenticatedActor;
import io.github.oneofwolvesbilly.orca.auth.api.OrcaProtectedCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.util.HexFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        classes = MinimalConsumerFixtureApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
@Import({
        EmbeddedAuthConsumerContractTest.RecordingFixtureConfiguration.class,
        EmbeddedAuthConsumerContractTest.SupportedMethodController.class
})
class EmbeddedAuthConsumerContractTest {

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RecordingFixtureActorCommand fixtureActorCommand;

    private final HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    @BeforeEach
    void setUp() throws Exception {
        fixtureActorCommand.reset();
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
        createSession("expired-session", "2000-01-01 00:00:00", null);
        createSession("revoked-session", "2999-01-01 00:00:00", "2026-08-08 00:00:00");
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
        fixtureActorCommand.assertActors("user-1");

        HttpResponse<String> logout = post("/api/auth/logout", "{}", sessionCookie);
        assertEquals(204, logout.statusCode());

        fixtureActorCommand.reset();
        HttpResponse<String> afterLogout = post(
                "/api/fixture/actor-context-check",
                "{}",
                sessionCookie
        );

        assertUnauthenticated(afterLogout);
        fixtureActorCommand.assertNoExecutions();
    }

    @Test
    void session_failure_set_and_demo_header_never_execute_fixture() throws Exception {
        List<HttpResponse<String>> responses = List.of(
                post("/api/fixture/actor-context-check", "{}", null),
                postWithHeaders("/api/fixture/actor-context-check", "{}", "Cookie", "ORCA_SESSION="),
                postWithHeaders("/api/fixture/actor-context-check", "{}", "Cookie", "ORCA_SESSION=%%%malformed%%%"),
                postWithHeaders("/api/fixture/actor-context-check", "{}", "Cookie", "ORCA_SESSION=unknown-session"),
                postWithHeaders("/api/fixture/actor-context-check", "{}", "Cookie", "ORCA_SESSION=expired-session"),
                postWithHeaders("/api/fixture/actor-context-check", "{}", "Cookie", "ORCA_SESSION=revoked-session"),
                postWithHeaders(
                        "/api/fixture/actor-context-check",
                        "{}",
                        "Cookie", "ORCA_SESSION=first; ORCA_SESSION=second"
                ),
                postWithHeaders("/api/fixture/actor-context-check", "{}", "X-User-Id", "user-1"),
                post("/api/fixture/actor-context-check?actorId=attacker-controlled", "{}", null)
        );

        for (HttpResponse<String> response : responses) {
            assertUnauthenticated(response);
            assertEquals(responses.getFirst().body(), response.body());
        }
        fixtureActorCommand.assertNoExecutions();
    }

    @ParameterizedTest
    @EnumSource(value = RequestMethod.class, names = {"POST", "PUT", "PATCH", "DELETE"})
    void every_supported_method_establishes_the_session_actor_before_handler_execution(RequestMethod method)
            throws Exception {
        String sessionCookie = loginCookie();

        HttpResponse<String> response = send(
                method,
                "/api/fixture/supported-method-check",
                sessionCookie
        );

        assertEquals(204, response.statusCode());
        fixtureActorCommand.assertActors("user-1");
    }

    @ParameterizedTest
    @EnumSource(value = RequestMethod.class, names = {"POST", "PUT", "PATCH", "DELETE"})
    void every_supported_method_rejects_before_handler_execution_without_a_session(RequestMethod method)
            throws Exception {
        HttpResponse<String> response = send(
                method,
                "/api/fixture/supported-method-check",
                null
        );

        assertUnauthenticated(response);
        fixtureActorCommand.assertNoExecutions();
    }

    @Test
    void attacker_controlled_actor_parameter_cannot_replace_session_actor() throws Exception {
        String sessionCookie = loginCookie();

        HttpResponse<String> response = post(
                "/api/fixture/actor-context-check?actorId=attacker-controlled",
                "{}",
                sessionCookie
        );

        assertEquals(204, response.statusCode());
        fixtureActorCommand.assertActors("user-1");
    }

    @Test
    void attacker_controlled_actor_path_cannot_create_or_replace_session_actor() throws Exception {
        HttpResponse<String> withoutSession = post(
                "/api/fixture/path-actor/attacker-controlled",
                "{}",
                null
        );

        assertUnauthenticated(withoutSession);
        fixtureActorCommand.assertNoExecutions();

        String sessionCookie = loginCookie();
        HttpResponse<String> withSession = post(
                "/api/fixture/path-actor/attacker-controlled",
                "{}",
                sessionCookie
        );

        assertEquals(204, withSession.statusCode());
        fixtureActorCommand.assertActors("user-1");
    }

    @Test
    void attacker_controlled_actor_header_cannot_create_or_replace_session_actor() throws Exception {
        HttpResponse<String> withoutSession = postWithHeaders(
                "/api/fixture/actor-context-check",
                "{}",
                "X-Actor-Id",
                "attacker-controlled"
        );

        assertUnauthenticated(withoutSession);
        fixtureActorCommand.assertNoExecutions();

        String sessionCookie = loginCookie();
        HttpResponse<String> withSession = postWithCookieAndHeader(
                "/api/fixture/actor-context-check",
                "{}",
                sessionCookie,
                "X-Actor-Id",
                "attacker-controlled"
        );

        assertEquals(204, withSession.statusCode());
        fixtureActorCommand.assertActors("user-1");
    }

    @Test
    void protected_declaration_without_embedded_enablement_fails_application_startup() {
        SpringApplication application = new SpringApplication(MissingEnablementApplication.class);
        application.setDefaultProperties(Map.of(
                "server.port", "0",
                "spring.main.banner-mode", "off"
        ));
        application.setLogStartupInfo(false);

        Exception failure = org.junit.jupiter.api.Assertions.assertThrows(Exception.class, application::run);

        assertTrue(rootCauseMessage(failure).contains("@EnableOrcaEmbeddedAuth"));
    }

    @Test
    void attacker_controlled_actor_body_cannot_create_or_replace_session_actor() throws Exception {
        HttpResponse<String> withoutSession = post(
                "/api/fixture/actor-context-check",
                "{\"actorId\":\"attacker-controlled\"}",
                null
        );

        assertUnauthenticated(withoutSession);
        fixtureActorCommand.assertNoExecutions();

        String sessionCookie = loginCookie();

        HttpResponse<String> withSession = post(
                "/api/fixture/actor-context-check",
                "{\"actorId\":\"attacker-controlled\"}",
                sessionCookie
        );

        assertEquals(400, withSession.statusCode());
        assertFalse(withSession.body().contains("user-1"));
        assertFalse(withSession.body().contains(sessionCookie));
        fixtureActorCommand.assertNoExecutions();
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

    private void createSession(String sessionId, String expiresAt, String revokedAt) {
        jdbcTemplate.update(
                """
                INSERT INTO auth_authenticated_sessions
                    (session_id, user_id, created_at, expires_at, revoked_at)
                VALUES (?, ?, ?, ?, ?)
                """,
                sessionId,
                "user-1",
                Timestamp.valueOf("1999-01-01 00:00:00"),
                Timestamp.valueOf(expiresAt),
                revokedAt == null ? null : Timestamp.valueOf(revokedAt)
        );
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

    private HttpResponse<String> postWithCookieAndHeader(
            String path,
            String body,
            String cookie,
            String headerName,
            String headerValue
    ) throws Exception {
        return client.send(
                request(path, body)
                        .header("Cookie", cookie)
                        .header(headerName, headerValue)
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );
    }

    private HttpResponse<String> send(RequestMethod method, String path, String cookie) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(
                        URI.create("http://localhost:%d%s".formatted(port, path))
                )
                .method(method.name(), HttpRequest.BodyPublishers.noBody());
        if (cookie != null) {
            request.header("Cookie", cookie);
        }
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpRequest.Builder request(String path, String body) {
        return HttpRequest.newBuilder(URI.create("http://localhost:%d%s".formatted(port, path)))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body));
    }

    private static void assertUnauthenticated(HttpResponse<String> response) {
        assertEquals(401, response.statusCode());
        assertTrue(response.body().contains("UNAUTHENTICATED"));
        assertFalse(response.body().contains("ORCA_SESSION"));
        assertFalse(response.body().contains("user-1"));
        assertFalse(response.body().contains("revoked"));
        assertFalse(response.body().contains("expired"));
    }

    private static String sha256(String value) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
    }

    private static String rootCauseMessage(Throwable failure) {
        Throwable cause = failure;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause.getMessage() == null ? "" : cause.getMessage();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class RecordingFixtureConfiguration {

        @Bean
        @Primary
        RecordingFixtureActorCommand recordingFixtureActorCommand() {
            return new RecordingFixtureActorCommand();
        }
    }

    private static final class RecordingFixtureActorCommand implements FixtureActorCommand {

        private final List<String> actorIds = new ArrayList<>();

        @Override
        public void handle(String actorId) {
            actorIds.add(actorId);
        }

        void reset() {
            actorIds.clear();
        }

        void assertActors(String... expectedActorIds) {
            assertEquals(List.of(expectedActorIds), actorIds);
        }

        void assertNoExecutions() {
            assertTrue(actorIds.isEmpty(), "fixture handler execution count must be zero");
        }
    }

    @RestController
    static class SupportedMethodController {

        private final RecordingFixtureActorCommand fixtureActorCommand;

        SupportedMethodController(RecordingFixtureActorCommand fixtureActorCommand) {
            this.fixtureActorCommand = fixtureActorCommand;
        }

        @RequestMapping(
                path = "/api/fixture/supported-method-check",
                method = {RequestMethod.POST, RequestMethod.PUT, RequestMethod.PATCH, RequestMethod.DELETE}
        )
        @OrcaProtectedCommand
        ResponseEntity<Void> supportedMethod(AuthenticatedActor actor) {
            fixtureActorCommand.handle(actor.actorId());
            return ResponseEntity.noContent().build();
        }

        @PostMapping("/api/fixture/path-actor/{actorId}")
        @OrcaProtectedCommand
        ResponseEntity<Void> pathActor(AuthenticatedActor actor, @PathVariable String actorId) {
            fixtureActorCommand.handle(actor.actorId());
            return ResponseEntity.noContent().build();
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import(MissingEnablementController.class)
    static class MissingEnablementApplication {
    }

    @RestController
    static class MissingEnablementController {

        @PostMapping("/api/missing-enablement")
        @OrcaProtectedCommand
        void protectedCommand(AuthenticatedActor actor) {
        }
    }
}
