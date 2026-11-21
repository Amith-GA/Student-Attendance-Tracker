package com.attendance.tracker.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    // AT-59: explains that the admin resets forgotten passwords (public page, no login needed)
    @GetMapping("/forgot-password")
    public String forgotPassword() {
        return "forgot-password";
    }

    // "/" has no page of its own; send logged-in users to a role page via login success handler.
    @GetMapping("/")
    public String root() {
        return "redirect:/login";
    }

    // "/admin/home", "/teacher/home" and "/student/home" are handled by AdminDashboardController,
    // TeacherDashboardController and StudentDashboardController (the pages need data from the database)
}
