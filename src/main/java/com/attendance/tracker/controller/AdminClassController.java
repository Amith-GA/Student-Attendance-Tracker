package com.attendance.tracker.controller;

import com.attendance.tracker.model.AppUser;
import com.attendance.tracker.model.ClassRoom;
import com.attendance.tracker.model.Role;
import com.attendance.tracker.model.Subject;
import com.attendance.tracker.repository.ClassRoomRepository;
import com.attendance.tracker.repository.SubjectRepository;
import com.attendance.tracker.repository.UserRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * AT-13: admin adds classes and subjects (and assigns a teacher to each subject).
 * Only ADMIN users can open /admin/** (see SecurityConfig).
 */
@Controller
public class AdminClassController {

    private final ClassRoomRepository classes;
    private final SubjectRepository subjects;
    private final UserRepository users;

    public AdminClassController(ClassRoomRepository classes, SubjectRepository subjects,
                                UserRepository users) {
        this.classes = classes;
        this.subjects = subjects;
        this.users = users;
    }

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
}
