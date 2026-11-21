package com.attendance.tracker.controller;

import com.attendance.tracker.model.AppUser;
import com.attendance.tracker.model.Role;
import com.attendance.tracker.repository.UserRepository;
import com.attendance.tracker.service.AccountService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * AT-59: a student or teacher forgot their password, so the admin sets a new temporary one.
 * (There is no e-mail in this app, so "forgot password" means "ask the admin".)
 * Only ADMIN users can open /admin/** (see SecurityConfig).
 */
@Controller
public class AdminPasswordController {

    private final UserRepository users;
    private final AccountService accountService;

    public AdminPasswordController(UserRepository users, AccountService accountService) {
        this.users = users;
        this.accountService = accountService;
    }

    @GetMapping("/admin/users/{id}/reset-password")
    public String form(@PathVariable Long id, Model model, RedirectAttributes flash) {
        AppUser user = users.findById(id).orElse(null);
        if (user == null || user.getRole() == Role.ADMIN) {
            flash.addFlashAttribute("error", "User not found.");
            return "redirect:/admin/home";
        }
        model.addAttribute("user", user);
        model.addAttribute("backUrl", backUrl(user));
        model.addAttribute("active", user.getRole() == Role.TEACHER ? "teachers" : "students");
        return "admin/reset-password";
    }

    @PostMapping("/admin/users/{id}/reset-password")
    public String reset(@PathVariable Long id, @RequestParam String newPassword,
                        RedirectAttributes flash) {
        try {
            AppUser user = accountService.resetPassword(id, newPassword);
            flash.addFlashAttribute("message", "New password set for " + user.getFullName() + " ('"
                    + user.getUsername() + "'). Tell them the new password.");
            return "redirect:" + backUrl(user);
        } catch (IllegalArgumentException e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/users/" + id + "/reset-password";
        }
    }

    /** The list page the admin came from. */
    private String backUrl(AppUser user) {
        return user.getRole() == Role.TEACHER ? "/admin/teachers" : "/admin/students";
    }
}
