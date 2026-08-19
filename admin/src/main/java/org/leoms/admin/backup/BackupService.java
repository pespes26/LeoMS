package org.leoms.admin.backup;

import org.leoms.admin.audit.AuditService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class BackupService {
    private final JdbcTemplate jdbc;
    private final AuditService audit;

    public BackupService(JdbcTemplate jdbc, AuditService audit) {
        this.jdbc = jdbc;
        this.audit = audit;
    }

    public List<BackupJob> recent() {
        return jdbc.query("""
                SELECT id, requested_by, requested_at, started_at, finished_at, state, snapshot_id, message
                FROM leoms_backup_jobs ORDER BY requested_at DESC LIMIT 25
                """, (rs, row) -> new BackupJob(rs.getLong("id"), rs.getString("requested_by"),
                time(rs.getTimestamp("requested_at")), time(rs.getTimestamp("started_at")),
                time(rs.getTimestamp("finished_at")), rs.getString("state"),
                rs.getString("snapshot_id"), rs.getString("message")));
    }

    @Transactional
    public void request(String actor) {
        Integer locked = jdbc.queryForObject("SELECT GET_LOCK('leoms_backup_request', 2)", Integer.class);
        if (locked == null || locked != 1) throw new IllegalStateException("Backup queue is busy");
        try {
            Integer active = jdbc.queryForObject("""
                    SELECT COUNT(*) FROM leoms_backup_jobs WHERE state IN ('REQUESTED','RUNNING')
                    """, Integer.class);
            if (active != null && active > 0) {
                audit.record(actor, "BACKUP_REQUEST", "backup", "queue", "FAILURE", "backup already active");
                throw new IllegalStateException("A backup is already requested or running");
            }
            jdbc.update("INSERT INTO leoms_backup_jobs (requested_by) VALUES (?)", actor);
            audit.record(actor, "BACKUP_REQUEST", "backup", "queue", "SUCCESS", "manual backup queued");
        } finally {
            jdbc.queryForObject("SELECT RELEASE_LOCK('leoms_backup_request')", Integer.class);
        }
    }

    private static LocalDateTime time(Timestamp value) {
        return value == null ? null : value.toLocalDateTime();
    }
}
