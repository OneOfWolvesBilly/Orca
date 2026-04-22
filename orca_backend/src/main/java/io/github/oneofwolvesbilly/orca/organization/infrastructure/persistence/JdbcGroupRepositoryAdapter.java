package io.github.oneofwolvesbilly.orca.organization.infrastructure.persistence;

import io.github.oneofwolvesbilly.orca.organization.application.GroupRepository;
import io.github.oneofwolvesbilly.orca.organization.domain.Group;
import io.github.oneofwolvesbilly.orca.organization.domain.GroupId;
import io.github.oneofwolvesbilly.orca.organization.domain.GroupInvitationId;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public final class JdbcGroupRepositoryAdapter implements GroupRepository {

    private final DataSource dataSource;
    private final GroupEntityMapper mapper;

    public JdbcGroupRepositoryAdapter(DataSource dataSource, GroupEntityMapper mapper) {
        this.dataSource = dataSource;
        this.mapper = mapper;
    }

    @Override
    public Optional<Group> findById(GroupId groupId) {
        try (Connection connection = dataSource.getConnection()) {
            Optional<GroupEntityMapper.GroupRecord> group = findGroupRecord(connection, groupId);
            if (group.isEmpty()) {
                return Optional.empty();
            }

            return Optional.of(mapper.toDomain(
                    group.get(),
                    findMemberRecords(connection, groupId),
                    findInvitationRecords(connection, groupId)
            ));
        } catch (SQLException ex) {
            throw new IllegalStateException("failed to load group: " + groupId.value(), ex);
        }
    }

    @Override
    public Optional<Group> findByInvitationId(GroupInvitationId invitationId) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT group_id FROM invitation_index WHERE invitation_id = ?"
             )) {
            statement.setString(1, invitationId.value());

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return findById(GroupId.of(resultSet.getString("group_id")));
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("failed to load group by invitation: " + invitationId.value(), ex);
        }
    }

    @Override
    public void indexInvitation(GroupInvitationId invitationId, GroupId groupId) {
        try (Connection connection = dataSource.getConnection()) {
            upsertInvitationIndex(connection, invitationId, groupId);
        } catch (SQLException ex) {
            throw new IllegalStateException("failed to index invitation: " + invitationId.value(), ex);
        }
    }

    @Override
    public void save(Group group) {
        try (Connection connection = dataSource.getConnection()) {
            boolean originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                GroupEntityMapper.GroupRecord groupRecord = mapper.toGroupRecord(group);
                upsertGroup(connection, groupRecord);
                replaceMembers(connection, group);
                replaceInvitations(connection, group);
                connection.commit();
            } catch (RuntimeException | SQLException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(originalAutoCommit);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("failed to save group: " + group.id().value(), ex);
        }
    }

    private Optional<GroupEntityMapper.GroupRecord> findGroupRecord(Connection connection, GroupId groupId)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id, name, description FROM organization_groups WHERE id = ?"
        )) {
            statement.setString(1, groupId.value());

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(new GroupEntityMapper.GroupRecord(
                        resultSet.getString("id"),
                        resultSet.getString("name"),
                        resultSet.getString("description")
                ));
            }
        }
    }

    private java.util.List<GroupEntityMapper.MemberRecord> findMemberRecords(Connection connection, GroupId groupId)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT group_id, user_id, role FROM group_members WHERE group_id = ? ORDER BY user_id"
        )) {
            statement.setString(1, groupId.value());

            try (ResultSet resultSet = statement.executeQuery()) {
                java.util.ArrayList<GroupEntityMapper.MemberRecord> members = new java.util.ArrayList<>();
                while (resultSet.next()) {
                    members.add(new GroupEntityMapper.MemberRecord(
                            resultSet.getString("group_id"),
                            resultSet.getString("user_id"),
                            resultSet.getString("role")
                    ));
                }
                return members;
            }
        }
    }

    private java.util.List<GroupEntityMapper.InvitationRecord> findInvitationRecords(Connection connection, GroupId groupId)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                """
                SELECT id, group_id, invitee_id, intended_role, status
                FROM group_invitations
                WHERE group_id = ?
                ORDER BY id
                """
        )) {
            statement.setString(1, groupId.value());

            try (ResultSet resultSet = statement.executeQuery()) {
                java.util.ArrayList<GroupEntityMapper.InvitationRecord> invitations = new java.util.ArrayList<>();
                while (resultSet.next()) {
                    invitations.add(new GroupEntityMapper.InvitationRecord(
                            resultSet.getString("id"),
                            resultSet.getString("group_id"),
                            resultSet.getString("invitee_id"),
                            resultSet.getString("intended_role"),
                            resultSet.getString("status")
                    ));
                }
                return invitations;
            }
        }
    }

    private void upsertGroup(Connection connection, GroupEntityMapper.GroupRecord group) throws SQLException {
        try (PreparedStatement update = connection.prepareStatement(
                "UPDATE organization_groups SET name = ?, description = ? WHERE id = ?"
        )) {
            update.setString(1, group.name());
            update.setString(2, group.description());
            update.setString(3, group.id());
            if (update.executeUpdate() > 0) {
                return;
            }
        }

        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO organization_groups (id, name, description) VALUES (?, ?, ?)"
        )) {
            insert.setString(1, group.id());
            insert.setString(2, group.name());
            insert.setString(3, group.description());
            insert.executeUpdate();
        }
    }

    private void replaceMembers(Connection connection, Group group) throws SQLException {
        try (PreparedStatement delete = connection.prepareStatement(
                "DELETE FROM group_members WHERE group_id = ?"
        )) {
            delete.setString(1, group.id().value());
            delete.executeUpdate();
        }

        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO group_members (group_id, user_id, role) VALUES (?, ?, ?)"
        )) {
            for (GroupEntityMapper.MemberRecord member : mapper.toMemberRecords(group)) {
                insert.setString(1, member.groupId());
                insert.setString(2, member.userId());
                insert.setString(3, member.role());
                insert.addBatch();
            }
            insert.executeBatch();
        }
    }

    private void replaceInvitations(Connection connection, Group group) throws SQLException {
        try (PreparedStatement deleteIndex = connection.prepareStatement(
                "DELETE FROM invitation_index WHERE group_id = ?"
        )) {
            deleteIndex.setString(1, group.id().value());
            deleteIndex.executeUpdate();
        }

        try (PreparedStatement deleteInvitations = connection.prepareStatement(
                "DELETE FROM group_invitations WHERE group_id = ?"
        )) {
            deleteInvitations.setString(1, group.id().value());
            deleteInvitations.executeUpdate();
        }

        try (PreparedStatement insertInvitation = connection.prepareStatement(
                """
                INSERT INTO group_invitations (id, group_id, invitee_id, intended_role, status)
                VALUES (?, ?, ?, ?, ?)
                """
        );
             PreparedStatement insertIndex = connection.prepareStatement(
                     "INSERT INTO invitation_index (invitation_id, group_id) VALUES (?, ?)"
             )) {
            for (GroupEntityMapper.InvitationRecord invitation : mapper.toInvitationRecords(group)) {
                insertInvitation.setString(1, invitation.id());
                insertInvitation.setString(2, invitation.groupId());
                insertInvitation.setString(3, invitation.inviteeId());
                insertInvitation.setString(4, invitation.intendedRole());
                insertInvitation.setString(5, invitation.status());
                insertInvitation.addBatch();

                insertIndex.setString(1, invitation.id());
                insertIndex.setString(2, invitation.groupId());
                insertIndex.addBatch();
            }
            insertInvitation.executeBatch();
            insertIndex.executeBatch();
        }
    }

    private void upsertInvitationIndex(Connection connection, GroupInvitationId invitationId, GroupId groupId)
            throws SQLException {
        try (PreparedStatement update = connection.prepareStatement(
                "UPDATE invitation_index SET group_id = ? WHERE invitation_id = ?"
        )) {
            update.setString(1, groupId.value());
            update.setString(2, invitationId.value());
            if (update.executeUpdate() > 0) {
                return;
            }
        }

        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO invitation_index (invitation_id, group_id) VALUES (?, ?)"
        )) {
            insert.setString(1, invitationId.value());
            insert.setString(2, groupId.value());
            insert.executeUpdate();
        }
    }
}
