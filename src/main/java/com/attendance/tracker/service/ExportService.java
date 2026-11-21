package com.attendance.tracker.service;

import com.attendance.tracker.model.AttendanceRecord;
import com.attendance.tracker.repository.AttendanceRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * AT-42: builds the attendance CSV file (one line per attendance record).
 */
@Service
public class ExportService {

    private final AttendanceRepository attendance;

    public ExportService(AttendanceRepository attendance) {
        this.attendance = attendance;
    }

    /** The whole CSV file as text: a header line, then one line per record. */
    public String attendanceCsv() {
        StringBuilder csv = new StringBuilder();
        csv.append("Class,Roll No,Student,Subject,Date,Status\n");

        List<AttendanceRecord> records = attendance.findAllForExport();
        for (AttendanceRecord r : records) {
            csv.append(cell(r.getSubject().getClassRoom().getName())).append(',')
               .append(cell(r.getStudent().getRollNo())).append(',')
               .append(cell(r.getStudent().getUser().getFullName())).append(',')
               .append(cell(r.getSubject().getName())).append(',')
               .append(r.getAttendanceDate()).append(',')   // always yyyy-mm-dd
               .append(r.getStatus()).append('\n');
        }
        return csv.toString();
    }

    /** How many records the CSV will contain (shown on the export page). */
    public long recordCount() {
        return attendance.count();
    }

    /**
     * Makes one text value safe for a CSV cell:
     *  - a value with a comma, quote or line break is wrapped in "quotes" (quotes inside are doubled)
     *  - a value starting with = + - or @ gets a ' in front, so Excel shows it as text
     *    instead of running it as a formula
     */
    private String cell(String value) {
        if (value == null) {
            return "";
        }
        if (!value.isEmpty() && "=+-@".indexOf(value.charAt(0)) >= 0) {
            value = "'" + value;
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            value = "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
