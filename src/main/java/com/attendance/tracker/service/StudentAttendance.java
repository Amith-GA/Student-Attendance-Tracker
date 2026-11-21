package com.attendance.tracker.service;

/**
 * One row of the teacher's class report: one student's attendance in ONE subject
 * (how many Present, Late and Absent classes).
 * "Attended" means Present or Late. The formulas are in AttendanceMath.
 */
public class StudentAttendance {

    private final String rollNo;
    private final String studentName;
    private final int present;
    private final int late;
    private final int absent;

    public StudentAttendance(String rollNo, String studentName, int present, int late, int absent) {
        this.rollNo = rollNo;
        this.studentName = studentName;
        this.present = present;
        this.late = late;
        this.absent = absent;
    }

    public String getRollNo() { return rollNo; }
    public String getStudentName() { return studentName; }
    public int getPresent() { return present; }
    public int getLate() { return late; }
    public int getAbsent() { return absent; }

    /** Present and Late both count as attended. */
    public int getAttended() { return present + late; }

    public int getTotal() { return present + late + absent; }

    /** True when no class has been held yet, so there is no percentage to show. */
    public boolean isNoClassesYet() {
        return getTotal() == 0;
    }

    public double getPercentage() {
        return AttendanceMath.percentage(getAttended(), getTotal());
    }

    public boolean isBelowLimit() {
        return AttendanceMath.isBelowLimit(getAttended(), getTotal());
    }

    /** AT-70: "none", "low", "borderline" or "good" (used for badge and bar colours). */
    public String getLevel() {
        return AttendanceMath.level(getAttended(), getTotal());
    }
}
