package com.attendance.tracker.controller;

import com.attendance.tracker.model.AttendanceRecord;
import com.attendance.tracker.model.AttendanceStatus;
import com.attendance.tracker.model.Subject;
import com.attendance.tracker.repository.AttendanceRepository;
import com.attendance.tracker.repository.StudentRepository;
import com.attendance.tracker.repository.SubjectRepository;
import com.attendance.tracker.repository.UserRepository;
import com.attendance.tracker.service.AttendanceService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AT-18: teacher marks attendance.
 * Step 1: choose subject (which also fixes the class) and date.
 * Step 2: a list of the class's students with Present / Absent / Late, then Save.
 * Only TEACHER users can open /teacher/** (see SecurityConfig).
 */
@Controller
public class TeacherAttendanceController {

    private final AttendanceService attendanceService;
    private final AttendanceRepository attendance;
    private final SubjectRepository subjects;
    private final StudentRepository students;
    private final UserRepository users;

    public TeacherAttendanceController(AttendanceService attendanceService, AttendanceRepository attendance,
                                       SubjectRepository subjects, StudentRepository students,
                                       UserRepository users) {
        this.attendanceService = attendanceService;
        this.attendance = attendance;
        this.subjects = subjects;
        this.students = students;
        this.users = users;
    }

    /** Step 1: pick a subject and a date. "principal" is the logged-in user. */
    @GetMapping("/teacher/attendance")
    public String chooseSubject(Principal principal, Model model) {
        Long teacherId = users.findByUsername(principal.getName()).get().getId();
        model.addAttribute("subjects", subjects.findByTeacherIdOrderByClassRoomNameAscNameAsc(teacherId));
        model.addAttribute("today", LocalDate.now());
        return "teacher/attendance";
    }

    /** Step 2: show the students of the subject's class. */
    @GetMapping("/teacher/attendance/mark")
    public String markPage(@RequestParam(required = false) Long subjectId,
                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                           Principal principal, Model model, RedirectAttributes flash) {
        try {
            Subject subject = attendanceService.findSubjectOfTeacher(principal.getName(), subjectId);
            attendanceService.checkDate(date);

            model.addAttribute("subject", subject);
            model.addAttribute("date", date);
            model.addAttribute("students",
                    students.findByClassRoomIdAndUserActiveTrueOrderByRollNoAsc(subject.getClassRoom().getId()));
            // If attendance was already saved for this subject and date, show it instead of the form
            List<AttendanceRecord> saved =
                    attendance.findBySubjectIdAndAttendanceDateOrderByStudentRollNoAsc(subjectId, date);
            model.addAttribute("saved", saved);
            // AT-23: saved attendance can be edited only if it was created today
            model.addAttribute("editable", attendanceService.isEditable(saved));
            return "teacher/attendance-mark";
        } catch (IllegalArgumentException e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/teacher/attendance";
        }
    }

    /**
     * Save the form. The radio buttons are named status_<studentId>, for example status_5=ABSENT,
     * so we read all parameters and pick out the ones that start with "status_".
     */
    @PostMapping("/teacher/attendance/save")
    public String save(@RequestParam Long subjectId,
                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                       @RequestParam Map<String, String> params,
                       Principal principal, RedirectAttributes flash) {
        try {
            Map<Long, AttendanceStatus> statuses = readStatuses(params);
            int count = attendanceService.markAttendance(principal.getName(), subjectId, date, statuses);
            flash.addFlashAttribute("message", "Attendance saved for " + count + " students on " + date + ".");
            return "redirect:/teacher/attendance";
        } catch (IllegalArgumentException e) {
            flash.addFlashAttribute("error", e.getMessage());
        } catch (DataIntegrityViolationException e) {
            // the database unique rule stopped a duplicate (for example a double click on Save)
            flash.addFlashAttribute("error", "Attendance for this subject and date is already saved.");
        }
        return "redirect:/teacher/attendance/mark?subjectId=" + subjectId + "&date=" + date;
    }

    /** AT-23: save changes to attendance that was saved earlier today. */
    @PostMapping("/teacher/attendance/update")
    public String update(@RequestParam Long subjectId,
                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                         @RequestParam Map<String, String> params,
                         Principal principal, RedirectAttributes flash) {
        try {
            Map<Long, AttendanceStatus> statuses = readStatuses(params);
            int count = attendanceService.updateAttendance(principal.getName(), subjectId, date, statuses);
            flash.addFlashAttribute("message", "Attendance updated for " + count + " students on " + date + ".");
            return "redirect:/teacher/attendance";
        } catch (IllegalArgumentException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/teacher/attendance/mark?subjectId=" + subjectId + "&date=" + date;
    }

    /** Turns "status_5=ABSENT" style parameters into a map: student id 5 -> ABSENT. */
    private Map<Long, AttendanceStatus> readStatuses(Map<String, String> params) {
        Map<Long, AttendanceStatus> statuses = new HashMap<>();
        try {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (entry.getKey().startsWith("status_")) {
                    Long studentId = Long.valueOf(entry.getKey().substring("status_".length()));
                    statuses.put(studentId, AttendanceStatus.valueOf(entry.getValue()));
                }
            }
        } catch (IllegalArgumentException e) {
            // only happens if someone edited the form by hand (bad student id or status text)
            throw new IllegalArgumentException("The form data was not valid. Please try again.");
        }
        return statuses;
    }
}
