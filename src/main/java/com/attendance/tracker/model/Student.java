package com.attendance.tracker.model;

import jakarta.persistence.*;

/**
 * Student profile: roll number and the class the student is enrolled in.
 * The login details (username, password) live in the linked AppUser.
 */
@Entity
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String rollNo;

    // The login account of this student (role STUDENT)
    @OneToOne(optional = false)
    private AppUser user;

    // Many students belong to one class (this is the enrolment)
    @ManyToOne(optional = false)
    private ClassRoom classRoom;

    protected Student() { }

    public Student(String rollNo, AppUser user, ClassRoom classRoom) {
        this.rollNo = rollNo;
        this.user = user;
        this.classRoom = classRoom;
    }

    public Long getId() { return id; }
    public String getRollNo() { return rollNo; }
    public AppUser getUser() { return user; }
    public ClassRoom getClassRoom() { return classRoom; }
}
