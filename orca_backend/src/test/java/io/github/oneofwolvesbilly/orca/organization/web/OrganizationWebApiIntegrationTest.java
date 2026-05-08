package io.github.oneofwolvesbilly.orca.organization.web;

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
import java.net.http.HttpRequest.BodyPublisher;
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
    private JdbcTemplate jdbcTemplate;

    private final HttpClient client = HttpClient.newHttpClient();

    @BeforeEach
    void setUpUsers() {
        jdbcTemplate.update("DELETE FROM invitation_index");
        jdbcTemplate.update("DELETE FROM group_invitations");
        jdbcTemplate.update("DELETE FROM group_members");
        jdbcTemplate.update("DELETE FROM organization_groups");
        jdbcTemplate.update("DELETE FROM auth_registered_users");

        registerAuthUser("admin");
        registerAuthUser("user-1");
        registerAuthUser("outsider");
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
        String acceptingGroupId = createGroup("admin");
        String rejectingGroupId = createGroup("admin");
        String revokingGroupId = createGroup("admin");

        HttpResponse<String> inviteResponse = post("/api/groups/%s/invitations".formatted(acceptingGroupId), "admin", """
                {
                  "inviteeUserId": "user-1",
                  "intendedRole": "MEMBER"
                }
                """);

        assertEquals(200, inviteResponse.statusCode());
        String invitationToAccept = extract(INVITATION_ID, inviteResponse.body());
        HttpResponse<String> acceptResponse = post(
                "/api/group-invitations/%s/accept".formatted(invitationToAccept),
                "user-1",
                "{}"
        );

        assertEquals(200, acceptResponse.statusCode());
        assertEquals("{\"status\":\"ACCEPTED\"}", acceptResponse.body());

        String invitationToReject = inviteMember(rejectingGroupId, "admin", "outsider");
        HttpResponse<String> rejectResponse = post(
                "/api/group-invitations/%s/reject".formatted(invitationToReject),
                "outsider",
                "{}"
        );

        assertEquals(200, rejectResponse.statusCode());
        assertEquals("{\"status\":\"REJECTED\"}", rejectResponse.body());

        String invitationToRevoke = inviteMember(revokingGroupId, "admin", "outsider");
        HttpResponse<String> revokeResponse = post(
                "/api/group-invitations/%s/revoke".formatted(invitationToRevoke),
                "admin",
                "{}"
        );

        assertEquals(200, revokeResponse.statusCode());
        assertEquals("{\"status\":\"REVOKED\"}", revokeResponse.body());
    }

    @Test
    void command_endpoints_reject_missing_authenticated_user() throws Exception {
        String groupId = createGroup("admin");
        String invitationId = inviteMember(groupId, "admin", "user-1");

        HttpResponse<String> createGroupResponse = postWithoutUser("/api/groups", """
                {
                  "name": "Core Team"
                }
                """);
        HttpResponse<String> inviteMemberResponse = postWithoutUser("/api/groups/%s/invitations".formatted(groupId), """
                {
                  "inviteeUserId": "outsider",
                  "intendedRole": "MEMBER"
                }
                """);
        HttpResponse<String> acceptResponse = postWithoutUser(
                "/api/group-invitations/%s/accept".formatted(invitationId),
                "{}"
        );
        HttpResponse<String> rejectResponse = postWithoutUser(
                "/api/group-invitations/%s/reject".formatted(invitationId),
                "{}"
        );
        HttpResponse<String> revokeResponse = postWithoutUser(
                "/api/group-invitations/%s/revoke".formatted(invitationId),
                "{}"
        );

        assertEquals(401, createGroupResponse.statusCode());
        assertEquals(401, inviteMemberResponse.statusCode());
        assertEquals(401, acceptResponse.statusCode());
        assertEquals(401, rejectResponse.statusCode());
        assertEquals(401, revokeResponse.statusCode());
    }

    @Test
    void command_endpoints_reject_blank_authenticated_user() throws Exception {
        String groupId = createGroup("admin");
        String invitationId = inviteMember(groupId, "admin", "user-1");

        HttpResponse<String> createGroupResponse = post("/api/groups", "   ", """
                {
                  "name": "Core Team"
                }
                """);
        HttpResponse<String> inviteMemberResponse = post("/api/groups/%s/invitations".formatted(groupId), "   ", """
                {
                  "inviteeUserId": "outsider",
                  "intendedRole": "MEMBER"
                }
                """);
        HttpResponse<String> acceptResponse = post(
                "/api/group-invitations/%s/accept".formatted(invitationId),
                "   ",
                "{}"
        );
        HttpResponse<String> rejectResponse = post(
                "/api/group-invitations/%s/reject".formatted(invitationId),
                "   ",
                "{}"
        );
        HttpResponse<String> revokeResponse = post(
                "/api/group-invitations/%s/revoke".formatted(invitationId),
                "   ",
                "{}"
        );

        assertEquals(401, createGroupResponse.statusCode());
        assertEquals(401, inviteMemberResponse.statusCode());
        assertEquals(401, acceptResponse.statusCode());
        assertEquals(401, rejectResponse.statusCode());
        assertEquals(401, revokeResponse.statusCode());
    }

    @Test
    void command_endpoints_reject_multiple_authenticated_user_headers() throws Exception {
        HttpResponse<String> createGroupResponse = post("/api/groups", java.util.List.of("admin", "outsider"), """
                {
                  "name": "Core Team"
                }
                """);

        assertEquals(401, createGroupResponse.statusCode());
    }

    @Test
    void command_endpoints_reject_unknown_authenticated_user() throws Exception {
        HttpResponse<String> response = post("/api/groups", "missing-user", """
                {
                  "name": "Core Team"
                }
                """);

        assertEquals(401, response.statusCode());
    }

    @Test
    void unmapped_api_requests_do_not_require_current_user_context() throws Exception {
        HttpRequest request = requestBuilder("/api/unmapped-command", HttpRequest.BodyPublishers.ofString("{}"))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(404, response.statusCode());
    }

    @Test
    void non_post_requests_to_command_paths_do_not_require_current_user_context() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:%d/api/groups".formatted(port)))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(405, response.statusCode());
    }

    @Test
    void command_endpoints_reject_malformed_or_missing_required_request_bodies() throws Exception {
        String groupId = createGroup("admin");
        String invitationId = inviteMember(groupId, "admin", "user-1");

        HttpResponse<String> malformedCreateGroupResponse = post("/api/groups", "admin", "{");
        HttpResponse<String> missingNameResponse = post("/api/groups", "admin", """
                {
                  "description": "Platform"
                }
                """);
        HttpResponse<String> missingRoleResponse = post("/api/groups/%s/invitations".formatted(groupId), "admin", """
                {
                  "inviteeUserId": "user-1"
                }
                """);
        HttpResponse<String> malformedAcceptResponse = post(
                "/api/group-invitations/%s/accept".formatted(invitationId),
                "user-1",
                "{"
        );
        HttpResponse<String> missingRejectBodyResponse = postWithoutBody(
                "/api/group-invitations/%s/reject".formatted(invitationId),
                "user-1"
        );
        HttpResponse<String> missingRevokeBodyResponse = postWithoutBody(
                "/api/group-invitations/%s/revoke".formatted(invitationId),
                "admin"
        );

        assertEquals(400, malformedCreateGroupResponse.statusCode());
        assertEquals(400, missingNameResponse.statusCode());
        assertEquals(400, missingRoleResponse.statusCode());
        assertEquals(400, malformedAcceptResponse.statusCode());
        assertEquals(400, missingRejectBodyResponse.statusCode());
        assertEquals(400, missingRevokeBodyResponse.statusCode());
    }

    @Test
    void command_endpoints_map_unknown_group_or_invitation_to_not_found() throws Exception {
        HttpResponse<String> missingGroupResponse = post("/api/groups/missing-group/invitations", "admin", """
                {
                  "inviteeUserId": "user-1",
                  "intendedRole": "MEMBER"
                }
                """);
        HttpResponse<String> missingAcceptInvitationResponse = post(
                "/api/group-invitations/missing-invitation/accept",
                "user-1",
                "{}"
        );
        HttpResponse<String> missingRejectInvitationResponse = post(
                "/api/group-invitations/missing-invitation/reject",
                "user-1",
                "{}"
        );
        HttpResponse<String> missingRevokeInvitationResponse = post(
                "/api/group-invitations/missing-invitation/revoke",
                "admin",
                "{}"
        );

        assertEquals(404, missingGroupResponse.statusCode());
        assertEquals(404, missingAcceptInvitationResponse.statusCode());
        assertEquals(404, missingRejectInvitationResponse.statusCode());
        assertEquals(404, missingRevokeInvitationResponse.statusCode());
    }

    @Test
    void command_endpoints_map_actor_permission_mismatch_to_forbidden() throws Exception {
        String invitationGroupId = createGroup("admin");
        String acceptGroupId = createGroup("admin");
        String rejectGroupId = createGroup("admin");
        String revokeGroupId = createGroup("admin");
        String invitationForAccept = inviteMember(acceptGroupId, "admin", "user-1");
        String invitationForReject = inviteMember(rejectGroupId, "admin", "outsider");
        String invitationForRevoke = inviteMember(revokeGroupId, "admin", "user-1");

        HttpResponse<String> inviteForbiddenResponse = post("/api/groups/%s/invitations".formatted(invitationGroupId), "outsider", """
                {
                  "inviteeUserId": "user-1",
                  "intendedRole": "MEMBER"
                }
                """);
        HttpResponse<String> acceptForbiddenResponse = post(
                "/api/group-invitations/%s/accept".formatted(invitationForAccept),
                "outsider",
                "{}"
        );
        HttpResponse<String> rejectForbiddenResponse = post(
                "/api/group-invitations/%s/reject".formatted(invitationForReject),
                "user-1",
                "{}"
        );
        HttpResponse<String> revokeForbiddenResponse = post(
                "/api/group-invitations/%s/revoke".formatted(invitationForRevoke),
                "outsider",
                "{}"
        );

        assertEquals(403, inviteForbiddenResponse.statusCode());
        assertEquals(403, acceptForbiddenResponse.statusCode());
        assertEquals(403, rejectForbiddenResponse.statusCode());
        assertEquals(403, revokeForbiddenResponse.statusCode());
    }

    @Test
    void command_endpoints_map_other_domain_or_application_validation_failures_to_bad_request() throws Exception {
        String groupId = createGroup("admin");
        String acceptedInvitationId = inviteMember(groupId, "admin", "user-1");

        HttpResponse<String> acceptResponse = post(
                "/api/group-invitations/%s/accept".formatted(acceptedInvitationId),
                "user-1",
                "{}"
        );
        HttpResponse<String> duplicatePendingInvitationResponse = post("/api/groups/%s/invitations".formatted(groupId), "admin", """
                {
                  "inviteeUserId": "outsider",
                  "intendedRole": "MEMBER"
                }
                """);
        HttpResponse<String> duplicatePendingInvitationRetryResponse = post("/api/groups/%s/invitations".formatted(groupId), "admin", """
                {
                  "inviteeUserId": "outsider",
                  "intendedRole": "MEMBER"
                }
                """);
        HttpResponse<String> inviteUnknownUserResponse = post("/api/groups/%s/invitations".formatted(groupId), "admin", """
                {
                  "inviteeUserId": "missing-user",
                  "intendedRole": "MEMBER"
                }
                """);
        HttpResponse<String> acceptAlreadyAcceptedResponse = post(
                "/api/group-invitations/%s/accept".formatted(acceptedInvitationId),
                "user-1",
                "{}"
        );

        assertEquals(200, acceptResponse.statusCode());
        assertEquals(200, duplicatePendingInvitationResponse.statusCode());
        assertEquals(400, duplicatePendingInvitationRetryResponse.statusCode());
        assertEquals(400, inviteUnknownUserResponse.statusCode());
        assertEquals(400, acceptAlreadyAcceptedResponse.statusCode());
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
        return post(path, java.util.List.of(actorUserId), body);
    }

    private HttpResponse<String> post(String path, java.util.List<String> actorUserIds, String body) throws Exception {
        HttpRequest request = requestBuilder(path, HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpRequest.Builder rebased = HttpRequest.newBuilder(request.uri())
                .method(request.method(), request.bodyPublisher().orElse(HttpRequest.BodyPublishers.noBody()));
        request.headers().map().forEach((name, values) -> values.forEach(value -> rebased.header(name, value)));
        actorUserIds.forEach(actorUserId -> rebased.header("X-User-Id", actorUserId));
        request = rebased.build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> postWithoutUser(String path, String body) throws Exception {
        HttpRequest request = requestBuilder(path, HttpRequest.BodyPublishers.ofString(body)).build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> postWithoutBody(String path, String actorUserId) throws Exception {
        HttpRequest request = requestBuilder(path, HttpRequest.BodyPublishers.noBody())
                .header("X-User-Id", actorUserId)
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpRequest.Builder requestBuilder(String path, BodyPublisher bodyPublisher) {
        return HttpRequest.newBuilder(URI.create("http://localhost:%d%s".formatted(port, path)))
                .header("Content-Type", "application/json")
                .POST(bodyPublisher);
    }

    private static String extract(Pattern pattern, String body) {
        return Optional.of(pattern.matcher(body))
                .filter(matcher -> matcher.find())
                .map(matcher -> matcher.group(1))
                .orElseThrow(() -> new AssertionError("Response body did not match " + pattern + ": " + body));
    }

    private void registerAuthUser(String userId) {
        jdbcTemplate.update("INSERT INTO auth_registered_users (user_id) VALUES (?)", userId);
    }
}
