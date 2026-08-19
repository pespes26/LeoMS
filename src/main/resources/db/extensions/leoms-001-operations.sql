--liquibase formatted sql

--changeset leoms:leoms-001-admin-audit
CREATE TABLE leoms_admin_audit
(
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    actor       VARCHAR(64)  NOT NULL,
    action      VARCHAR(64)  NOT NULL,
    target_type VARCHAR(32)  NOT NULL,
    target_id   VARCHAR(64)  NOT NULL,
    occurred_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    outcome     VARCHAR(16)  NOT NULL,
    detail      VARCHAR(255) NULL,
    PRIMARY KEY (id),
    INDEX idx_leoms_audit_time (occurred_at),
    CONSTRAINT chk_leoms_audit_outcome CHECK (outcome IN ('SUCCESS', 'FAILURE'))
);

--changeset leoms:leoms-002-backup-jobs
CREATE TABLE leoms_backup_jobs
(
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    requested_by VARCHAR(64)  NOT NULL,
    requested_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    started_at   TIMESTAMP(6) NULL,
    finished_at  TIMESTAMP(6) NULL,
    state        VARCHAR(16)  NOT NULL DEFAULT 'REQUESTED',
    snapshot_id  VARCHAR(128) NULL,
    message      VARCHAR(255) NULL,
    PRIMARY KEY (id),
    INDEX idx_leoms_backup_state_time (state, requested_at),
    CONSTRAINT chk_leoms_backup_state CHECK (state IN ('REQUESTED', 'RUNNING', 'SUCCEEDED', 'FAILED'))
);
