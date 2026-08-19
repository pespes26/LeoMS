package org.leoms.admin.account;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.leoms.admin.audit.AuditService;
import org.leoms.admin.backup.BackupService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers(disabledWithoutDocker = true)
class AccountServiceMySqlTest {
    @Container
    static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4.0");

    private JdbcTemplate jdbc;
    private AccountService accounts;
    private BackupService backups;

    @BeforeEach
    void setup() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword());
        jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("DROP TABLE IF EXISTS characters, leoms_backup_jobs, leoms_admin_audit, accounts");
        jdbc.execute("CREATE TABLE accounts (id INT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(13) UNIQUE NOT NULL, password VARCHAR(128) NOT NULL, pin VARCHAR(10) NOT NULL, pic VARCHAR(26) NOT NULL, tos TINYINT NOT NULL, banned TINYINT NOT NULL DEFAULT 0, banreason TEXT, createdat TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, lastlogin TIMESTAMP NULL)");
        jdbc.execute("CREATE TABLE characters (id INT AUTO_INCREMENT PRIMARY KEY, accountid INT NOT NULL, name VARCHAR(13), level INT, job INT, world INT, gm TINYINT)");
        jdbc.execute("CREATE TABLE leoms_admin_audit (id BIGINT AUTO_INCREMENT PRIMARY KEY, actor VARCHAR(64), action VARCHAR(64), target_type VARCHAR(32), target_id VARCHAR(64), occurred_at TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6), outcome VARCHAR(16), detail VARCHAR(255))");
        jdbc.execute("CREATE TABLE leoms_backup_jobs (id BIGINT AUTO_INCREMENT PRIMARY KEY, requested_by VARCHAR(64), requested_at TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6), started_at TIMESTAMP(6), finished_at TIMESTAMP(6), state VARCHAR(16) DEFAULT 'REQUESTED', snapshot_id VARCHAR(128), message VARCHAR(255))");
        AuditService audit = new AuditService(jdbc);
        accounts = new AccountService(jdbc, new BCryptPasswordEncoder(12), audit);
        backups = new BackupService(jdbc, audit);
    }

    @Test
    void createsResetsAndBansWithAudits() {
        CreateAccountForm create = createForm("Player10");
        int id = accounts.create(create, "owner");
        String hash = jdbc.queryForObject("SELECT password FROM accounts WHERE id=?", String.class, id);
        assertThat(new BCryptPasswordEncoder().matches("correct horse battery", hash)).isTrue();

        ResetCredentialsForm reset = new ResetCredentialsForm();
        reset.setPassword("another secure password"); reset.setPin("9876"); reset.setPic("654321");
        accounts.resetCredentials(id, reset, "owner");
        accounts.ban(id, "friend requested lock", "owner");
        accounts.unban(id, "owner");

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM leoms_admin_audit", Integer.class)).isEqualTo(4);
        assertThat(new BCryptPasswordEncoder().matches("another secure password",
                jdbc.queryForObject("SELECT password FROM accounts WHERE id=?", String.class, id))).isTrue();
        assertThat(jdbc.queryForObject("SELECT GROUP_CONCAT(detail) FROM leoms_admin_audit", String.class))
                .doesNotContain("correct horse battery", "another secure password", "9876", "654321");
    }

    @Test
    void duplicateUsernameIsRejectedAndAudited() {
        accounts.create(createForm("Duplicate"), "owner");
        assertThatThrownBy(() -> accounts.create(createForm("Duplicate"), "owner"))
                .isInstanceOf(DuplicateKeyException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM leoms_admin_audit WHERE outcome='FAILURE'", Integer.class)).isEqualTo(1);
    }

    @Test
    void queuesOnlyOneBackupAndAuditsBothOutcomes() {
        backups.request("owner");
        assertThat(jdbc.queryForObject("SELECT state FROM leoms_backup_jobs", String.class))
                .isEqualTo("REQUESTED");
        assertThatThrownBy(() -> backups.request("owner"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already requested");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM leoms_admin_audit WHERE action='BACKUP_REQUEST' AND outcome='SUCCESS'", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM leoms_admin_audit WHERE action='BACKUP_REQUEST' AND outcome='FAILURE'", Integer.class)).isEqualTo(1);
    }

    private CreateAccountForm createForm(String username) {
        CreateAccountForm form = new CreateAccountForm();
        form.setUsername(username); form.setPassword("correct horse battery");
        form.setPin("1234"); form.setPic("123456");
        return form;
    }
}
