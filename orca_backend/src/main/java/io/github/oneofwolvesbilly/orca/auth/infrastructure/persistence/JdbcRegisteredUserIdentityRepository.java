package io.github.oneofwolvesbilly.orca.auth.infrastructure.persistence;

import io.github.oneofwolvesbilly.orca.auth.application.RegisteredUserIdentityRepository;
import io.github.oneofwolvesbilly.orca.auth.domain.AuthenticatedUserId;
import io.github.oneofwolvesbilly.orca.auth.domain.RegisteredUserIdentity;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.Objects;

public final class JdbcRegisteredUserIdentityRepository implements RegisteredUserIdentityRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcRegisteredUserIdentityRepository(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(Objects.requireNonNull(dataSource, "dataSource"));
    }

    @Override
    public void save(RegisteredUserIdentity identity) {
        Objects.requireNonNull(identity, "identity");
        jdbcTemplate.update(
                "INSERT INTO auth_registered_users (user_id) VALUES (?)",
                identity.authenticatedUserId().value()
        );
    }

    @Override
    public boolean exists(AuthenticatedUserId authenticatedUserId) {
        Objects.requireNonNull(authenticatedUserId, "authenticatedUserId");
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM auth_registered_users WHERE user_id = ?",
                Integer.class,
                authenticatedUserId.value()
        );
        return count != null && count > 0;
    }
}
