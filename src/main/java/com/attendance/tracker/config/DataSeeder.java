package com.attendance.tracker.config;

import com.attendance.tracker.model.AppUser;
import com.attendance.tracker.model.Role;
import com.attendance.tracker.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Creates three demo accounts the first time the app starts,
 * so you can log in and test straight away.
 */
@Configuration
public class DataSeeder {

    @Bean
    public CommandLineRunner seedUsers(UserRepository users, PasswordEncoder encoder) {
        return args -> {
            if (users.count() > 0) {
                return;
            }
            users.save(new AppUser("admin", encoder.encode("admin123"), "Admin User", Role.ADMIN));
            users.save(new AppUser("teacher1", encoder.encode("teacher123"), "Teacher One", Role.TEACHER));
            users.save(new AppUser("student1", encoder.encode("student123"), "Student One", Role.STUDENT));
        };
    }
}
