package com.attendance.tracker.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/login")
    public String login() {
        return "login";
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

    @GetMapping("/student/home")
    public String studentHome() {
        return "student/home";
    }
}
