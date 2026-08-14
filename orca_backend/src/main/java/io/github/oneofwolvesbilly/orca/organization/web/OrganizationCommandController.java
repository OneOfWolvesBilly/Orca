package io.github.oneofwolvesbilly.orca.organization.web;

import io.github.oneofwolvesbilly.orca.auth.api.OrcaProtectedCommand;
import io.github.oneofwolvesbilly.orca.auth.domain.CurrentUserContext;
import io.github.oneofwolvesbilly.orca.organization.application.AcceptInvitationCommand;
import io.github.oneofwolvesbilly.orca.organization.application.AcceptInvitationUseCase;
import io.github.oneofwolvesbilly.orca.organization.application.CreateGroupCommand;
import io.github.oneofwolvesbilly.orca.organization.application.CreateGroupUseCase;
import io.github.oneofwolvesbilly.orca.organization.application.InviteMemberCommand;
import io.github.oneofwolvesbilly.orca.organization.application.InviteMemberUseCase;
import io.github.oneofwolvesbilly.orca.organization.application.RejectInvitationCommand;
import io.github.oneofwolvesbilly.orca.organization.application.RejectInvitationUseCase;
import io.github.oneofwolvesbilly.orca.organization.application.RevokeInvitationCommand;
import io.github.oneofwolvesbilly.orca.organization.application.RevokeInvitationUseCase;
import io.github.oneofwolvesbilly.orca.organization.domain.GroupId;
import io.github.oneofwolvesbilly.orca.organization.domain.GroupInvitationId;
import io.github.oneofwolvesbilly.orca.organization.domain.GroupRole;
import io.github.oneofwolvesbilly.orca.organization.domain.InvitationStatus;
import io.github.oneofwolvesbilly.orca.organization.domain.UserId;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
@RequestMapping("/api")
final class OrganizationCommandController {

    private final CreateGroupUseCase createGroupUseCase;
    private final InviteMemberUseCase inviteMemberUseCase;
    private final AcceptInvitationUseCase acceptInvitationUseCase;
    private final RejectInvitationUseCase rejectInvitationUseCase;
    private final RevokeInvitationUseCase revokeInvitationUseCase;

    OrganizationCommandController(
            CreateGroupUseCase createGroupUseCase,
            InviteMemberUseCase inviteMemberUseCase,
            AcceptInvitationUseCase acceptInvitationUseCase,
            RejectInvitationUseCase rejectInvitationUseCase,
            RevokeInvitationUseCase revokeInvitationUseCase
    ) {
        this.createGroupUseCase = Objects.requireNonNull(createGroupUseCase, "createGroupUseCase");
        this.inviteMemberUseCase = Objects.requireNonNull(inviteMemberUseCase, "inviteMemberUseCase");
        this.acceptInvitationUseCase = Objects.requireNonNull(acceptInvitationUseCase, "acceptInvitationUseCase");
        this.rejectInvitationUseCase = Objects.requireNonNull(rejectInvitationUseCase, "rejectInvitationUseCase");
        this.revokeInvitationUseCase = Objects.requireNonNull(revokeInvitationUseCase, "revokeInvitationUseCase");
    }

    @PostMapping("/groups")
    @OrcaProtectedCommand
    CreateGroupResponse createGroup(
            CurrentUserContext currentUserContext,
            @RequestBody CreateGroupRequest request
    ) {
        requiredRequest(request);
        UserId actor = authenticatedUser(currentUserContext);
        var result = createGroupUseCase.handle(new CreateGroupCommand(
                actor,
                requiredField(request.name(), "name"),
                request.description()
        ));
        return new CreateGroupResponse(result.groupId().value());
    }

    @PostMapping("/groups/{groupId}/invitations")
    @OrcaProtectedCommand
    InviteMemberResponse inviteMember(
            CurrentUserContext currentUserContext,
            @PathVariable String groupId,
            @RequestBody InviteMemberRequest request
    ) {
        requiredRequest(request);
        UserId actor = authenticatedUser(currentUserContext);
        var result = inviteMemberUseCase.handle(new InviteMemberCommand(
                GroupId.of(groupId),
                actor,
                UserId.of(requiredField(request.inviteeUserId(), "inviteeUserId")),
                requiredRole(request.intendedRole())
        ));
        return new InviteMemberResponse(result.invitationId().value());
    }

    @PostMapping("/group-invitations/{invitationId}/accept")
    @OrcaProtectedCommand
    InvitationStatusResponse acceptInvitation(
            CurrentUserContext currentUserContext,
            @PathVariable String invitationId,
            @RequestBody InvitationActionRequest request
    ) {
        requiredRequest(request);
        acceptInvitationUseCase.handle(new AcceptInvitationCommand(
                authenticatedUser(currentUserContext),
                GroupInvitationId.of(invitationId)
        ));
        return new InvitationStatusResponse(InvitationStatus.ACCEPTED.name());
    }

    @PostMapping("/group-invitations/{invitationId}/reject")
    @OrcaProtectedCommand
    InvitationStatusResponse rejectInvitation(
            CurrentUserContext currentUserContext,
            @PathVariable String invitationId,
            @RequestBody InvitationActionRequest request
    ) {
        requiredRequest(request);
        rejectInvitationUseCase.handle(new RejectInvitationCommand(
                authenticatedUser(currentUserContext),
                GroupInvitationId.of(invitationId)
        ));
        return new InvitationStatusResponse(InvitationStatus.REJECTED.name());
    }

    @PostMapping("/group-invitations/{invitationId}/revoke")
    @OrcaProtectedCommand
    InvitationStatusResponse revokeInvitation(
            CurrentUserContext currentUserContext,
            @PathVariable String invitationId,
            @RequestBody InvitationActionRequest request
    ) {
        requiredRequest(request);
        revokeInvitationUseCase.handle(new RevokeInvitationCommand(
                authenticatedUser(currentUserContext),
                GroupInvitationId.of(invitationId)
        ));
        return new InvitationStatusResponse(InvitationStatus.REVOKED.name());
    }

    private UserId authenticatedUser(CurrentUserContext currentUserContext) {
        return UserId.of(currentUserContext.authenticatedUserId().value());
    }

    private static String requiredField(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value;
    }

    private static GroupRole requiredRole(GroupRole role) {
        if (role == null) {
            throw new IllegalArgumentException("intendedRole is required");
        }
        return role;
    }

    private static void requiredRequest(Object request) {
        if (request == null) {
            throw new IllegalArgumentException("request body is required");
        }
    }

    record CreateGroupRequest(String name, String description) {
    }

    record CreateGroupResponse(String groupId) {
    }

    record InviteMemberRequest(String inviteeUserId, GroupRole intendedRole) {
    }

    record InviteMemberResponse(String invitationId) {
    }

    record InvitationActionRequest() {
    }

    record InvitationStatusResponse(String status) {
    }
}
