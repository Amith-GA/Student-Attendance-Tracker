package com.attendance.tracker.service;

/**
 * One row of a student's attendance table: how many classes of ONE subject
 * the student attended, out of how many were held.
 * "Attended" means Present or Late.
 */
public class SubjectAttendance {

    /** Students must have at least this much attendance (AT-38). */
    public static final double MINIMUM_PERCENT = 75.0;

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

    /**
     * Attendance percentage with one decimal, never above 100.
     * Returns 0 when no class has been held (the page shows "-" in that case).
     */
    public double getPercentage() {
        if (total == 0) {
            return 0;
        }
        double percent = attended * 100.0 / total;
        percent = Math.round(percent * 10) / 10.0; // one decimal place
        return Math.min(percent, 100.0);           // cap at 100%
    }

    /**
     * AT-38: true when attendance is below 75%.
     * Uses the same rounded value that is shown on the page, so the page never
     * shows "75.0%" together with a warning. Exactly 75% is fine.
     * A subject with no classes yet is never flagged.
     */
    public boolean isBelowLimit() {
        return total > 0 && getPercentage() < MINIMUM_PERCENT;
    }
}
