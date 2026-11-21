package com.attendance.tracker.service;

import com.attendance.tracker.model.AttendanceRecord;
import com.attendance.tracker.model.Student;
import com.attendance.tracker.model.Subject;
import com.attendance.tracker.repository.AttendanceRepository;
import com.attendance.tracker.repository.StudentRepository;
import com.attendance.tracker.repository.SubjectRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Works out attendance numbers for the reports.
 * Rule used everywhere: Present and Late both count as "attended".
 */
@Service
public class ReportService {

    private final AttendanceRepository attendance;
    private final SubjectRepository subjects;
    private final StudentRepository students;

    public ReportService(AttendanceRepository attendance, SubjectRepository subjects,
                         StudentRepository students) {
        this.attendance = attendance;
        this.subjects = subjects;
        this.students = students;
    }

    /**
     * AT-34: one row for EVERY student of the subject's class, with attended / total / percentage
     * for this subject. Students without any record yet show total 0.
     * AT-46: only classes held between "from" and "to" (both included) are counted.
     * Either date may be null, which means "no limit on that side".
     */
    public List<StudentAttendance> classReport(Subject subject, LocalDate from, LocalDate to) {
        // deactivated students are not listed (their records are kept in the database)
        List<Student> classStudents =
                students.findByClassRoomIdAndUserActiveTrueOrderByRollNoAsc(subject.getClassRoom().getId());
        List<AttendanceRecord> records = recordsInRange(subject, from, to);

        List<StudentAttendance> rows = new ArrayList<>();
        for (Student student : classStudents) {
            int present = 0;
            int late = 0;
            int absent = 0;
            for (AttendanceRecord record : records) {
                if (record.getStudent().getId().equals(student.getId())) {
                    switch (record.getStatus()) {
                        case PRESENT -> present++;
                        case LATE -> late++;
                        case ABSENT -> absent++;
                    }
                }
            }
            rows.add(new StudentAttendance(student.getRollNo(), student.getUser().getFullName(),
                    present, late, absent));
        }
        return rows;
    }

    /**
     * AT-70: narrows the report down and sorts it.
     *  - search: keeps students whose name or roll number contains the text (ignores upper/lower case)
     *  - onlyBelowLimit: keeps only students below 75%
     *  - sort: "roll" (default), "name", "lowest" (lowest attendance first) or "highest"
     * Students with no class held yet always go to the end when sorting by attendance.
     */
    public List<StudentAttendance> filterAndSort(List<StudentAttendance> rows, String search,
                                                 boolean onlyBelowLimit, String sort) {
        String needle = (search == null) ? "" : search.trim().toLowerCase();
        List<StudentAttendance> result = new ArrayList<>();
        for (StudentAttendance row : rows) {
            boolean matches = needle.isEmpty()
                    || row.getRollNo().toLowerCase().contains(needle)
                    || row.getStudentName().toLowerCase().contains(needle);
            if (matches && (!onlyBelowLimit || row.isBelowLimit())) {
                result.add(row);
            }
        }

        // false sorts before true, so "no classes yet" (true) goes last
        Comparator<StudentAttendance> noClassesLast = Comparator.comparing(StudentAttendance::isNoClassesYet);
        String how = (sort == null) ? "roll" : sort;
        switch (how) {
            case "name" -> result.sort(Comparator.comparing(r -> r.getStudentName().toLowerCase()));
            case "lowest" -> result.sort(noClassesLast.thenComparingDouble(StudentAttendance::getPercentage));
            case "highest" -> result.sort(noClassesLast.thenComparing(
                    Comparator.comparingDouble(StudentAttendance::getPercentage).reversed()));
            default -> { } // "roll": the report already comes in roll number order
        }
        return result;
    }

    /** AT-70: the average attendance of a whole report (all attended / all classes), 0 when empty. */
    public double classAverage(List<StudentAttendance> rows) {
        int attended = 0;
        int total = 0;
        for (StudentAttendance row : rows) {
            attended += row.getAttended();
            total += row.getTotal();
        }
        return AttendanceMath.percentage(attended, total);
    }

    /**
     * AT-70: one line per subject of the teacher for the dashboard:
     * number of (active) students, classes held so far, and whether today's attendance is taken.
     */
    public List<SubjectOverview> overviewForTeacher(Long teacherId, LocalDate today) {
        List<SubjectOverview> result = new ArrayList<>();
        for (Subject subject : subjects.findByTeacherIdOrderByClassRoomNameAscNameAsc(teacherId)) {
            int studentCount = (int) students.countByClassRoomIdAndUserActiveTrue(subject.getClassRoom().getId());
            int held = classesHeld(subject, null, null);
            boolean taken = attendance.existsBySubjectIdAndAttendanceDate(subject.getId(), today);
            result.add(new SubjectOverview(subject, studentCount, held, taken));
        }
        return result;
    }

    /** How many different days attendance was taken for this subject (within the date range). */
    public int classesHeld(Subject subject, LocalDate from, LocalDate to) {
        return (int) recordsInRange(subject, from, to).stream()
                .map(AttendanceRecord::getAttendanceDate)
                .distinct()
                .count();
    }

    /** AT-46: the subject's records whose date is between from and to (null = no limit). */
    private List<AttendanceRecord> recordsInRange(Subject subject, LocalDate from, LocalDate to) {
        List<AttendanceRecord> result = new ArrayList<>();
        for (AttendanceRecord record : attendance.findBySubjectId(subject.getId())) {
            if (isInRange(record.getAttendanceDate(), from, to)) {
                result.add(record);
            }
        }
        return result;
    }

    /** AT-30: one row per subject of the student's class, over all dates. */
    public List<SubjectAttendance> subjectWiseAttendance(Student student) {
        return subjectWiseAttendance(student, null, null, null);
    }

    /**
     * AT-30 / AT-64: one row per subject of the student's class with the number of
     * Present, Late and Absent classes.
     * subjectId (optional) keeps just that subject; from / to (optional) limit the dates.
     */
    public List<SubjectAttendance> subjectWiseAttendance(Student student, Long subjectId,
                                                         LocalDate from, LocalDate to) {
        checkDateRange(from, to);
        List<Subject> classSubjects = subjects.findByClassRoomIdOrderByNameAsc(student.getClassRoom().getId());
        if (subjectId != null && classSubjects.stream().noneMatch(s -> s.getId().equals(subjectId))) {
            throw new IllegalArgumentException("That subject does not belong to your class.");
        }
        List<AttendanceRecord> records = attendance.findByStudentId(student.getId());

        List<SubjectAttendance> rows = new ArrayList<>();
        for (Subject subject : classSubjects) {
            if (subjectId != null && !subject.getId().equals(subjectId)) {
                continue;
            }
            int present = 0;
            int late = 0;
            int absent = 0;
            for (AttendanceRecord record : records) {
                if (record.getSubject().getId().equals(subject.getId())
                        && isInRange(record.getAttendanceDate(), from, to)) {
                    switch (record.getStatus()) {
                        case PRESENT -> present++;
                        case LATE -> late++;
                        case ABSENT -> absent++;
                    }
                }
            }
            String teacher = (subject.getTeacher() == null) ? "Not assigned" : subject.getTeacher().getFullName();
            rows.add(new SubjectAttendance(subject.getName(), teacher, present, late, absent));
        }
        return rows;
    }

    /** AT-64: the student's individual records (newest first), with the same optional filters. */
    public List<AttendanceRecord> history(Student student, Long subjectId, LocalDate from, LocalDate to) {
        checkDateRange(from, to);
        List<AttendanceRecord> result = new ArrayList<>();
        for (AttendanceRecord record : attendance.findByStudentId(student.getId())) {
            boolean subjectOk = (subjectId == null) || record.getSubject().getId().equals(subjectId);
            if (subjectOk && isInRange(record.getAttendanceDate(), from, to)) {
                result.add(record);
            }
        }
        result.sort(Comparator.comparing(AttendanceRecord::getAttendanceDate).reversed()
                .thenComparing(r -> r.getSubject().getName()));
        return result;
    }

    /** The "from" date must not be after the "to" date. */
    private void checkDateRange(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("The 'from' date cannot be after the 'to' date.");
        }
    }

    /** true when the date is between from and to (both included); null means "no limit". */
    private boolean isInRange(LocalDate date, LocalDate from, LocalDate to) {
        boolean afterStart = (from == null) || !date.isBefore(from);
        boolean beforeEnd = (to == null) || !date.isAfter(to);
        return afterStart && beforeEnd;
    }
}
