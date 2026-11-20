package com.attendance.tracker.controller;

import com.attendance.tracker.model.AppUser;
import com.attendance.tracker.model.Role;
import com.attendance.tracker.repository.UserRepository;
import com.attendance.tracker.service.TeacherService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Admin adds, edits and deletes teacher accounts.
 */
@Controller
public class AdminTeacherController {

    private final UserRepository users;
    private final TeacherService teacherService;

    public AdminTeacherController(UserRepository users, TeacherService teacherService) {
        this.users = users;
        this.teacherService = teacherService;
    }

    @GetMapping("/admin/teachers")
    public String page(Model model) {
        model.addAttribute("teachers", users.findByRole(Role.TEACHER));
        return "admin/teachers";
    }

    @PostMapping("/admin/teachers")
    public String addTeacher(@RequestParam String fullName,
                             @RequestParam String username,
                             @RequestParam String password,
                             RedirectAttributes flash) {
        try {
            teacherService.addTeacher(fullName, username, password);
            flash.addFlashAttribute("message", "Teacher '" + fullName.trim() + "' added. "
                    + "They can log in as '" + username.trim() + "'.");
        } catch (IllegalArgumentException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/teachers";
    }

    /** Show the edit form for one teacher. */
    @GetMapping("/admin/teachers/{id}/edit")
    public String editForm(@PathVariable Long id, Model model, RedirectAttributes flash) {
        AppUser teacher = users.findById(id).orElse(null);
        // only teachers can be edited on this page (never an admin or a student)
        if (teacher == null || teacher.getRole() != Role.TEACHER) {
            flash.addFlashAttribute("error", "Teacher not found.");
            return "redirect:/admin/teachers";
        }
        model.addAttribute("teacher", teacher);
        return "admin/teacher-edit";
    }

    /** Save the changes. An empty password box keeps the old password. */
    @PostMapping("/admin/teachers/{id}/edit")
    public String edit(@PathVariable Long id,
                       @RequestParam String fullName,
                       @RequestParam String username,
                       @RequestParam(defaultValue = "") String newPassword,
                       RedirectAttributes flash) {
        try {
            teacherService.updateTeacher(id, fullName, username, newPassword);
            flash.addFlashAttribute("message", "Teacher '" + fullName.trim() + "' updated.");
            return "redirect:/admin/teachers";
        } catch (IllegalArgumentException e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/teachers/" + id + "/edit";
        }
    }

    @PostMapping("/admin/teachers/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes flash) {
        try {
            teacherService.deleteTeacher(id);
            flash.addFlashAttribute("message", "Teacher deleted. Their subjects are now 'not assigned'.");
        } catch (IllegalArgumentException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/teachers";
    }
}
