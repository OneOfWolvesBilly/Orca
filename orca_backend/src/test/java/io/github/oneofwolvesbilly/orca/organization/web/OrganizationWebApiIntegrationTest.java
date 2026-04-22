package io.github.oneofwolvesbilly.orca.organization.web;

import io.github.oneofwolvesbilly.orca.OrcaApplication;
import io.github.oneofwolvesbilly.orca.organization.application.RegisteredUserDirectory;
import io.github.oneofwolvesbilly.orca.organization.domain.UserId;
import io.github.oneofwolvesbilly.orca.organization.infrastructure.inmemory.InMemoryRegisteredUserDirectory;
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
import java.util.Optional;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@SpringBootTest(classes = OrcaApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class OrganizationWebApiIntegrationTest {

    private static final Pattern GROUP_ID = Pattern.compile("\"groupId\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern INVITATION_ID = Pattern.compile("\"invitationId\"\\s*:\\s*\"([^\"]+)\"");

    @LocalServerPort
    private int port;

    @Autowired
    private RegisteredUserDirectory registeredUserDirectory;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final HttpClient client = HttpClient.newHttpClient();

    @BeforeEach
    void setUpUsers() {
        jdbcTemplate.update("DELETE FROM invitation_index");
        jdbcTemplate.update("DELETE FROM group_invitations");
        jdbcTemplate.update("DELETE FROM group_members");
        jdbcTemplate.update("DELETE FROM organization_groups");

        InMemoryRegisteredUserDirectory users = (InMemoryRegisteredUserDirectory) registeredUserDirectory;
        users.clear();
        users.register(UserId.of("admin"));
        users.register(UserId.of("user-1"));
        users.register(UserId.of("outsider"));
    }

    @Test
    void create_group_uses_authenticated_user_and_returns_group_id() throws Exception {
        HttpResponse<String> response = post("/api/groups", "admin", """
                {
                  "name": "Core Team",
                  "description": "Platform"
                }
                """);

        assertEquals(200, response.statusCode());
        assertFalse(extract(GROUP_ID, response.body()).isBlank());
    }

    @Test
    void invite_member_accept_reject_and_revoke_flow_uses_post_contracts() throws Exception {
        String groupId = createGroup("admin");

        String invitationToAccept = inviteMember(groupId, "admin", "user-1");
        HttpResponse<String> acceptResponse = post(
                "/api/group-invitations/%s/accept".formatted(invitationToAccept),
                "user-1",
                "{}"
        );

        assertEquals(200, acceptResponse.statusCode());
        assertEquals("{\"status\":\"ACCEPTED\"}", acceptResponse.body());

        String invitationToReject = inviteMember(groupId, "admin", "outsider");
        HttpResponse<String> rejectResponse = post(
                "/api/group-invitations/%s/reject".formatted(invitationToReject),
                "outsider",
                "{}"
        );

        assertEquals(200, rejectResponse.statusCode());
        assertEquals("{\"status\":\"REJECTED\"}", rejectResponse.body());

        String invitationToRevoke = inviteMember(groupId, "admin", "outsider");
        HttpResponse<String> revokeResponse = post(
                "/api/group-invitations/%s/revoke".formatted(invitationToRevoke),
                "admin",
                "{}"
        );

        assertEquals(200, revokeResponse.statusCode());
        assertEquals("{\"status\":\"REVOKED\"}", revokeResponse.body());
    }

    @Test
    void command_endpoint_rejects_missing_authenticated_user_before_use_case_execution() throws Exception {
        HttpResponse<String> response = postWithoutUser("/api/groups", """
                {
                  "name": "Core Team"
                }
                """);

        assertEquals(401, response.statusCode());
    }

    @Test
    void command_endpoint_rejects_missing_required_body_fields_as_validation_errors() throws Exception {
        HttpResponse<String> missingNameResponse = post("/api/groups", "admin", """
                {
                  "description": "Platform"
                }
                """);

        assertEquals(400, missingNameResponse.statusCode());

        String groupId = createGroup("admin");

        HttpResponse<String> missingRoleResponse = post("/api/groups/%s/invitations".formatted(groupId), "admin", """
                {
                  "inviteeUserId": "user-1"
                }
                """);

        assertEquals(400, missingRoleResponse.statusCode());
    }

    @Test
    void invite_member_maps_not_found_and_forbidden_failures_to_http_errors() throws Exception {
        HttpResponse<String> missingGroupResponse = post("/api/groups/missing-group/invitations", "admin", """
                {
                  "inviteeUserId": "user-1",
                  "intendedRole": "MEMBER"
                }
                """);

        assertEquals(404, missingGroupResponse.statusCode());

        String groupId = createGroup("admin");

        HttpResponse<String> forbiddenResponse = post("/api/groups/%s/invitations".formatted(groupId), "outsider", """
                {
                  "inviteeUserId": "user-1",
                  "intendedRole": "MEMBER"
                }
                """);

        assertEquals(403, forbiddenResponse.statusCode());
    }

    private String createGroup(String actorUserId) throws Exception {
        HttpResponse<String> response = post("/api/groups", actorUserId, """
                {
                  "name": "Core Team"
                }
                """);

        assertEquals(200, response.statusCode());
        return extract(GROUP_ID, response.body());
    }

    private String inviteMember(String groupId, String actorUserId, String inviteeUserId) throws Exception {
        HttpResponse<String> response = post("/api/groups/%s/invitations".formatted(groupId), actorUserId, """
                {
                  "inviteeUserId": "%s",
                  "intendedRole": "MEMBER"
                }
                """.formatted(inviteeUserId));

        assertEquals(200, response.statusCode());
        return extract(INVITATION_ID, response.body());
    }

    private HttpResponse<String> post(String path, String actorUserId, String body) throws Exception {
        HttpRequest request = requestBuilder(path, body)
                .header("X-User-Id", actorUserId)
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> postWithoutUser(String path, String body) throws Exception {
        HttpRequest request = requestBuilder(path, body).build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpRequest.Builder requestBuilder(String path, String body) {
        return HttpRequest.newBuilder(URI.create("http://localhost:%d%s".formatted(port, path)))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body));
    }

    private static String extract(Pattern pattern, String body) {
        return Optional.of(pattern.matcher(body))
                .filter(matcher -> matcher.find())
                .map(matcher -> matcher.group(1))
                .orElseThrow(() -> new AssertionError("Response body did not match " + pattern + ": " + body));
    }
}
