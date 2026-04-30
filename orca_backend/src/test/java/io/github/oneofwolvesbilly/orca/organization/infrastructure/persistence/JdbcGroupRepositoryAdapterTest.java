package io.github.oneofwolvesbilly.orca.organization.infrastructure.persistence;

import io.github.oneofwolvesbilly.orca.organization.application.GroupRepository;
import io.github.oneofwolvesbilly.orca.organization.domain.Group;
import io.github.oneofwolvesbilly.orca.organization.domain.GroupId;
import io.github.oneofwolvesbilly.orca.organization.domain.GroupInvitation;
import io.github.oneofwolvesbilly.orca.organization.domain.GroupInvitationId;
import io.github.oneofwolvesbilly.orca.organization.domain.GroupDescription;
import io.github.oneofwolvesbilly.orca.organization.domain.GroupName;
import io.github.oneofwolvesbilly.orca.organization.domain.GroupRole;
import io.github.oneofwolvesbilly.orca.organization.domain.InvitationStatus;
import io.github.oneofwolvesbilly.orca.organization.domain.UserId;
import org.flywaydb.core.Flyway;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcGroupRepositoryAdapterTest {

    private DataSource dataSource;
    private GroupRepository repository;

    @BeforeEach
    void setUp() {
        this.dataSource = newDataSource();
        migrate(dataSource);
        this.repository = new JdbcGroupRepositoryAdapter(dataSource, new GroupEntityMapper());
    }

    @Test
    void should_persist_and_reload_group_by_id_with_pending_invitation() {
        Group group = Group.create(
                GroupId.of("g1"),
                GroupName.of("team"),
                GroupDescription.of("core team"),
                UserId.of("admin")
        );

        var invitation = group.inviteMember(
                UserId.of("admin"),
                UserId.of("user-1"),
                GroupRole.MEMBER
        );

        GroupInvitationId invitationId = invitation.id();

        repository.save(group);

        Group reloaded = repository.findById(GroupId.of("g1")).orElseThrow();

        assertEquals("g1", reloaded.id().value());
        assertEquals("team", reloaded.name().value());
        assertEquals("core team", reloaded.description().orElseThrow().value());
        assertEquals(1, reloaded.members().size());
        assertTrue(reloaded.hasPendingInvitationFor(UserId.of("user-1")));
        assertEquals(InvitationStatus.PENDING, invitationStatus(reloaded, invitationId));
        assertTrue(repository.findByInvitationId(invitationId).isPresent());

        reloaded.acceptInvitation(invitationId, UserId.of("user-1"));

        assertEquals(2, reloaded.members().size());
        assertFalse(reloaded.hasPendingInvitationFor(UserId.of("user-1")));
    }

    @Test
    void should_resolve_group_by_invitation_id_after_persistence() {
        Group group = groupWithPendingInvitation("g1", "user-1");
        GroupInvitationId invitationId = group.invitations().getFirst().id();

        repository.save(group);

        Group reloaded = repository.findByInvitationId(invitationId).orElseThrow();

        assertEquals("g1", reloaded.id().value());
        assertTrue(reloaded.hasPendingInvitationFor(UserId.of("user-1")));
    }

    @Test
    void should_persist_accepted_invitation_state_and_membership() {
        Group group = groupWithPendingInvitation("g1", "user-1");
        GroupInvitationId invitationId = group.invitations().getFirst().id();
        repository.save(group);

        Group reloaded = repository.findByInvitationId(invitationId).orElseThrow();
        reloaded.acceptInvitation(invitationId, UserId.of("user-1"));
        repository.save(reloaded);

        Group afterRestart = repository.findByInvitationId(invitationId).orElseThrow();

        assertEquals(InvitationStatus.ACCEPTED, invitationStatus(afterRestart, invitationId));
        assertFalse(afterRestart.hasPendingInvitationFor(UserId.of("user-1")));
        assertEquals(2, afterRestart.members().size());
    }

    @Test
    void should_persist_rejected_invitation_state_and_pending_tracking() {
        Group group = groupWithPendingInvitation("g1", "user-1");
        GroupInvitationId invitationId = group.invitations().getFirst().id();
        repository.save(group);

        Group reloaded = repository.findByInvitationId(invitationId).orElseThrow();
        reloaded.rejectInvitation(invitationId, UserId.of("user-1"));
        repository.save(reloaded);

        Group afterRestart = repository.findByInvitationId(invitationId).orElseThrow();

        assertEquals(InvitationStatus.REJECTED, invitationStatus(afterRestart, invitationId));
        assertFalse(afterRestart.hasPendingInvitationFor(UserId.of("user-1")));
        assertEquals(1, afterRestart.members().size());
    }

    @Test
    void should_persist_revoked_invitation_state_and_pending_tracking() {
        Group group = groupWithPendingInvitation("g1", "user-1");
        GroupInvitationId invitationId = group.invitations().getFirst().id();
        repository.save(group);

        Group reloaded = repository.findByInvitationId(invitationId).orElseThrow();
        reloaded.revokeInvitation(invitationId, UserId.of("admin"));
        repository.save(reloaded);

        Group afterRestart = repository.findByInvitationId(invitationId).orElseThrow();

        assertEquals(InvitationStatus.REVOKED, invitationStatus(afterRestart, invitationId));
        assertFalse(afterRestart.hasPendingInvitationFor(UserId.of("user-1")));
        assertEquals(1, afterRestart.members().size());
    }

    @Test
    void should_return_empty_for_unknown_group_or_invitation_id() {
        assertTrue(repository.findById(GroupId.of("missing")).isEmpty());
        assertTrue(repository.findByInvitationId(GroupInvitationId.of("missing")).isEmpty());
    }

    @Test
    void should_reject_invalid_persisted_group_state() throws Exception {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement insert = connection.prepareStatement(
                     "INSERT INTO organization_groups (id, name, description) VALUES (?, ?, ?)"
             )) {
            insert.setString(1, "invalid");
            insert.setString(2, "Invalid");
            insert.setString(3, null);
            insert.executeUpdate();
        }

        assertThrows(IllegalStateException.class, () -> repository.findById(GroupId.of("invalid")));
    }

    private static Group groupWithPendingInvitation(String groupId, String inviteeId) {
        Group group = Group.create(
                GroupId.of(groupId),
                GroupName.of("team"),
                null,
                UserId.of("admin")
        );
        group.inviteMember(
                UserId.of("admin"),
                UserId.of(inviteeId),
                GroupRole.MEMBER
        );
        return group;
    }

    private static InvitationStatus invitationStatus(Group group, GroupInvitationId invitationId) {
        return group.invitations().stream()
                .filter(invitation -> invitation.id().equals(invitationId))
                .map(GroupInvitation::status)
                .findFirst()
                .orElseThrow();
    }

    private static DataSource newDataSource() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:orca;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword("sa");
        return dataSource;
    }

    private static void migrate(DataSource dataSource) {
        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .cleanDisabled(false)
                .load()
                .clean();

        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .cleanDisabled(false)
                .load()
                .migrate();
    }
}
