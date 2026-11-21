package com.attendance.tracker.controller;

import com.attendance.tracker.model.Subject;
import com.attendance.tracker.repository.SubjectRepository;
import com.attendance.tracker.repository.UserRepository;
import com.attendance.tracker.service.AttendanceService;
import com.attendance.tracker.service.SheetService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDate;

/**
 * AT-75: the teacher downloads the attendance sheet of one of their subjects as an Excel file.
 * Only TEACHER users can open /teacher/** (see SecurityConfig), and a teacher can
 * only download a subject the admin assigned to them.
 */
@Controller
public class TeacherSheetController {

    private static final MediaType XLSX =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final AttendanceService attendanceService;
    private final SheetService sheetService;
    private final SubjectRepository subjects;
    private final UserRepository users;

    public TeacherSheetController(AttendanceService attendanceService, SheetService sheetService,
                                  SubjectRepository subjects, UserRepository users) {
        this.attendanceService = attendanceService;
        this.sheetService = sheetService;
        this.subjects = subjects;
        this.users = users;
    }

    /** The page where the teacher chooses the subject and the dates. */
    @GetMapping("/teacher/download")
    public String page(Principal principal, Model model) {
        Long teacherId = users.findByUsername(principal.getName()).get().getId();
        model.addAttribute("subjects", subjects.findByTeacherIdOrderByClassRoomNameAscNameAsc(teacherId));
        return "teacher/download";
    }

    /** The download itself. "attachment" makes the browser save the file instead of showing it. */
    @GetMapping("/teacher/download/sheet.xlsx")
    public Object download(@RequestParam(required = false) Long subjectId,
                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                           Principal principal, RedirectAttributes flash) {
        try {
            Subject subject = attendanceService.findSubjectOfTeacher(principal.getName(), subjectId);
            if (from != null && to != null && from.isAfter(to)) {
                throw new IllegalArgumentException("The 'from' date cannot be after the 'to' date.");
            }
            byte[] file = sheetService.classSheet(subject, from, to);
            String fileName = safe(subject.getClassRoom().getName()) + "-" + safe(subject.getName())
                    + "-" + LocalDate.now() + ".xlsx";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .contentType(XLSX)
                    .body(file);
        } catch (IllegalArgumentException e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/teacher/download";
        }
    }

    /** Keeps only letters, digits, "-" and "_", so a class name can never break the file name. */
    private String safe(String text) {
        return text.replaceAll("[^A-Za-z0-9_-]+", "_");
    }
}
