package io.github.oneofwolvesbilly.orcaconsumer;

import org.flywaydb.core.Flyway;
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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        classes = PublishedArtifactConsumerApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
class PublishedArtifactConsumerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RecordingActorCommand command;

    @Autowired
    private Flyway flyway;

    private final HttpClient client = HttpClient.newHttpClient();

    @BeforeEach
    void setUp() throws Exception {
        command.reset();
        jdbcTemplate.update("DELETE FROM auth_login_failure_audits");
        jdbcTemplate.update("DELETE FROM auth_authenticated_sessions");
        jdbcTemplate.update("DELETE FROM auth_login_credentials");
        jdbcTemplate.update("DELETE FROM auth_system_role_assignments");
        jdbcTemplate.update("DELETE FROM auth_registered_users");
        jdbcTemplate.update(
                "INSERT INTO auth_registered_users (user_id) VALUES (?)",
                "published-consumer-user"
        );
        jdbcTemplate.update(
                """
                INSERT INTO auth_login_credentials (login_identifier, password_hash, user_id)
                VALUES (?, ?, ?)
                """,
                "published-consumer-login",
                sha256("published-consumer-password"),
                "published-consumer-user"
        );
    }

    @Test
    void login_resolves_public_actor_and_logout_rejects_reuse() throws Exception {
        HttpResponse<String> login = post(
                "/api/auth/login",
                """
                {
                  "loginIdentifier": "published-consumer-login",
                  "password": "published-consumer-password"
                }
                """,
                null
        );

        assertEquals(204, login.statusCode());
        String sessionCookie = login.headers().firstValue("Set-Cookie")
                .orElseThrow()
                .split(";", 2)[0];
        assertFalse(sessionCookie.contains("published-consumer-user"));

        HttpResponse<String> protectedResponse = post(
                "/api/fixture/actor-context-check",
                "{}",
                sessionCookie
        );

        assertEquals(204, protectedResponse.statusCode());
        assertEquals("", protectedResponse.body());
        assertEquals(java.util.List.of("published-consumer-user"), command.actorIds());

        assertEquals(204, post("/api/auth/logout", "{}", sessionCookie).statusCode());
        command.reset();

        HttpResponse<String> rejected = post(
                "/api/fixture/actor-context-check",
                "{}",
                sessionCookie
        );

        assertUnauthenticated(rejected);
        assertTrue(command.actorIds().isEmpty());
    }

    @Test
    void missing_session_rejects_before_consumer_command() throws Exception {
        HttpResponse<String> response = post(
                "/api/fixture/actor-context-check",
                "{}",
                null
        );

        assertUnauthenticated(response);
        assertTrue(command.actorIds().isEmpty());
    }

    @Test
    void packaged_migrations_start_an_empty_schema() {
        Integer migrationCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM flyway_schema_history
                WHERE success = TRUE AND version IS NOT NULL
                """,
                Integer.class
        );

        assertEquals(8, migrationCount);
        assertEquals(
                0,
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM auth_authenticated_sessions",
                        Integer.class
                )
        );
    }

    @Test
    void startup_against_current_schema_does_not_add_migration() {
        assertEquals(0, flyway.migrate().migrationsExecuted);
    }

    private HttpResponse<String> post(String path, String body, String cookie) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(
                        URI.create("http://localhost:%d%s".formatted(port, path))
                )
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body));
        if (cookie != null) {
            request.header("Cookie", cookie);
        }
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static void assertUnauthenticated(HttpResponse<String> response) {
        assertEquals(401, response.statusCode());
        assertTrue(response.body().contains("UNAUTHENTICATED"));
        assertFalse(response.body().contains("ORCA_SESSION"));
        assertFalse(response.body().contains("published-consumer-user"));
    }

    private static String sha256(String value) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
    }
}
