package com.attendance.tracker.controller;

import com.attendance.tracker.model.Subject;
import com.attendance.tracker.repository.ClassRoomRepository;
import com.attendance.tracker.repository.SubjectRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;

/**
 * AT-81: a read-only list of the subjects, opened from the "Subjects" tile on the admin dashboard.
 * A subject belongs to ONE class, so the same subject name appears once for every class that has it.
 * The list is sorted by subject name, so those rows stay together. Adding, editing and deleting
 * subjects is still done on the Classes & Subjects page.
 * Only ADMIN users can open /admin/** (see SecurityConfig).
 */
@Controller
public class AdminSubjectListController {

    private final SubjectRepository subjects;
    private final ClassRoomRepository classes;

    public AdminSubjectListController(SubjectRepository subjects, ClassRoomRepository classes) {
        this.subjects = subjects;
        this.classes = classes;
    }

    @GetMapping("/admin/subjects")
    public String list(@RequestParam(required = false) Long classId, Model model) {
        List<Subject> all = subjects.findAllByOrderByNameAscClassRoomNameAsc();
        List<Subject> shown = new ArrayList<>();
        for (Subject subject : all) {
            if (classId == null || subject.getClassRoom().getId().equals(classId)) {
                shown.add(subject);
            }
        }
        model.addAttribute("subjects", shown);
        model.addAttribute("totalSubjects", all.size());
        model.addAttribute("classes", classes.findAllByOrderByNameAsc());
        model.addAttribute("classId", classId);
        return "admin/subjects";
    }
}
