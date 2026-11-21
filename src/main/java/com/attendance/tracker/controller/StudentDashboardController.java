package com.attendance.tracker.controller;

import com.attendance.tracker.model.AppUser;
import com.attendance.tracker.model.Student;
import com.attendance.tracker.repository.StudentRepository;
import com.attendance.tracker.repository.UserRepository;
import com.attendance.tracker.service.ReportService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;

/**
 * Student home page: the student's own subject-wise attendance (AT-30).
 * Only STUDENT users can open /student/** (see SecurityConfig), and the page
 * only ever shows the data of the logged-in student.
 */
@Controller
public class StudentDashboardController {

    private final UserRepository users;
    private final StudentRepository students;
    private final ReportService reports;

    public StudentDashboardController(UserRepository users, StudentRepository students,
                                      ReportService reports) {
        this.users = users;
        this.students = students;
        this.reports = reports;
    }

    @GetMapping("/student/home")
    public String home(Principal principal, Model model) {
        AppUser user = users.findByUsername(principal.getName()).get();
        Student student = students.findByUserId(user.getId()).orElse(null);

        model.addAttribute("fullName", user.getFullName());
        model.addAttribute("student", student); // null if the admin has not enrolled this account
        if (student != null) {
            model.addAttribute("rows", reports.subjectWiseAttendance(student));
        }
        return "student/home";
    }
}
