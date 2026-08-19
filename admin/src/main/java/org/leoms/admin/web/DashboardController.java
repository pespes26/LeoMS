package org.leoms.admin.web;

import org.leoms.admin.backup.BackupService;
import org.leoms.admin.ops.RecentLogService;
import org.leoms.admin.ops.StatusService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class DashboardController {
    private final StatusService status;
    private final RecentLogService logs;
    private final BackupService backups;

    public DashboardController(StatusService status, RecentLogService logs, BackupService backups) {
        this.status = status;
        this.logs = logs;
        this.backups = backups;
    }

    @GetMapping("/")
    String dashboard(@RequestParam(defaultValue = "cosmic-log.log") String log, Model model) {
        model.addAttribute("status", status.status());
        model.addAttribute("logNames", logs.allowedLogs());
        model.addAttribute("selectedLog", log);
        model.addAttribute("logText", logs.tail(log));
        model.addAttribute("backups", backups.recent());
        return "dashboard";
    }
}
