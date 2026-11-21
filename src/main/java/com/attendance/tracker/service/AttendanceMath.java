package com.attendance.tracker.service;

/**
 * The attendance formulas, kept in ONE place so that every page
 * (student page, teacher report, CSV) calculates in exactly the same way.
 * "Attended" means Present or Late.
 */
public final class AttendanceMath {

    /** Students must have at least this much attendance. */
    public static final double MINIMUM_PERCENT = 75.0;

    private AttendanceMath() { }

    /** Percentage with one decimal, never above 100. Returns 0 when no class was held. */
    public static double percentage(int attended, int total) {
        if (total == 0) {
            return 0;
        }
        double percent = attended * 100.0 / total;
        percent = Math.round(percent * 10) / 10.0; // one decimal place
        return Math.min(percent, 100.0);           // cap at 100%
    }

    /**
     * True when attendance is below 75%. Uses the same rounded value that is shown on the page,
     * so a page never shows "75.0%" together with a warning. Exactly 75% is fine.
     * When no class has been held yet nobody is flagged.
     */
    public static boolean isBelowLimit(int attended, int total) {
        return total > 0 && percentage(attended, total) < MINIMUM_PERCENT;
    }

    /** From this percentage upwards attendance is "good"; between 75 and this it is "borderline". */
    public static final double GOOD_PERCENT = 80.0;

    /**
     * AT-64: a label for the colour of a badge or bar:
     * "none" (no class held yet), "low" (below 75), "borderline" (75 up to 80) or "good" (80 and up).
     * "low" is exactly the AT-38 warning rule.
     */
    public static String level(int attended, int total) {
        if (total == 0) {
            return "none";
        }
        double percent = percentage(attended, total);
        if (percent < MINIMUM_PERCENT) {
            return "low";
        }
        return percent < GOOD_PERCENT ? "borderline" : "good";
    }

    /**
     * AT-64: how many classes in a row the student must attend to get back to 75%.
     * 0 when attendance is already fine. Every extra class adds 1 to both attended and total.
     * It uses isBelowLimit, so it always agrees with the warning shown on the page.
     */
    public static int classesNeeded(int attended, int total) {
        int extra = 0;
        while (isBelowLimit(attended + extra, total + extra)) {
            extra++;
        }
        return extra;
    }
}
