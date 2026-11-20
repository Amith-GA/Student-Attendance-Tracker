package com.attendance.tracker.security;

import com.attendance.tracker.model.AppUser;
import com.attendance.tracker.repository.UserRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Tells Spring Security how to load a user from our database.
 * A deactivated user (active = false) is marked "disabled", so login is refused.
 */
@Service
public class AppUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public AppUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AppUser u = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("No such user: " + username));

        return User.withUsername(u.getUsername())
                .password(u.getPassword())
                .roles(u.getRole().name())   // becomes ROLE_ADMIN / ROLE_TEACHER / ROLE_STUDENT
                .disabled(!u.isActive())
                .build();
    }
}
