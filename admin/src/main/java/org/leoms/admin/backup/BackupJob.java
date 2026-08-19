package org.leoms.admin.backup;

import java.time.LocalDateTime;

public record BackupJob(long id, String requestedBy, LocalDateTime requestedAt,
                        LocalDateTime startedAt, LocalDateTime finishedAt, String state,
                        String snapshotId, String message) {}
