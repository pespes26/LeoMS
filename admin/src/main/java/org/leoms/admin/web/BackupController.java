package org.leoms.admin.web;

import org.leoms.admin.backup.BackupService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class BackupController {
    private final BackupService backups;

    public BackupController(BackupService backups) {
        this.backups = backups;
    }

    @PostMapping("/backups/request")
    String request(Authentication auth, RedirectAttributes redirect) {
        try {
            backups.request(auth.getName());
            redirect.addFlashAttribute("message", "Backup requested.");
        } catch (IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/";
    }
}
