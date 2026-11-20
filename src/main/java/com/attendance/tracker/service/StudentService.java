package com.attendance.tracker.service;

import com.attendance.tracker.model.AppUser;
import com.attendance.tracker.model.ClassRoom;
import com.attendance.tracker.model.Role;
import com.attendance.tracker.model.Student;
import com.attendance.tracker.repository.AttendanceRepository;
import com.attendance.tracker.repository.ClassRoomRepository;
import com.attendance.tracker.repository.StudentRepository;
import com.attendance.tracker.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Add, edit and delete students.
 * A student is stored in two tables (the login account AND the student profile),
 * so every method here is @Transactional: both saves succeed or both are undone.
 * Every method throws IllegalArgumentException with a readable message
 * when the input is not valid; the controller shows that message on the page.
 */
@Service
public class StudentService {

    private final UserRepository users;
    private final StudentRepository students;
    private final ClassRoomRepository classes;
    private final AttendanceRepository attendance;
    private final PasswordEncoder encoder;

    public StudentService(UserRepository users, StudentRepository students,
                          ClassRoomRepository classes, AttendanceRepository attendance,
                          PasswordEncoder encoder) {
        this.users = users;
        this.students = students;
        this.classes = classes;
        this.attendance = attendance;
        this.encoder = encoder;
    }

    @Transactional
    public void addStudent(String fullName, String username, String password,
                           String rollNo, Long classId) {
        fullName = clean(fullName);
        username = clean(username);
        rollNo = clean(rollNo);

        if (fullName.isEmpty() || username.isEmpty() || rollNo.isEmpty()
                || password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Please fill in every field.");
        }
        checkPassword(password);
        ClassRoom classRoom = findClass(classId);
        if (users.existsByUsername(username)) {
            throw new IllegalArgumentException("Username '" + username + "' is already taken.");
        }
        if (students.existsByRollNoIgnoreCaseAndClassRoomId(rollNo, classId)) {
            throw new IllegalArgumentException(
                    "Roll number " + rollNo + " already exists in " + classRoom.getName() + ".");
        }

        // 1) the login account (password is stored as a BCrypt hash)
        AppUser user = users.save(new AppUser(username, encoder.encode(password), fullName, Role.STUDENT));
        // 2) the student profile linked to that account
        students.save(new Student(rollNo, user, classRoom));
    }

    /** Edit a student. Leave newPassword empty to keep the current password. */
    @Transactional
    public void updateStudent(Long studentId, String fullName, String username, String newPassword,
                              String rollNo, Long classId) {
        Student student = students.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found."));
        AppUser user = student.getUser();

        fullName = clean(fullName);
        username = clean(username);
        rollNo = clean(rollNo);

        if (fullName.isEmpty() || username.isEmpty() || rollNo.isEmpty()) {
            throw new IllegalArgumentException("Name, username and roll number cannot be empty.");
        }
        boolean changePassword = newPassword != null && !newPassword.isEmpty();
        if (changePassword) {
            checkPassword(newPassword);
        }
        ClassRoom classRoom = findClass(classId);
        if (users.existsByUsernameAndIdNot(username, user.getId())) {
            throw new IllegalArgumentException("Username '" + username + "' is already taken.");
        }
        if (students.existsByRollNoIgnoreCaseAndClassRoomIdAndIdNot(rollNo, classId, studentId)) {
            throw new IllegalArgumentException(
                    "Roll number " + rollNo + " already exists in " + classRoom.getName() + ".");
        }

        // Changes to these managed objects are saved automatically when the method ends
        user.setFullName(fullName);
        user.setUsername(username);
        if (changePassword) {
            user.setPassword(encoder.encode(newPassword));
        }
        student.setRollNo(rollNo);
        student.setClassRoom(classRoom);
    }

    /** Delete the student's attendance records, the student profile and the login account. */
    @Transactional
    public void deleteStudent(Long studentId) {
        Student student = students.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found."));
        AppUser user = student.getUser();
        attendance.deleteByStudentId(studentId);
        students.delete(student);
        users.delete(user);
    }

    // ---- small helpers ----

    private String clean(String text) {
        return text == null ? "" : text.trim();
    }

    private void checkPassword(String password) {
        if (password.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters.");
        }
    }

    private ClassRoom findClass(Long classId) {
        ClassRoom classRoom = (classId == null) ? null : classes.findById(classId).orElse(null);
        if (classRoom == null) {
            throw new IllegalArgumentException("Please choose a class.");
        }
        return classRoom;
    }
}
