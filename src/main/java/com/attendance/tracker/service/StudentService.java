package com.attendance.tracker.service;

import com.attendance.tracker.model.AppUser;
import com.attendance.tracker.model.ClassRoom;
import com.attendance.tracker.model.Role;
import com.attendance.tracker.model.Student;
import com.attendance.tracker.repository.ClassRoomRepository;
import com.attendance.tracker.repository.StudentRepository;
import com.attendance.tracker.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adding a student needs two saves (the login account AND the student profile).
 * @Transactional makes them succeed or fail together, so we never end up
 * with a login account that has no student profile.
 */
@Service
public class StudentService {

    private final UserRepository users;
    private final StudentRepository students;
    private final ClassRoomRepository classes;
    private final PasswordEncoder encoder;

    public StudentService(UserRepository users, StudentRepository students,
                          ClassRoomRepository classes, PasswordEncoder encoder) {
        this.users = users;
        this.students = students;
        this.classes = classes;
        this.encoder = encoder;
    }

    /** Throws IllegalArgumentException with a readable message if the input is not valid. */
    @Transactional
    public void addStudent(String fullName, String username, String password,
                           String rollNo, Long classId) {
        fullName = fullName == null ? "" : fullName.trim();
        username = username == null ? "" : username.trim();
        rollNo = rollNo == null ? "" : rollNo.trim();

        if (fullName.isEmpty() || username.isEmpty() || rollNo.isEmpty()
                || password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Please fill in every field.");
        }
        if (password.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters.");
        }
        ClassRoom classRoom = (classId == null) ? null : classes.findById(classId).orElse(null);
        if (classRoom == null) {
            throw new IllegalArgumentException("Please choose a class.");
        }
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
}
