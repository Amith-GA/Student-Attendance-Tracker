package com.attendance.tracker.controller;

import com.attendance.tracker.model.Role;
import com.attendance.tracker.repository.ClassRoomRepository;
import com.attendance.tracker.repository.StudentRepository;
import com.attendance.tracker.repository.SubjectRepository;
import com.attendance.tracker.repository.UserRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * AT-81: the admin's home page with a few numbers: how many students, teachers, classes and subjects.
 * Only ADMIN users can open /admin/** (see SecurityConfig).
 */
@Controller
public class AdminDashboardController {

    private final StudentRepository students;
    private final UserRepository users;
    private final ClassRoomRepository classes;
    private final SubjectRepository subjects;

    public AdminDashboardController(StudentRepository students, UserRepository users,
                                    ClassRoomRepository classes, SubjectRepository subjects) {
        this.students = students;
        this.users = users;
        this.classes = classes;
        this.subjects = subjects;
    }

    @GetMapping("/admin/home")
    public String home(Model model) {
        model.addAttribute("studentCount", students.count());
        model.addAttribute("deactivatedCount", students.countByUserActiveFalse());
        model.addAttribute("teacherCount", users.countByRole(Role.TEACHER));
        model.addAttribute("classCount", classes.count());
        model.addAttribute("subjectCount", subjects.count());
        return "admin/home";
    }
}
