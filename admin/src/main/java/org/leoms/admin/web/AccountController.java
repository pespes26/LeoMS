package org.leoms.admin.web;

import jakarta.validation.Valid;
import org.leoms.admin.account.*;
import org.leoms.admin.audit.AuditService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/accounts")
public class AccountController {
    private final AccountService accounts;
    private final AuditService audit;

    public AccountController(AccountService accounts, AuditService audit) {
        this.accounts = accounts;
        this.audit = audit;
    }

    @GetMapping
    String search(@RequestParam(defaultValue = "") String q, Model model) {
        model.addAttribute("query", q);
        model.addAttribute("accounts", accounts.search(q));
        return "accounts/search";
    }

    @GetMapping("/new")
    String createForm(Model model) {
        if (!model.containsAttribute("form")) model.addAttribute("form", new CreateAccountForm());
        return "accounts/new";
    }

    @PostMapping
    String create(@Valid @ModelAttribute("form") CreateAccountForm form, BindingResult errors,
                  Authentication auth, RedirectAttributes redirect) {
        if (errors.hasErrors()) return "accounts/new";
        try {
            int id = accounts.create(form, auth.getName());
            redirect.addFlashAttribute("message", "Account created.");
            return "redirect:/accounts/" + id;
        } catch (DuplicateKeyException e) {
            errors.rejectValue("username", "duplicate", "That username already exists");
            return "accounts/new";
        }
    }

    @GetMapping("/{id}")
    String detail(@PathVariable int id, Model model) {
        model.addAttribute("account", accounts.find(id).orElseThrow());
        model.addAttribute("characters", accounts.characters(id));
        if (!model.containsAttribute("resetForm")) model.addAttribute("resetForm", new ResetCredentialsForm());
        if (!model.containsAttribute("banForm")) model.addAttribute("banForm", new BanForm());
        return "accounts/detail";
    }

    @PostMapping("/{id}/credentials")
    String reset(@PathVariable int id, @Valid @ModelAttribute("resetForm") ResetCredentialsForm form,
                 BindingResult errors, Authentication auth, RedirectAttributes redirect, Model model) {
        if (errors.hasErrors()) {
            model.addAttribute("account", accounts.find(id).orElseThrow());
            model.addAttribute("characters", accounts.characters(id));
            model.addAttribute("banForm", new BanForm());
            return "accounts/detail";
        }
        try {
            accounts.resetCredentials(id, form, auth.getName());
            redirect.addFlashAttribute("message", "Credentials reset.");
        } catch (RuntimeException e) {
            audit.record(auth.getName(), "CREDENTIAL_RESET", "account", Integer.toString(id), "FAILURE", "request rejected");
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/accounts/" + id;
    }

    @PostMapping("/{id}/ban")
    String ban(@PathVariable int id, @Valid @ModelAttribute BanForm form, BindingResult errors,
               Authentication auth, RedirectAttributes redirect) {
        if (errors.hasErrors()) {
            redirect.addFlashAttribute("error", "A ban reason is required (255 characters maximum).");
        } else {
            try {
                accounts.ban(id, form.getReason(), auth.getName());
                redirect.addFlashAttribute("message", "Account banned.");
            } catch (RuntimeException e) {
                audit.record(auth.getName(), "ACCOUNT_BAN", "account", Integer.toString(id),
                        "FAILURE", "request rejected");
                redirect.addFlashAttribute("error", e.getMessage());
            }
        }
        return "redirect:/accounts/" + id;
    }

    @PostMapping("/{id}/unban")
    String unban(@PathVariable int id, Authentication auth, RedirectAttributes redirect) {
        try {
            accounts.unban(id, auth.getName());
            redirect.addFlashAttribute("message", "Account unbanned.");
        } catch (RuntimeException e) {
            audit.record(auth.getName(), "ACCOUNT_UNBAN", "account", Integer.toString(id),
                    "FAILURE", "request rejected");
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/accounts/" + id;
    }
}
