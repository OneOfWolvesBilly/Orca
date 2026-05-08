package io.github.oneofwolvesbilly.orca.auth.infrastructure.persistence;

import io.github.oneofwolvesbilly.orca.auth.application.AuthSystemRoleDirectory;
import io.github.oneofwolvesbilly.orca.auth.domain.AuthSystemRole;
import io.github.oneofwolvesbilly.orca.auth.domain.AuthenticatedUserId;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.Objects;

public final class JdbcAuthSystemRoleDirectory implements AuthSystemRoleDirectory {

    private final JdbcTemplate jdbcTemplate;

    public JdbcAuthSystemRoleDirectory(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(Objects.requireNonNull(dataSource, "dataSource"));
    }

    @Override
    public boolean hasRole(AuthenticatedUserId authenticatedUserId, AuthSystemRole role) {
        Objects.requireNonNull(authenticatedUserId, "authenticatedUserId");
        Objects.requireNonNull(role, "role");

        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM auth_system_role_assignments
                WHERE user_id = ? AND role = ?
                """,
                Integer.class,
                authenticatedUserId.value(),
                role.name()
        );
        return count != null && count > 0;
    }
}
