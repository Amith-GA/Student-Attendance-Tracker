package com.attendance.tracker.controller;

import com.attendance.tracker.model.Subject;
import com.attendance.tracker.repository.SubjectRepository;
import com.attendance.tracker.repository.UserRepository;
import com.attendance.tracker.service.AttendanceService;
import com.attendance.tracker.service.ReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.time.LocalDate;

/**
 * AT-34: the teacher's class-wise report for one of their subjects.
 * Only TEACHER users can open /teacher/** (see SecurityConfig), and a teacher can
 * only open a report for a subject the admin assigned to them.
 */
@Controller
public class TeacherReportController {

    private final AttendanceService attendanceService;
    private final ReportService reports;
    private final SubjectRepository subjects;
    private final UserRepository users;

    public TeacherReportController(AttendanceService attendanceService, ReportService reports,
                                   SubjectRepository subjects, UserRepository users) {
        this.attendanceService = attendanceService;
        this.reports = reports;
        this.subjects = subjects;
        this.users = users;
    }

    /** One page: the subject drop-down on top, and the report below once a subject is chosen. */
    @GetMapping("/teacher/report")
    public String report(@RequestParam(required = false) Long subjectId,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                         Principal principal, Model model) {
        Long teacherId = users.findByUsername(principal.getName()).get().getId();
        model.addAttribute("subjects", subjects.findByTeacherIdOrderByClassRoomNameAscNameAsc(teacherId));
        // AT-46: the chosen dates are sent back to the page so the boxes stay filled in
        model.addAttribute("from", from);
        model.addAttribute("to", to);

        if (subjectId != null) {
            try {
                Subject subject = attendanceService.findSubjectOfTeacher(principal.getName(), subjectId);
                if (from != null && to != null && from.isAfter(to)) {
                    throw new IllegalArgumentException("The 'from' date cannot be after the 'to' date.");
                }
                model.addAttribute("subject", subject);
                model.addAttribute("rows", reports.classReport(subject, from, to));
                model.addAttribute("classesHeld", reports.classesHeld(subject, from, to));
            } catch (IllegalArgumentException e) {
                model.addAttribute("error", e.getMessage());
            }
        }
        return "teacher/report";
    }
}
