package com.attendance.tracker.controller;

import com.attendance.tracker.repository.UserRepository;
import com.attendance.tracker.service.ReportService;
import com.attendance.tracker.service.SubjectOverview;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

/**
 * AT-70: the teacher's home page. Shows the teacher's subjects and whether
 * today's attendance has been taken for each of them.
 * Only TEACHER users can open /teacher/** (see SecurityConfig).
 */
@Controller
public class TeacherDashboardController {

    private final UserRepository users;
    private final ReportService reports;

    public TeacherDashboardController(UserRepository users, ReportService reports) {
        this.users = users;
        this.reports = reports;
    }

    @GetMapping("/teacher/home")
    public String home(Principal principal, Model model) {
        Long teacherId = users.findByUsername(principal.getName()).get().getId();
        LocalDate today = LocalDate.now();
        List<SubjectOverview> rows = reports.overviewForTeacher(teacherId, today);

        int takenToday = 0;
        for (SubjectOverview row : rows) {
            if (row.isTakenToday()) {
                takenToday++;
            }
        }
        model.addAttribute("today", today);
        model.addAttribute("rows", rows);
        model.addAttribute("takenToday", takenToday);
        model.addAttribute("pendingToday", rows.size() - takenToday);
        return "teacher/home";
    }
}
