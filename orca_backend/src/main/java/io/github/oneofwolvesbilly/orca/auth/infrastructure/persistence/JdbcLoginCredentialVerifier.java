package io.github.oneofwolvesbilly.orca.auth.infrastructure.persistence;

import io.github.oneofwolvesbilly.orca.auth.application.LoginCredentialVerifier;
import io.github.oneofwolvesbilly.orca.auth.application.LoginRejectedException;
import io.github.oneofwolvesbilly.orca.auth.domain.AuthenticatedUserId;
import io.github.oneofwolvesbilly.orca.auth.domain.LoginIdentifier;
import io.github.oneofwolvesbilly.orca.auth.domain.SubmittedPassword;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

public final class JdbcLoginCredentialVerifier implements LoginCredentialVerifier {

    private final JdbcTemplate jdbcTemplate;

    public JdbcLoginCredentialVerifier(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(Objects.requireNonNull(dataSource, "dataSource"));
    }

    @Override
    public AuthenticatedUserId verify(LoginIdentifier loginIdentifier, SubmittedPassword password) {
        Objects.requireNonNull(loginIdentifier, "loginIdentifier");
        Objects.requireNonNull(password, "password");

        try {
            return jdbcTemplate.queryForObject(
                    """
                    SELECT c.user_id
                    FROM auth_login_credentials c
                    INNER JOIN auth_registered_users u ON u.user_id = c.user_id
                    WHERE c.login_identifier = ?
                      AND c.password_hash = ?
                    """,
                    (rs, rowNum) -> AuthenticatedUserId.of(rs.getString("user_id")),
                    loginIdentifier.value(),
                    hashPasswordForStorage(password.value())
            );
        } catch (IncorrectResultSizeDataAccessException ex) {
            throw new LoginRejectedException();
        }
    }

    public static String hashPasswordForStorage(String password) {
        Objects.requireNonNull(password, "password");
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }
}
