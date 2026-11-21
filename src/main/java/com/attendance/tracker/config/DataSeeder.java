package com.attendance.tracker.config;

import com.attendance.tracker.model.AppUser;
import com.attendance.tracker.model.Role;
import com.attendance.tracker.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Runs when the app starts, on an empty database:
 *  1) creates the first login, the admin (the admin then adds everything else on the admin pages)
 *  2) loads a real batch from seed-local/students.csv if that file exists (see BatchImporter, AT-85)
 * Both steps are in ONE runner so that they always run in this order.
 * If any user already exists, step 1 adds nothing, and step 2 also does nothing.
 */
@Configuration
public class DataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    @Bean
    public CommandLineRunner seedData(UserRepository users, PasswordEncoder encoder, BatchImporter importer) {
        return args -> {
            if (users.count() == 0) {
                users.save(new AppUser("admin", encoder.encode("admin123"), "Admin User", Role.ADMIN));
            }
            try {
                String summary = importer.importIfPresent();
                if (summary != null) {
                    log.info(summary);
                }
            } catch (RuntimeException e) {
                // nothing was saved (one transaction); the app still starts with just the admin
                log.error("Batch data was NOT loaded: {}", e.getMessage());
            }
        };
    }
}
