package com.attendance.tracker.service;

/**
 * One row of a student's attendance table: the classes of ONE subject
 * (how many Present, Late and Absent). "Attended" means Present or Late.
 * The formulas are in AttendanceMath.
 */
public class SubjectAttendance {

    /** Students must have at least this much attendance (AT-38). */
    public static final double MINIMUM_PERCENT = AttendanceMath.MINIMUM_PERCENT;

    private final String subjectName;
    private final String teacherName;
    private final int present;
    private final int late;
    private final int absent;

    public SubjectAttendance(String subjectName, String teacherName, int present, int late, int absent) {
        this.subjectName = subjectName;
        this.teacherName = teacherName;
        this.present = present;
        this.late = late;
        this.absent = absent;
    }

    public String getSubjectName() { return subjectName; }
    public String getTeacherName() { return teacherName; }
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

    /** AT-38: true when attendance is below 75%. */
    public boolean isBelowLimit() {
        return AttendanceMath.isBelowLimit(getAttended(), getTotal());
    }

    /** AT-64: "none", "low", "borderline" or "good" (used for badge and bar colours). */
    public String getLevel() {
        return AttendanceMath.level(getAttended(), getTotal());
    }

    /** AT-64: classes in a row needed to reach 75% again (0 if already fine). */
    public int getClassesNeeded() {
        return AttendanceMath.classesNeeded(getAttended(), getTotal());
    }
}
