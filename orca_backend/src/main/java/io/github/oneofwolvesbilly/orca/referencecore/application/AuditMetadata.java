package io.github.oneofwolvesbilly.orca.referencecore.application;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record AuditMetadata(List<AuditMetadataEntry> entries) {

    private static final AuditMetadata EMPTY = new AuditMetadata(List.of());

    public AuditMetadata {
        entries = List.copyOf(Objects.requireNonNull(entries, "entries"));

        Set<String> keys = new HashSet<>();
        for (AuditMetadataEntry entry : entries) {
            if (!keys.add(entry.key())) {
                throw new IllegalArgumentException("metadata keys must be unique");
            }
        }
    }

    public static AuditMetadata empty() {
        return EMPTY;
    }

    public static AuditMetadata of(Collection<AuditMetadataEntry> entries) {
        return new AuditMetadata(List.copyOf(Objects.requireNonNull(entries, "entries")));
    }
}
