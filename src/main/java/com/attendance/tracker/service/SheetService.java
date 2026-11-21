package com.attendance.tracker.service;

import com.attendance.tracker.model.AttendanceRecord;
import com.attendance.tracker.model.AttendanceStatus;
import com.attendance.tracker.model.Student;
import com.attendance.tracker.model.Subject;
import com.attendance.tracker.repository.StudentRepository;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;

/**
 * AT-75: builds the teacher's attendance sheet as a real Excel (.xlsx) file.
 *
 * One row per student, one column per day a class was held, and a letter in each cell:
 * P = present, L = late, A = absent. At the right: totals and the attendance percentage.
 * The dates are written as TEXT (for example "21 Nov") and the column widths are set,
 * so Excel can never show "####" like it did with the old CSV file.
 */
@Service
public class SheetService {

    private static final DateTimeFormatter SHORT_DATE = DateTimeFormatter.ofPattern("dd MMM", Locale.ENGLISH);
    private static final DateTimeFormatter LONG_DATE = DateTimeFormatter.ofPattern("dd MMM yy", Locale.ENGLISH);
    private static final DateTimeFormatter FULL_DATE = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);

    private final ReportService reports;
    private final StudentRepository students;

    public SheetService(ReportService reports, StudentRepository students) {
        this.reports = reports;
        this.students = students;
    }

    /** The whole .xlsx file for one subject, as bytes. from / to are optional (null = no limit). */
    public byte[] classSheet(Subject subject, LocalDate from, LocalDate to) {
        // deactivated students are left out, the same as on the class report
        List<Student> classStudents =
                students.findByClassRoomIdAndUserActiveTrueOrderByRollNoAsc(subject.getClassRoom().getId());
        List<AttendanceRecord> records = reports.recordsInRange(subject, from, to);

        // the days a class was held, oldest first
        TreeSet<LocalDate> days = new TreeSet<>();
        // student id -> (date -> status)
        Map<Long, Map<LocalDate, AttendanceStatus>> statusOf = new HashMap<>();
        for (AttendanceRecord record : records) {
            days.add(record.getAttendanceDate());
            statusOf.computeIfAbsent(record.getStudent().getId(), id -> new HashMap<>())
                    .put(record.getAttendanceDate(), record.getStatus());
        }
        // if the days cover more than one year, the year is shown too
        boolean severalYears = !days.isEmpty() && days.first().getYear() != days.last().getYear();
        DateTimeFormatter dayFormat = severalYears ? LONG_DATE : SHORT_DATE;

        try (Workbook book = new XSSFWorkbook()) {
            Sheet sheet = book.createSheet("Attendance");
            Styles styles = new Styles(book);

            // ---- title lines ----
            String title = subject.getClassRoom().getName() + " - " + subject.getName();
            text(sheet, 0, 0, title, styles.title);
            String teacher = (subject.getTeacher() == null) ? "-" : subject.getTeacher().getFullName();
            String period = (from == null && to == null) ? "All dates"
                    : (from == null ? "Up to " + to.format(FULL_DATE)
                    : (to == null ? "From " + from.format(FULL_DATE)
                    : from.format(FULL_DATE) + " to " + to.format(FULL_DATE)));
            text(sheet, 1, 0, "Teacher: " + teacher + "   |   Period: " + period
                    + "   |   Classes held: " + days.size()
                    + "   |   Created: " + LocalDate.now().format(FULL_DATE), null);

            // ---- header row ----
            int headerRow = 3;
            Row header = sheet.createRow(headerRow);
            int col = 0;
            cell(header, col++, "Roll no", styles.header);
            cell(header, col++, "Name", styles.header);
            for (LocalDate day : days) {
                cell(header, col++, day.format(dayFormat), styles.header);
            }
            int presentCol = col;
            String[] endHeaders = {"Present", "Late", "Absent", "Total", "Attendance %"};
            for (String endHeader : endHeaders) {
                cell(header, col++, endHeader, styles.header);
            }
            int lastCol = col - 1;

            // ---- one row per student ----
            int rowNumber = headerRow + 1;
            for (Student student : classStudents) {
                Row row = sheet.createRow(rowNumber++);
                Map<LocalDate, AttendanceStatus> mine = statusOf.getOrDefault(student.getId(), Map.of());
                int present = 0;
                int late = 0;
                int absent = 0;

                int c = 0;
                cell(row, c++, student.getRollNo(), styles.plain);
                cell(row, c++, student.getUser().getFullName(), styles.plain);
                for (LocalDate day : days) {
                    AttendanceStatus status = mine.get(day);
                    if (status == null) {
                        cell(row, c++, "-", styles.center);     // no record for this student that day
                    } else if (status == AttendanceStatus.PRESENT) {
                        present++;
                        cell(row, c++, "P", styles.center);
                    } else if (status == AttendanceStatus.LATE) {
                        late++;
                        cell(row, c++, "L", styles.late);
                    } else {
                        absent++;
                        cell(row, c++, "A", styles.absent);
                    }
                }
                int total = present + late + absent;
                number(row, c++, present, styles.center);
                number(row, c++, late, styles.center);
                number(row, c++, absent, styles.center);
                number(row, c++, total, styles.center);
                if (total == 0) {
                    cell(row, c, "-", styles.center);
                } else {
                    // Excel stores a percentage as a fraction: 0.8 with the format 0.0% is shown as 80.0%
                    double fraction = AttendanceMath.percentage(present + late, total) / 100.0;
                    number(row, c, fraction, AttendanceMath.isBelowLimit(present + late, total)
                            ? styles.percentLow : styles.percent);
                }
            }

            // ---- layout: column widths are set by hand, so Excel never shows "####" ----
            sheet.setColumnWidth(0, width(classStudents.stream().map(Student::getRollNo).toList(), "Roll no", 8));
            sheet.setColumnWidth(1, width(classStudents.stream().map(s -> s.getUser().getFullName()).toList(), "Name", 8));
            for (int c = 2; c < presentCol; c++) {
                sheet.setColumnWidth(c, 10 * 256);              // fits "21 Nov" and "21 Nov 26"
            }
            for (int c = presentCol; c <= lastCol; c++) {
                sheet.setColumnWidth(c, (c == lastCol ? 16 : 10) * 256);
            }
            sheet.createFreezePane(2, headerRow + 1);           // names and dates stay visible when scrolling

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            book.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Could not create the Excel file.", e);
        }
    }

    // ---------- small helpers ----------

    private void text(Sheet sheet, int rowIndex, int colIndex, String value, CellStyle style) {
        Row row = sheet.createRow(rowIndex);
        cell(row, colIndex, value, style);
    }

    private void cell(Row row, int colIndex, String value, CellStyle style) {
        Cell cell = row.createCell(colIndex);
        cell.setCellValue(value);
        if (style != null) {
            cell.setCellStyle(style);
        }
    }

    private void number(Row row, int colIndex, double value, CellStyle style) {
        Cell cell = row.createCell(colIndex);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    /** Column width (in Excel units) that fits the longest text, plus some space. */
    private int width(List<String> values, String headerText, int minimum) {
        int longest = Math.max(minimum, headerText.length());
        for (String value : values) {
            if (value != null) {
                longest = Math.max(longest, value.length());
            }
        }
        return Math.min(longest + 3, 60) * 256;
    }

    /** The cell looks used in the sheet, created once per workbook. */
    private static class Styles {
        final CellStyle title;
        final CellStyle header;
        final CellStyle plain;
        final CellStyle center;
        final CellStyle late;
        final CellStyle absent;
        final CellStyle percent;
        final CellStyle percentLow;

        Styles(Workbook book) {
            Font bold = book.createFont();
            bold.setBold(true);
            Font big = book.createFont();
            big.setBold(true);
            big.setFontHeightInPoints((short) 14);

            title = book.createCellStyle();
            title.setFont(big);

            header = border(book.createCellStyle());
            header.setFont(bold);
            header.setAlignment(HorizontalAlignment.CENTER);
            header.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            header.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            plain = border(book.createCellStyle());

            center = border(book.createCellStyle());
            center.setAlignment(HorizontalAlignment.CENTER);

            late = border(book.createCellStyle());
            late.setAlignment(HorizontalAlignment.CENTER);
            late.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
            late.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            absent = border(book.createCellStyle());
            absent.setAlignment(HorizontalAlignment.CENTER);
            absent.setFillForegroundColor(IndexedColors.ROSE.getIndex());
            absent.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // the built-in Excel format "0.0%" shows the fraction 0.75 as 75.0%
            percent = border(book.createCellStyle());
            percent.setAlignment(HorizontalAlignment.CENTER);
            percent.setDataFormat(book.createDataFormat().getFormat("0.0%"));

            percentLow = border(book.createCellStyle());
            percentLow.setAlignment(HorizontalAlignment.CENTER);
            percentLow.setDataFormat(book.createDataFormat().getFormat("0.0%"));
            percentLow.setFillForegroundColor(IndexedColors.ROSE.getIndex());
            percentLow.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            percentLow.setFont(bold);
        }

        private static CellStyle border(CellStyle style) {
            style.setBorderTop(BorderStyle.THIN);
            style.setBorderBottom(BorderStyle.THIN);
            style.setBorderLeft(BorderStyle.THIN);
            style.setBorderRight(BorderStyle.THIN);
            return style;
        }
    }
}
