package com.attendance.tracker.model;

import jakarta.persistence.*;

/**
 * A subject taught to one class, for example "Mathematics" in "CSE 5A".
 * Each subject can be assigned to a teacher, so later a teacher only
 * sees (and marks attendance for) their own subjects.
 */
@Entity
public class Subject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    // Many subjects belong to one class
    @ManyToOne(optional = false)
    private ClassRoom classRoom;

    // The teacher (an AppUser with role TEACHER). Can be empty until the admin assigns one.
    @ManyToOne
    private AppUser teacher;

    protected Subject() { }

    public Subject(String name, ClassRoom classRoom, AppUser teacher) {
        this.name = name;
        this.classRoom = classRoom;
        this.teacher = teacher;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public ClassRoom getClassRoom() { return classRoom; }
    public AppUser getTeacher() { return teacher; }
}
