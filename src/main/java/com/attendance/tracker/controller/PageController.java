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

    @GetMapping("/admin/home")
    public String adminHome() {
        return "admin/home";
    }

    @GetMapping("/teacher/home")
    public String teacherHome() {
        return "teacher/home";
    }

    // "/student/home" is handled by StudentDashboardController (it needs the student's data)
}
