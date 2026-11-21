package com.attendance.tracker.config;

import com.attendance.tracker.model.AppUser;
import com.attendance.tracker.model.Role;
import com.attendance.tracker.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Creates the first login when the app starts on an empty database.
 * Only the admin account is created: the admin then adds classes, teachers and students
 * on the admin pages. If any user already exists, nothing is added.
 */
@Configuration
public class DataSeeder {

    @Bean
    public CommandLineRunner seedAdmin(UserRepository users, PasswordEncoder encoder) {
        return args -> {
            if (users.count() > 0) {
                return;
            }
            users.save(new AppUser("admin", encoder.encode("admin123"), "Admin User", Role.ADMIN));
        };
    }
}
