package io.github.oneofwolvesbilly.orca.organization.web;

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
import java.net.http.HttpRequest.BodyPublisher;
import java.util.Optional;
import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@SpringBootTest(classes = OrcaApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class OrganizationWebApiIntegrationTest {

    private static final Pattern GROUP_ID = Pattern.compile("\"groupId\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern INVITATION_ID = Pattern.compile("\"invitationId\"\\s*:\\s*\"([^\"]+)\"");
    private static final String AUTH_SESSION_COOKIE_NAME = "ORCA_SESSION";

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUpUsers() {
        jdbcTemplate.update("DELETE FROM invitation_index");
        jdbcTemplate.update("DELETE FROM group_invitations");
        jdbcTemplate.update("DELETE FROM group_members");
        jdbcTemplate.update("DELETE FROM organization_groups");
        jdbcTemplate.update("DELETE FROM auth_authenticated_sessions");
        jdbcTemplate.update("DELETE FROM auth_login_credentials");
        jdbcTemplate.update("DELETE FROM auth_system_role_assignments");
        jdbcTemplate.update("DELETE FROM auth_registered_users");

        registerAuthUser("admin");
        registerAuthUser("user-1");
        registerAuthUser("outsider");
        createSession("session-admin", "admin", "2026-05-29 00:00:00", "2999-01-01 00:00:00");
        createSession("session-user-1", "user-1", "2026-05-29 00:00:00", "2999-01-01 00:00:00");
        createSession("session-outsider", "outsider", "2026-05-29 00:00:00", "2999-01-01 00:00:00");
        createSession("session-expired", "admin", "2000-01-01 00:00:00", "2000-01-01 08:00:00");
    }

    @Test
    void create_group_uses_session_authenticated_user_and_returns_group_id() throws Exception {
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
    void create_group_accepts_session_cookie_issued_by_password_login() throws Exception {
        jdbcTemplate.update(
                "INSERT INTO auth_login_credentials (login_identifier, password_hash, user_id) VALUES (?, ?, ?)",
                "admin-login",
                JdbcLoginCredentialVerifier.hashPasswordForStorage("correct-password"),
                "admin"
        );
        HttpResponse<String> loginResponse = postLogin("""
                {
                  "loginIdentifier": "admin-login",
                  "password": "correct-password"
                }
                """);
        String sessionCookie = loginResponse.headers().firstValue("Set-Cookie")
                .map(value -> value.split(";", 2)[0])
                .orElseThrow(() -> new AssertionError("Login response did not issue a session cookie"));

        HttpResponse<String> createGroupResponse = postWithCookie("/api/groups", sessionCookie, """
                {
                  "name": "Core Team",
                  "description": "Platform"
                }
                """);

        assertEquals(204, loginResponse.statusCode());
        assertEquals(200, createGroupResponse.statusCode());
        assertFalse(extract(GROUP_ID, createGroupResponse.body()).isBlank());
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
    void command_endpoints_reject_missing_session_cookie() throws Exception {
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
    void command_endpoints_reject_blank_session_id() throws Exception {
        String groupId = createGroup("admin");
        String invitationId = inviteMember(groupId, "admin", "user-1");

        HttpResponse<String> createGroupResponse = postWithSessionId("/api/groups", "   ", """
                {
                  "name": "Core Team"
                }
                """);
        HttpResponse<String> inviteMemberResponse = postWithSessionId("/api/groups/%s/invitations".formatted(groupId), "   ", """
                {
                  "inviteeUserId": "outsider",
                  "intendedRole": "MEMBER"
                }
                """);
        HttpResponse<String> acceptResponse = postWithSessionId(
                "/api/group-invitations/%s/accept".formatted(invitationId),
                "   ",
                "{}"
        );
        HttpResponse<String> rejectResponse = postWithSessionId(
                "/api/group-invitations/%s/reject".formatted(invitationId),
                "   ",
                "{}"
        );
        HttpResponse<String> revokeResponse = postWithSessionId(
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
    void command_endpoints_reject_unknown_and_expired_sessions_with_same_response() throws Exception {
        HttpResponse<String> unknownSessionResponse = postWithSessionId("/api/groups", "missing-session", """
                {
                  "name": "Core Team"
                }
                """);
        HttpResponse<String> expiredSessionResponse = postWithSessionId("/api/groups", "session-expired", """
                {
                  "name": "Core Team"
                }
                """);

        assertEquals(401, unknownSessionResponse.statusCode());
        assertEquals(401, expiredSessionResponse.statusCode());
        assertEquals(unknownSessionResponse.body(), expiredSessionResponse.body());
    }

    @Test
    void command_endpoints_reject_x_user_id_without_valid_session() throws Exception {
        HttpRequest request = requestBuilder("/api/groups", HttpRequest.BodyPublishers.ofString("""
                        {
                          "name": "Core Team"
                        }
                        """))
                .header("X-User-Id", "admin")
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
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
        assertError(missingNameResponse, 400, "VALIDATION_ERROR", "Request validation failed");
        assertError(missingRejectBodyResponse, 400, "VALIDATION_ERROR", "Request validation failed");
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
        assertError(missingGroupResponse, 404, "NOT_FOUND", "Requested resource was not found");
        assertError(missingAcceptInvitationResponse, 404, "NOT_FOUND", "Requested resource was not found");
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
        assertError(inviteForbiddenResponse, 403, "FORBIDDEN", "Operation is forbidden");
        assertError(acceptForbiddenResponse, 403, "FORBIDDEN", "Operation is forbidden");
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
        assertError(
                duplicatePendingInvitationRetryResponse,
                400,
                "APPLICATION_REJECTED",
                "Request was rejected"
        );
        assertError(inviteUnknownUserResponse, 400, "APPLICATION_REJECTED", "Request was rejected");
        assertError(acceptAlreadyAcceptedResponse, 400, "APPLICATION_REJECTED", "Request was rejected");
        assertFalse(duplicatePendingInvitationRetryResponse.body().contains("Pending invitation already exists"));
    }

    @Test
    void command_endpoints_map_every_terminal_invitation_retry_to_application_rejected() throws Exception {
        String acceptGroup = createGroup("admin");
        String rejectGroup = createGroup("admin");
        String revokeGroup = createGroup("admin");
        String accepted = inviteMember(acceptGroup, "admin", "user-1");
        String rejected = inviteMember(rejectGroup, "admin", "user-1");
        String revoked = inviteMember(revokeGroup, "admin", "user-1");

        assertEquals(200, post("/api/group-invitations/%s/accept".formatted(accepted), "user-1", "{}").statusCode());
        assertEquals(200, post("/api/group-invitations/%s/reject".formatted(rejected), "user-1", "{}").statusCode());
        assertEquals(200, post("/api/group-invitations/%s/revoke".formatted(revoked), "admin", "{}").statusCode());

        assertError(
                post("/api/group-invitations/%s/accept".formatted(accepted), "user-1", "{}"),
                400,
                "APPLICATION_REJECTED",
                "Request was rejected"
        );
        assertError(
                post("/api/group-invitations/%s/reject".formatted(rejected), "user-1", "{}"),
                400,
                "APPLICATION_REJECTED",
                "Request was rejected"
        );
        assertError(
                post("/api/group-invitations/%s/revoke".formatted(revoked), "admin", "{}"),
                400,
                "APPLICATION_REJECTED",
                "Request was rejected"
        );
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
        return postWithSessionId(path, "session-" + actorUserId, body);
    }

    private HttpResponse<String> postWithSessionId(String path, String sessionId, String body) throws Exception {
        return postWithCookie(path, "%s=%s".formatted(AUTH_SESSION_COOKIE_NAME, sessionId), body);
    }

    private HttpResponse<String> postWithCookie(String path, String cookie, String body) throws Exception {
        HttpRequest request = requestBuilder(path, HttpRequest.BodyPublishers.ofString(body))
                .header("Cookie", cookie)
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> postLogin(String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:%d/api/auth/login".formatted(port)))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> postWithoutUser(String path, String body) throws Exception {
        HttpRequest request = requestBuilder(path, HttpRequest.BodyPublishers.ofString(body)).build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> postWithoutBody(String path, String actorUserId) throws Exception {
        HttpRequest request = requestBuilder(path, HttpRequest.BodyPublishers.noBody())
                .header("Cookie", "%s=session-%s".formatted(AUTH_SESSION_COOKIE_NAME, actorUserId))
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

    private void assertError(HttpResponse<String> response, int status, String code, String message) throws Exception {
        Map<String, Object> body = objectMapper.readValue(response.body(), new TypeReference<>() {
        });
        assertEquals(status, body.get("status"));
        assertEquals(code, body.get("code"));
        assertEquals(message, body.get("message"));
        assertEquals(3, body.size());
    }

    private void registerAuthUser(String userId) {
        jdbcTemplate.update("INSERT INTO auth_registered_users (user_id) VALUES (?)", userId);
    }

    private void createSession(String sessionId, String userId, String createdAt, String expiresAt) {
        jdbcTemplate.update(
                "INSERT INTO auth_authenticated_sessions (session_id, user_id, created_at, expires_at) VALUES (?, ?, ?, ?)",
                sessionId,
                userId,
                createdAt,
                expiresAt
        );
    }
}
