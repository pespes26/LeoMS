package org.leoms.admin.account;

import org.leoms.admin.audit.AuditService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class AccountService {
    private final JdbcTemplate jdbc;
    private final PasswordEncoder encoder;
    private final AuditService audit;

    public AccountService(JdbcTemplate jdbc, PasswordEncoder encoder, AuditService audit) {
        this.jdbc = jdbc;
        this.encoder = encoder;
        this.audit = audit;
    }

    @Transactional
    public int create(CreateAccountForm form, String actor) {
        KeyHolder keys = new GeneratedKeyHolder();
        try {
            jdbc.update(connection -> {
                PreparedStatement ps = connection.prepareStatement("""
                        INSERT INTO accounts (`name`, password, pin, pic, tos)
                        VALUES (?, ?, ?, ?, 1)
                        """, Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, form.getUsername());
                ps.setString(2, encoder.encode(form.getPassword()));
                ps.setString(3, form.getPin());
                ps.setString(4, form.getPic());
                return ps;
            }, keys);
        } catch (DuplicateKeyException e) {
            audit.record(actor, "ACCOUNT_CREATE", "account", form.getUsername(), "FAILURE", "duplicate username");
            throw e;
        }
        int id = keys.getKey().intValue();
        audit.record(actor, "ACCOUNT_CREATE", "account", Integer.toString(id), "SUCCESS", "account created");
        return id;
    }

    public List<AccountSummary> search(String query) {
        String needle = query == null ? "" : query.strip();
        if (needle.isEmpty()) return List.of();
        String like = "%" + needle + "%";
        return jdbc.query("""
                SELECT DISTINCT a.id, a.name, a.banned, a.banreason, a.createdat, a.lastlogin
                FROM accounts a
                LEFT JOIN characters c ON c.accountid = a.id
                WHERE a.name LIKE ? OR c.name LIKE ?
                ORDER BY a.name LIMIT 50
                """, (rs, row) -> new AccountSummary(rs.getInt("id"), rs.getString("name"),
                rs.getBoolean("banned"), rs.getString("banreason"),
                rs.getTimestamp("createdat").toLocalDateTime(), timestamp(rs.getTimestamp("lastlogin"))), like, like);
    }

    public Optional<AccountSummary> find(int id) {
        List<AccountSummary> rows = jdbc.query("""
                SELECT id, name, banned, banreason, createdat, lastlogin FROM accounts WHERE id=?
                """, (rs, row) -> new AccountSummary(rs.getInt("id"), rs.getString("name"),
                rs.getBoolean("banned"), rs.getString("banreason"),
                rs.getTimestamp("createdat").toLocalDateTime(), timestamp(rs.getTimestamp("lastlogin"))), id);
        return rows.stream().findFirst();
    }

    public List<CharacterSummary> characters(int accountId) {
        return jdbc.query("""
                SELECT id, name, level, job, world, gm FROM characters WHERE accountid=? ORDER BY name
                """, (rs, row) -> new CharacterSummary(rs.getInt("id"), rs.getString("name"),
                rs.getInt("level"), rs.getInt("job"), rs.getInt("world"), rs.getInt("gm")), accountId);
    }

    @Transactional
    public void resetCredentials(int id, ResetCredentialsForm form, String actor) {
        List<String> assignments = new ArrayList<>();
        List<Object> values = new ArrayList<>();
        List<String> changed = new ArrayList<>();
        if (!form.getPassword().isBlank()) {
            if (form.getPassword().length() < 12) throw new IllegalArgumentException("Password must be at least 12 characters");
            assignments.add("password=?"); values.add(encoder.encode(form.getPassword())); changed.add("password");
        }
        if (!form.getPin().isBlank()) { assignments.add("pin=?"); values.add(form.getPin()); changed.add("PIN"); }
        if (!form.getPic().isBlank()) { assignments.add("pic=?"); values.add(form.getPic()); changed.add("PIC"); }
        if (assignments.isEmpty()) throw new IllegalArgumentException("Provide at least one credential to reset");
        values.add(id);
        int updated = jdbc.update("UPDATE accounts SET " + String.join(",", assignments) + " WHERE id=?", values.toArray());
        if (updated != 1) throw new IllegalArgumentException("Account not found");
        audit.record(actor, "CREDENTIAL_RESET", "account", Integer.toString(id), "SUCCESS",
                "reset " + String.join(", ", changed));
    }

    @Transactional
    public void ban(int id, String reason, String actor) {
        int updated = jdbc.update("UPDATE accounts SET banned=1, banreason=? WHERE id=?", reason.strip(), id);
        if (updated != 1) throw new IllegalArgumentException("Account not found");
        audit.record(actor, "ACCOUNT_BAN", "account", Integer.toString(id), "SUCCESS", "reason recorded");
    }

    @Transactional
    public void unban(int id, String actor) {
        int updated = jdbc.update("UPDATE accounts SET banned=0, banreason=NULL WHERE id=?", id);
        if (updated != 1) throw new IllegalArgumentException("Account not found");
        audit.record(actor, "ACCOUNT_UNBAN", "account", Integer.toString(id), "SUCCESS", "ban removed");
    }

    private static java.time.LocalDateTime timestamp(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
