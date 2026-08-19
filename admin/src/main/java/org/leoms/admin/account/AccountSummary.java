package org.leoms.admin.account;

import java.time.LocalDateTime;

public record AccountSummary(int id, String name, boolean banned, String banReason,
                             LocalDateTime createdAt, LocalDateTime lastLogin) {}
