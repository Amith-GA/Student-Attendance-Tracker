package com.attendance.tracker.config;

import com.attendance.tracker.model.AppUser;
import com.attendance.tracker.model.ClassRoom;
import com.attendance.tracker.model.Role;
import com.attendance.tracker.model.Student;
import com.attendance.tracker.model.Subject;
import com.attendance.tracker.repository.ClassRoomRepository;
import com.attendance.tracker.repository.StudentRepository;
import com.attendance.tracker.repository.SubjectRepository;
import com.attendance.tracker.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Creates demo data when the app starts, so you can log in and test straight away.
 * It only adds what is missing, so it is safe to run on every start.
 * Both steps live in ONE runner so that they always run in this order:
 * first the accounts, then the demo student's class.
 */
@Configuration
public class DataSeeder {

    private static final String DEMO_CLASS = "CSE 5A";

    @Bean
    public CommandLineRunner seedDemoData(UserRepository users, PasswordEncoder encoder,
                                          ClassRoomRepository classes, SubjectRepository subjects,
                                          StudentRepository students) {
        return args -> {
            seedUsers(users, encoder);
            seedDemoStudentProfile(users, classes, subjects, students);
        };
    }

    /** Step 1: three demo accounts, only on a completely empty database. */
    private void seedUsers(UserRepository users, PasswordEncoder encoder) {
        if (users.count() > 0) {
            return;
        }
        users.save(new AppUser("admin", encoder.encode("admin123"), "Admin User", Role.ADMIN));
        users.save(new AppUser("teacher1", encoder.encode("teacher123"), "Teacher One", Role.TEACHER));
        users.save(new AppUser("student1", encoder.encode("student123"), "Student One", Role.STUDENT));
    }

    /**
     * Step 2: make sure the demo account "student1" belongs to a class.
     * Without a student profile (class + roll number) the student dashboard would be empty.
     * If the demo class does not exist yet it is created with three subjects taught by teacher1.
     */
    private void seedDemoStudentProfile(UserRepository users, ClassRoomRepository classes,
                                        SubjectRepository subjects, StudentRepository students) {
        AppUser student1 = users.findByUsername("student1").orElse(null);
        if (student1 == null || student1.getRole() != Role.STUDENT
                || students.existsByUserId(student1.getId())) {
            return; // nothing to do
        }

        ClassRoom demoClass = classes.findAllByOrderByNameAsc().stream()
                .filter(c -> c.getName().equalsIgnoreCase(DEMO_CLASS))
                .findFirst().orElse(null);
        if (demoClass == null) {
            demoClass = classes.save(new ClassRoom(DEMO_CLASS));
            AppUser teacher1 = users.findByUsername("teacher1").orElse(null);
            for (String name : new String[]{"Mathematics", "Physics", "English"}) {
                subjects.save(new Subject(name, demoClass, teacher1));
            }
        }

        // roll number "001" unless that is already used in this class
        String rollNo = "001";
        if (students.existsByRollNoIgnoreCaseAndClassRoomId(rollNo, demoClass.getId())) {
            rollNo = "S1-001";
        }
        students.save(new Student(rollNo, student1, demoClass));
    }
}
