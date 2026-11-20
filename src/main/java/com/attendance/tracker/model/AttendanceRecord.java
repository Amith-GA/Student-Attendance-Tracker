package com.attendance.tracker.model;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * One row = one student's attendance for one subject on one date.
 * The unique constraint means the same student/subject/date can never be saved twice,
 * even if the teacher double-clicks Save.
 */
@Entity
@Table(name = "attendance_record",
       uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "subject_id", "attendance_date"}))
public class AttendanceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "student_id")
    private Student student;

    @ManyToOne(optional = false)
    @JoinColumn(name = "subject_id")
    private Subject subject;

    // The day the class was held (the teacher picks it)
    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AttendanceStatus status;

    // The teacher who saved it. Becomes empty if that teacher account is deleted later.
    @ManyToOne
    @JoinColumn(name = "marked_by_id")
    private AppUser markedBy;

    // When the row was created. Used later to allow editing only on the same day.
    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected AttendanceRecord() { }

    public AttendanceRecord(Student student, Subject subject, LocalDate attendanceDate,
                            AttendanceStatus status, AppUser markedBy) {
        this.student = student;
        this.subject = subject;
        this.attendanceDate = attendanceDate;
        this.status = status;
        this.markedBy = markedBy;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Student getStudent() { return student; }
    public Subject getSubject() { return subject; }
    public LocalDate getAttendanceDate() { return attendanceDate; }
    public AttendanceStatus getStatus() { return status; }
    public AppUser getMarkedBy() { return markedBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
