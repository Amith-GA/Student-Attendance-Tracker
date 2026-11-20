package com.attendance.tracker.controller;

import com.attendance.tracker.model.AppUser;
import com.attendance.tracker.model.ClassRoom;
import com.attendance.tracker.model.Role;
import com.attendance.tracker.model.Subject;
import com.attendance.tracker.repository.ClassRoomRepository;
import com.attendance.tracker.repository.StudentRepository;
import com.attendance.tracker.repository.SubjectRepository;
import com.attendance.tracker.repository.UserRepository;
import com.attendance.tracker.service.SubjectService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * AT-13: admin adds, edits and deletes classes and subjects
 * (and assigns a teacher to each subject).
 * Only ADMIN users can open /admin/** (see SecurityConfig).
 */
@Controller
public class AdminClassController {

    private final ClassRoomRepository classes;
    private final SubjectRepository subjects;
    private final StudentRepository students;
    private final UserRepository users;
    private final SubjectService subjectService;

    public AdminClassController(ClassRoomRepository classes, SubjectRepository subjects,
                                StudentRepository students, UserRepository users,
                                SubjectService subjectService) {
        this.classes = classes;
        this.subjects = subjects;
        this.students = students;
        this.users = users;
        this.subjectService = subjectService;
    }

    // ===================== Classes =====================

    /** Show the page: forms plus the list of classes and subjects. */
    @GetMapping("/admin/classes")
    public String page(Model model) {
        model.addAttribute("classes", classes.findAllByOrderByNameAsc());
        model.addAttribute("subjects", subjects.findAllByOrderByClassRoomNameAscNameAsc());
        model.addAttribute("teachers", users.findByRole(Role.TEACHER));
        return "admin/classes";
    }

    /** Add a new class. */
    @PostMapping("/admin/classes")
    public String addClass(@RequestParam String name, RedirectAttributes flash) {
        name = name.trim();
        if (name.isEmpty()) {
            flash.addFlashAttribute("error", "Class name cannot be empty.");
        } else if (classes.existsByNameIgnoreCase(name)) {
            flash.addFlashAttribute("error", "Class '" + name + "' already exists.");
        } else {
            classes.save(new ClassRoom(name));
            flash.addFlashAttribute("message", "Class '" + name + "' added.");
        }
        return "redirect:/admin/classes";
    }

    /** Show the edit form for one class. */
    @GetMapping("/admin/classes/{id}/edit")
    public String editClassForm(@PathVariable Long id, Model model, RedirectAttributes flash) {
        ClassRoom classRoom = classes.findById(id).orElse(null);
        if (classRoom == null) {
            flash.addFlashAttribute("error", "Class not found.");
            return "redirect:/admin/classes";
        }
        model.addAttribute("classRoom", classRoom);
        return "admin/class-edit";
    }

    /** Save the new class name. */
    @PostMapping("/admin/classes/{id}/edit")
    public String editClass(@PathVariable Long id, @RequestParam String name, RedirectAttributes flash) {
        ClassRoom classRoom = classes.findById(id).orElse(null);
        name = name.trim();
        if (classRoom == null) {
            flash.addFlashAttribute("error", "Class not found.");
        } else if (name.isEmpty()) {
            flash.addFlashAttribute("error", "Class name cannot be empty.");
            return "redirect:/admin/classes/" + id + "/edit";
        } else if (!name.equalsIgnoreCase(classRoom.getName()) && classes.existsByNameIgnoreCase(name)) {
            // another class already uses this name
            flash.addFlashAttribute("error", "Class '" + name + "' already exists.");
            return "redirect:/admin/classes/" + id + "/edit";
        } else {
            classRoom.setName(name);
            classes.save(classRoom);
            flash.addFlashAttribute("message", "Class renamed to '" + name + "'.");
        }
        return "redirect:/admin/classes";
    }

    /** Delete a class, but only when it has no subjects and no students left. */
    @PostMapping("/admin/classes/{id}/delete")
    public String deleteClass(@PathVariable Long id, RedirectAttributes flash) {
        ClassRoom classRoom = classes.findById(id).orElse(null);
        if (classRoom == null) {
            flash.addFlashAttribute("error", "Class not found.");
        } else if (subjects.existsByClassRoomId(id) || students.existsByClassRoomId(id)) {
            flash.addFlashAttribute("error", "Class '" + classRoom.getName()
                    + "' still has subjects or students. Delete or move them first.");
        } else {
            classes.delete(classRoom);
            flash.addFlashAttribute("message", "Class '" + classRoom.getName() + "' deleted.");
        }
        return "redirect:/admin/classes";
    }

    // ===================== Subjects =====================

    /** Add a subject to a class, optionally with a teacher. */
    @PostMapping("/admin/subjects")
    public String addSubject(@RequestParam String name,
                             @RequestParam(required = false) Long classId,
                             @RequestParam(required = false) Long teacherId,
                             RedirectAttributes flash) {
        name = name.trim();
        ClassRoom classRoom = (classId == null) ? null : classes.findById(classId).orElse(null);
        // teacherId is empty when "no teacher yet" is chosen
        AppUser teacher = (teacherId == null) ? null : users.findById(teacherId).orElse(null);

        if (name.isEmpty()) {
            flash.addFlashAttribute("error", "Subject name cannot be empty.");
        } else if (classRoom == null) {
            flash.addFlashAttribute("error", "Please choose a class for the subject.");
        } else if (teacher != null && teacher.getRole() != Role.TEACHER) {
            flash.addFlashAttribute("error", "The chosen user is not a teacher.");
        } else if (subjects.existsByNameIgnoreCaseAndClassRoomId(name, classId)) {
            flash.addFlashAttribute("error",
                    "'" + name + "' is already a subject of " + classRoom.getName() + ".");
        } else {
            subjects.save(new Subject(name, classRoom, teacher));
            flash.addFlashAttribute("message",
                    "Subject '" + name + "' added to " + classRoom.getName() + ".");
        }
        return "redirect:/admin/classes";
    }

    /** Show the edit form for one subject (name and teacher can be changed). */
    @GetMapping("/admin/subjects/{id}/edit")
    public String editSubjectForm(@PathVariable Long id, Model model, RedirectAttributes flash) {
        Subject subject = subjects.findById(id).orElse(null);
        if (subject == null) {
            flash.addFlashAttribute("error", "Subject not found.");
            return "redirect:/admin/classes";
        }
        model.addAttribute("subject", subject);
        model.addAttribute("teachers", users.findByRole(Role.TEACHER));
        return "admin/subject-edit";
    }

    /** Save the changed subject name / teacher. */
    @PostMapping("/admin/subjects/{id}/edit")
    public String editSubject(@PathVariable Long id,
                              @RequestParam String name,
                              @RequestParam(required = false) Long teacherId,
                              RedirectAttributes flash) {
        Subject subject = subjects.findById(id).orElse(null);
        if (subject == null) {
            flash.addFlashAttribute("error", "Subject not found.");
            return "redirect:/admin/classes";
        }
        name = name.trim();
        AppUser teacher = (teacherId == null) ? null : users.findById(teacherId).orElse(null);

        if (name.isEmpty()) {
            flash.addFlashAttribute("error", "Subject name cannot be empty.");
        } else if (teacher != null && teacher.getRole() != Role.TEACHER) {
            flash.addFlashAttribute("error", "The chosen user is not a teacher.");
        } else if (subjects.existsByNameIgnoreCaseAndClassRoomIdAndIdNot(
                name, subject.getClassRoom().getId(), id)) {
            flash.addFlashAttribute("error", "'" + name + "' is already a subject of "
                    + subject.getClassRoom().getName() + ".");
        } else {
            subject.setName(name);
            subject.setTeacher(teacher);
            subjects.save(subject);
            flash.addFlashAttribute("message", "Subject '" + name + "' updated.");
            return "redirect:/admin/classes";
        }
        return "redirect:/admin/subjects/" + id + "/edit";
    }

    /** Delete a subject together with its attendance records. */
    @PostMapping("/admin/subjects/{id}/delete")
    public String deleteSubject(@PathVariable Long id, RedirectAttributes flash) {
        try {
            subjectService.deleteSubject(id);
            flash.addFlashAttribute("message", "Subject deleted.");
        } catch (IllegalArgumentException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/classes";
    }
}
