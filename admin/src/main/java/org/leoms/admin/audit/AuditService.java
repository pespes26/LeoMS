package org.leoms.admin.audit;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {
    private final JdbcTemplate jdbc;

    public AuditService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String actor, String action, String targetType, String targetId,
                       String outcome, String detail) {
        jdbc.update("""
                INSERT INTO leoms_admin_audit
                    (actor, action, target_type, target_id, outcome, detail)
                VALUES (?, ?, ?, ?, ?, ?)
                """, actor, action, targetType, targetId, outcome, sanitize(detail));
    }

    private String sanitize(String detail) {
        if (detail == null) return null;
        String clean = detail.replaceAll("[\\r\\n\\t]", " ");
        return clean.substring(0, Math.min(clean.length(), 255));
    }
}
