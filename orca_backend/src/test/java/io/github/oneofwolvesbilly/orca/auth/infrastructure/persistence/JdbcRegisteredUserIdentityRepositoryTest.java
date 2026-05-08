package io.github.oneofwolvesbilly.orca.auth.infrastructure.persistence;

import io.github.oneofwolvesbilly.orca.auth.application.RegisteredUserIdentityRepository;
import io.github.oneofwolvesbilly.orca.auth.domain.AuthenticatedUserId;
import io.github.oneofwolvesbilly.orca.auth.domain.RegisteredUserIdentity;
import org.flywaydb.core.Flyway;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcRegisteredUserIdentityRepositoryTest {

    private RegisteredUserIdentityRepository repository;

    @BeforeEach
    void setUp() {
        DataSource dataSource = newDataSource();
        migrate(dataSource);
        repository = new JdbcRegisteredUserIdentityRepository(dataSource);
    }

    @Test
    void save_persists_registered_user_identity() {
        repository.save(RegisteredUserIdentity.of(AuthenticatedUserId.of("user-1")));

        assertTrue(repository.exists(AuthenticatedUserId.of("user-1")));
    }

    @Test
    void exists_returns_false_for_unknown_user_identity() {
        assertFalse(repository.exists(AuthenticatedUserId.of("missing-user")));
    }

    private static DataSource newDataSource() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:orca_auth_identity;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1");
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
