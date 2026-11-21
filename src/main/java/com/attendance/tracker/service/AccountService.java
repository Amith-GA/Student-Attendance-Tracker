package com.attendance.tracker.service;

import com.attendance.tracker.model.AppUser;
import com.attendance.tracker.model.Role;
import com.attendance.tracker.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * AT-59: password changes.
 *  - changePassword: a logged-in user changes their OWN password
 *  - resetPassword:  the admin sets a new password for a student or teacher who forgot theirs
 * Passwords are always stored as BCrypt hashes, never as plain text.
 * Throws IllegalArgumentException with a readable message when something is wrong.
 */
@Service
public class AccountService {

    private static final int MIN_LENGTH = 6;

    private final UserRepository users;
    private final PasswordEncoder encoder;

    public AccountService(UserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    /** A user changes their own password. They must know the current one. */
    @Transactional
    public void changePassword(String username, String currentPassword,
                               String newPassword, String confirmPassword) {
        AppUser user = users.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        // matches() compares the typed password with the stored BCrypt hash
        if (currentPassword == null || !encoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Your current password is not correct.");
        }
        checkNewPassword(newPassword);
        if (!newPassword.equals(confirmPassword)) {
            throw new IllegalArgumentException("The new password and the confirmation do not match.");
        }
        if (encoder.matches(newPassword, user.getPassword())) {
            throw new IllegalArgumentException("The new password must be different from the current one.");
        }
        // the user is a managed object, so this change is saved when the method ends
        user.setPassword(encoder.encode(newPassword));
    }

    /**
     * The admin sets a new (temporary) password for a student or teacher.
     * Admin accounts cannot be reset here.
     * @return the account, so the page can say whose password was changed
     */
    @Transactional
    public AppUser resetPassword(Long userId, String newPassword) {
        AppUser user = (userId == null) ? null : users.findById(userId).orElse(null);
        if (user == null || user.getRole() == Role.ADMIN) {
            throw new IllegalArgumentException("User not found.");
        }
        checkNewPassword(newPassword);
        user.setPassword(encoder.encode(newPassword));
        return user;
    }

    private void checkNewPassword(String password) {
        if (password == null || password.length() < MIN_LENGTH) {
            throw new IllegalArgumentException("Password must be at least " + MIN_LENGTH + " characters.");
        }
    }
}
