package com.attendance.tracker.model;

import jakarta.persistence.*;

/**
 * A class / section of students, for example "CSE 5A".
 * (Named ClassRoom because "Class" is already a Java keyword/type.)
 */
@Entity
public class ClassRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    protected ClassRoom() { }

    public ClassRoom(String name) {
        this.name = name;
    }

    public Long getId() { return id; }
    public String getName() { return name; }

    public void setName(String name) { this.name = name; }
}
