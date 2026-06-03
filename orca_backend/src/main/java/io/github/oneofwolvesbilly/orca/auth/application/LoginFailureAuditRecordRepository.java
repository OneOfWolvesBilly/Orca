package io.github.oneofwolvesbilly.orca.auth.application;

import io.github.oneofwolvesbilly.orca.auth.domain.LoginFailureAuditRecord;

public interface LoginFailureAuditRecordRepository {

    void save(LoginFailureAuditRecord record);
}
