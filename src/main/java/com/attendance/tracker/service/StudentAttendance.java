package com.attendance.tracker.service;

/**
 * One row of the teacher's class report: one student's attendance in ONE subject.
 * "Attended" means Present or Late. The formulas are in AttendanceMath.
 */
public class StudentAttendance {

    private final String rollNo;
    private final String studentName;
    private final int attended;
    private final int total;

    public StudentAttendance(String rollNo, String studentName, int attended, int total) {
        this.rollNo = rollNo;
        this.studentName = studentName;
        this.attended = attended;
        this.total = total;
    }

    public String getRollNo() { return rollNo; }
    public String getStudentName() { return studentName; }
    public int getAttended() { return attended; }
    public int getTotal() { return total; }

    /** True when no class has been held yet, so there is no percentage to show. */
    public boolean isNoClassesYet() {
        return total == 0;
    }

    public double getPercentage() {
        return AttendanceMath.percentage(attended, total);
    }

    public boolean isBelowLimit() {
        return AttendanceMath.isBelowLimit(attended, total);
    }
}
