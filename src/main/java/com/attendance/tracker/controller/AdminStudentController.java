package com.attendance.tracker.controller;

import com.attendance.tracker.repository.ClassRoomRepository;
import com.attendance.tracker.repository.StudentRepository;
import com.attendance.tracker.service.StudentService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * AT-13: admin adds students. Each new student also gets a login account.
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
}
