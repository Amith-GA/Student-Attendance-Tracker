package com.attendance.tracker.service;

/**
 * One row of a student's attendance table: how many classes of ONE subject
 * the student attended, out of how many were held.
 * "Attended" means Present or Late. The formulas are in AttendanceMath.
 */
public class SubjectAttendance {

    /** Students must have at least this much attendance (AT-38). */
    public static final double MINIMUM_PERCENT = AttendanceMath.MINIMUM_PERCENT;

    private final String subjectName;
    private final int attended;
    private final int total;

    public SubjectAttendance(String subjectName, int attended, int total) {
        this.subjectName = subjectName;
        this.attended = attended;
        this.total = total;
    }

    public String getSubjectName() { return subjectName; }
    public int getAttended() { return attended; }
    public int getTotal() { return total; }

    /** True when no class has been held yet, so there is no percentage to show. */
    public boolean isNoClassesYet() {
        return total == 0;
    }

    public double getPercentage() {
        return AttendanceMath.percentage(attended, total);
    }

    /** AT-38: true when attendance is below 75%. */
    public boolean isBelowLimit() {
        return AttendanceMath.isBelowLimit(attended, total);
    }
}
