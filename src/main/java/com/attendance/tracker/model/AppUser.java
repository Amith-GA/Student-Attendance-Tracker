package com.attendance.tracker.model;

import jakarta.persistence.*;

/**
 * One table for all three kinds of users (admin, teacher, student).
 * Table is named app_user because "user" is a reserved word in H2.
 */
@Entity
@Table(name = "app_user")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password; // BCrypt hash, never plain text

    @Column(nullable = false)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    // false = deactivated account (used later for AT "deactivate a student")
    @Column(nullable = false)
    private boolean active = true;

    protected AppUser() { }

    public AppUser(String username, String password, String fullName, Role role) {
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.role = role;
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getFullName() { return fullName; }
    public Role getRole() { return role; }
    public boolean isActive() { return active; }

    public void setUsername(String username) { this.username = username; }
    public void setPassword(String password) { this.password = password; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setActive(boolean active) { this.active = active; }
}
