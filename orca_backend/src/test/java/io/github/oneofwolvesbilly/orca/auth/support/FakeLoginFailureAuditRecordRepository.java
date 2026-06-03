package io.github.oneofwolvesbilly.orca.auth.support;

import io.github.oneofwolvesbilly.orca.auth.application.LoginFailureAuditRecordRepository;
import io.github.oneofwolvesbilly.orca.auth.domain.LoginFailureAuditRecord;

import java.util.ArrayList;
import java.util.List;

public final class FakeLoginFailureAuditRecordRepository implements LoginFailureAuditRecordRepository {

    private final List<LoginFailureAuditRecord> savedRecords = new ArrayList<>();

    @Override
    public void save(LoginFailureAuditRecord record) {
        savedRecords.add(record);
    }

    public LoginFailureAuditRecord savedRecord() {
        return savedRecords.getFirst();
    }

    public List<LoginFailureAuditRecord> savedRecords() {
        return List.copyOf(savedRecords);
    }
}
