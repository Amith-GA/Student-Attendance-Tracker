package com.attendance.tracker.service;

/**
 * One row of a student's attendance table: how many classes of ONE subject
 * the student attended, out of how many were held.
 * "Attended" means Present or Late.
 */
public class SubjectAttendance {

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
}
