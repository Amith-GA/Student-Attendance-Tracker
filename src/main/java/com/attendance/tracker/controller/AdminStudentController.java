package com.attendance.tracker.controller;

import com.attendance.tracker.model.Student;
import com.attendance.tracker.repository.ClassRoomRepository;
import com.attendance.tracker.repository.StudentRepository;
import com.attendance.tracker.service.StudentService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * AT-13: admin adds, edits and deletes students. Each student also has a login account.
 */
@Controller
public class AdminStudentController {

    private final StudentRepository students;
    private final ClassRoomRepository classes;
    private final StudentService studentService;

    public AdminStudentController(StudentRepository students, ClassRoomRepository classes,
                                  StudentService studentService) {
        this.students = students;
        this.classes = classes;
        this.studentService = studentService;
    }

    @GetMapping("/admin/students")
    public String page(Model model) {
        model.addAttribute("classes", classes.findAllByOrderByNameAsc());
        model.addAttribute("students", students.findAllByOrderByClassRoomNameAscRollNoAsc());
        return "admin/students";
    }

    @PostMapping("/admin/students")
    public String addStudent(@RequestParam String fullName,
                             @RequestParam String username,
                             @RequestParam String password,
                             @RequestParam String rollNo,
                             @RequestParam(required = false) Long classId,
                             RedirectAttributes flash) {
        try {
            studentService.addStudent(fullName, username, password, rollNo, classId);
            flash.addFlashAttribute("message", "Student '" + fullName.trim() + "' added. "
                    + "They can log in as '" + username.trim() + "'.");
        } catch (IllegalArgumentException e) {
            // validation problem: show the message on the page
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/students";
    }

    /** Show the edit form for one student. */
    @GetMapping("/admin/students/{id}/edit")
    public String editForm(@PathVariable Long id, Model model, RedirectAttributes flash) {
        Student student = students.findById(id).orElse(null);
        if (student == null) {
            flash.addFlashAttribute("error", "Student not found.");
            return "redirect:/admin/students";
        }
        model.addAttribute("student", student);
        model.addAttribute("classes", classes.findAllByOrderByNameAsc());
        return "admin/student-edit";
    }

    /** Save the changes. An empty password box keeps the old password. */
    @PostMapping("/admin/students/{id}/edit")
    public String edit(@PathVariable Long id,
                       @RequestParam String fullName,
                       @RequestParam String username,
                       @RequestParam(defaultValue = "") String newPassword,
                       @RequestParam String rollNo,
                       @RequestParam(required = false) Long classId,
                       RedirectAttributes flash) {
        try {
            studentService.updateStudent(id, fullName, username, newPassword, rollNo, classId);
            flash.addFlashAttribute("message", "Student '" + fullName.trim() + "' updated.");
            return "redirect:/admin/students";
        } catch (IllegalArgumentException e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/students/" + id + "/edit";
        }
    }

    /** AT-49: a former student can no longer log in. Their attendance records are kept. */
    @PostMapping("/admin/students/{id}/deactivate")
    public String deactivate(@PathVariable Long id, RedirectAttributes flash) {
        try {
            studentService.setActive(id, false);
            flash.addFlashAttribute("message", "Student deactivated. They can no longer log in; "
                    + "their attendance records are kept.");
        } catch (IllegalArgumentException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/students";
    }

    /** AT-49: let a deactivated student log in again. */
    @PostMapping("/admin/students/{id}/activate")
    public String activate(@PathVariable Long id, RedirectAttributes flash) {
        try {
            studentService.setActive(id, true);
            flash.addFlashAttribute("message", "Student activated. They can log in again.");
        } catch (IllegalArgumentException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/students";
    }

    @PostMapping("/admin/students/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes flash) {
        try {
            studentService.deleteStudent(id);
            flash.addFlashAttribute("message", "Student deleted.");
        } catch (IllegalArgumentException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/students";
    }
}
