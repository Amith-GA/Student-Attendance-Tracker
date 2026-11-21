package com.attendance.tracker.repository;

import com.attendance.tracker.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import com.attendance.tracker.model.Role;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByUsername(String username);

    boolean existsByUsername(String username);

    // Used when editing: the username may clash with anyone EXCEPT the user being edited
    boolean existsByUsernameAndIdNot(String username, Long id);

    // Used to fill the "assign teacher" drop-down
    List<AppUser> findByRole(Role role);

    // How many accounts have this role (shown on the admin dashboard)
    long countByRole(Role role);
}
