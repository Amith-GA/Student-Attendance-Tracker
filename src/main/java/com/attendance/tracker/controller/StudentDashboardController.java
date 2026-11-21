package com.attendance.tracker.controller;

import com.attendance.tracker.model.AppUser;
import com.attendance.tracker.model.Student;
import com.attendance.tracker.repository.StudentRepository;
import com.attendance.tracker.repository.SubjectRepository;
import com.attendance.tracker.repository.UserRepository;
import com.attendance.tracker.service.AttendanceMath;
import com.attendance.tracker.service.ReportService;
import com.attendance.tracker.service.SubjectAttendance;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * The student's own pages:
 *  - /student/home        dashboard with the overall picture (AT-30, AT-38, AT-64)
 *  - /student/attendance  detailed table with filters and the day-by-day history (AT-64)
 * Only STUDENT users can open /student/** (see SecurityConfig), and the pages
 * only ever show the data of the logged-in student.
 */
@Controller
public class StudentDashboardController {

    private final UserRepository users;
    private final StudentRepository students;
    private final SubjectRepository subjects;
    private final ReportService reports;

    public StudentDashboardController(UserRepository users, StudentRepository students,
                                      SubjectRepository subjects, ReportService reports) {
        this.users = users;
        this.students = students;
        this.subjects = subjects;
        this.reports = reports;
    }

    /** Dashboard: overall percentage, tiles, warning and a short list of all subjects. */
    @GetMapping("/student/home")
    public String home(Principal principal, Model model) {
        Student student = loadStudent(principal, model);
        if (student != null) {
            addSummary(model, reports.subjectWiseAttendance(student));
        }
        return "student/home";
    }

    /** Detail page: one row per subject plus the history, both with optional filters. */
    @GetMapping("/student/attendance")
    public String attendance(@RequestParam(required = false) Long subjectId,
                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                             Principal principal, Model model) {
        Student student = loadStudent(principal, model);
        if (student != null) {
            // the chosen filters are sent back so the boxes stay filled in
            model.addAttribute("classSubjects", subjects.findByClassRoomIdOrderByNameAsc(student.getClassRoom().getId()));
            model.addAttribute("subjectId", subjectId);
            model.addAttribute("from", from);
            model.addAttribute("to", to);
            try {
                addSummary(model, reports.subjectWiseAttendance(student, subjectId, from, to));
                model.addAttribute("history", reports.history(student, subjectId, from, to));
            } catch (IllegalArgumentException e) {
                model.addAttribute("error", e.getMessage());
            }
        }
        return "student/attendance";
    }

    /** Finds the logged-in student's profile (null if the admin has not enrolled the account yet). */
    private Student loadStudent(Principal principal, Model model) {
        AppUser user = users.findByUsername(principal.getName()).get();
        Student student = students.findByUserId(user.getId()).orElse(null);
        model.addAttribute("fullName", user.getFullName());
        model.addAttribute("student", student);
        return student;
    }

    /** Puts the subject rows and the overall numbers (all subjects together) into the page. */
    private void addSummary(Model model, List<SubjectAttendance> rows) {
        int present = 0;
        int late = 0;
        int absent = 0;
        List<SubjectAttendance> low = new ArrayList<>();
        for (SubjectAttendance row : rows) {
            present += row.getPresent();
            late += row.getLate();
            absent += row.getAbsent();
            if (row.isBelowLimit()) {
                low.add(row); // AT-38: subjects below 75%, shown in a warning
            }
        }
        int attended = present + late;
        int total = attended + absent;

        model.addAttribute("rows", rows);
        model.addAttribute("lowSubjects", low);
        model.addAttribute("minimumPercent", (int) SubjectAttendance.MINIMUM_PERCENT);
        model.addAttribute("overallPresent", present);
        model.addAttribute("overallLate", late);
        model.addAttribute("overallAbsent", absent);
        model.addAttribute("overallAttended", attended);
        model.addAttribute("overallTotal", total);
        model.addAttribute("overallPercent", AttendanceMath.percentage(attended, total));
        model.addAttribute("overallLevel", AttendanceMath.level(attended, total));
    }
}
