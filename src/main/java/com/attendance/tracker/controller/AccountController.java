package com.attendance.tracker.controller;

import com.attendance.tracker.service.AccountService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

/**
 * AT-59: every logged-in user (admin, teacher or student) can change their own password.
 * /account/** has no role rule in SecurityConfig, so any logged-in user may open it.
 */
@Controller
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/account/password")
    public String form() {
        return "account/password";
    }

    @PostMapping("/account/password")
    public String change(@RequestParam String currentPassword,
                         @RequestParam String newPassword,
                         @RequestParam String confirmPassword,
                         Principal principal, RedirectAttributes flash) {
        try {
            accountService.changePassword(principal.getName(), currentPassword, newPassword, confirmPassword);
            flash.addFlashAttribute("message", "Your password was changed.");
        } catch (IllegalArgumentException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/account/password";
    }
}
