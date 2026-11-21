package com.attendance.tracker.service;

import com.attendance.tracker.model.Subject;

/**
 * AT-70: one line of the teacher dashboard. One of the teacher's subjects (which also fixes the class),
 * with how many students it has, how many classes were held so far, and whether
 * attendance has already been taken today.
 */
public class SubjectOverview {

    private final Subject subject;
    private final int students;
    private final int classesHeld;
    private final boolean takenToday;

    public SubjectOverview(Subject subject, int students, int classesHeld, boolean takenToday) {
        this.subject = subject;
        this.students = students;
        this.classesHeld = classesHeld;
        this.takenToday = takenToday;
    }

    public Subject getSubject() { return subject; }
    public int getStudents() { return students; }
    public int getClassesHeld() { return classesHeld; }
    public boolean isTakenToday() { return takenToday; }
}
