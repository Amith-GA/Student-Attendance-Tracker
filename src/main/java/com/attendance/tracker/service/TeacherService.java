package com.attendance.tracker.service;

import com.attendance.tracker.model.AppUser;
import com.attendance.tracker.model.Role;
import com.attendance.tracker.model.Subject;
import com.attendance.tracker.repository.AttendanceRepository;
import com.attendance.tracker.repository.SubjectRepository;
import com.attendance.tracker.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Add, edit and delete teachers. A teacher is just an AppUser with role TEACHER.
 * Methods throw IllegalArgumentException with a readable message on bad input.
 */
@Service
public class TeacherService {

    private final UserRepository users;
    private final SubjectRepository subjects;
    private final AttendanceRepository attendance;
    private final PasswordEncoder encoder;

    public TeacherService(UserRepository users, SubjectRepository subjects,
                          AttendanceRepository attendance, PasswordEncoder encoder) {
        this.users = users;
        this.subjects = subjects;
        this.attendance = attendance;
        this.encoder = encoder;
    }

    @Transactional
    public void addTeacher(String fullName, String username, String password) {
        fullName = clean(fullName);
        username = clean(username);

        if (fullName.isEmpty() || username.isEmpty() || password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Please fill in every field.");
        }
        checkPassword(password);
        if (users.existsByUsername(username)) {
            throw new IllegalArgumentException("Username '" + username + "' is already taken.");
        }
        users.save(new AppUser(username, encoder.encode(password), fullName, Role.TEACHER));
    }

    /** Edit a teacher. Leave newPassword empty to keep the current password. */
    @Transactional
    public void updateTeacher(Long teacherId, String fullName, String username, String newPassword) {
        AppUser teacher = findTeacher(teacherId);

        fullName = clean(fullName);
        username = clean(username);

        if (fullName.isEmpty() || username.isEmpty()) {
            throw new IllegalArgumentException("Name and username cannot be empty.");
        }
        boolean changePassword = newPassword != null && !newPassword.isEmpty();
        if (changePassword) {
            checkPassword(newPassword);
        }
        if (users.existsByUsernameAndIdNot(username, teacherId)) {
            throw new IllegalArgumentException("Username '" + username + "' is already taken.");
        }

        teacher.setFullName(fullName);
        teacher.setUsername(username);
        if (changePassword) {
            teacher.setPassword(encoder.encode(newPassword));
        }
    }

    /**
     * Delete a teacher. Their subjects are kept but become "not assigned",
     * and attendance they saved is kept (it just no longer shows who marked it).
     */
    @Transactional
    public void deleteTeacher(Long teacherId) {
        AppUser teacher = findTeacher(teacherId);
        for (Subject subject : subjects.findByTeacherId(teacherId)) {
            subject.setTeacher(null);
        }
        attendance.clearMarkedBy(teacherId);
        subjects.flush(); // make sure the subjects are updated before the teacher row disappears
        users.delete(teacher);
    }

    // ---- small helpers ----

    /** Only returns users whose role is TEACHER, so this page can never edit an admin or student. */
    private AppUser findTeacher(Long teacherId) {
        AppUser user = users.findById(teacherId).orElse(null);
        if (user == null || user.getRole() != Role.TEACHER) {
            throw new IllegalArgumentException("Teacher not found.");
        }
        return user;
    }

    private String clean(String text) {
        return text == null ? "" : text.trim();
    }

    private void checkPassword(String password) {
        if (password.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters.");
        }
    }
}
