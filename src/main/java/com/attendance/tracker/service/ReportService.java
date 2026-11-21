package com.attendance.tracker.service;

import com.attendance.tracker.model.AttendanceRecord;
import com.attendance.tracker.model.AttendanceStatus;
import com.attendance.tracker.model.Student;
import com.attendance.tracker.model.Subject;
import com.attendance.tracker.repository.AttendanceRepository;
import com.attendance.tracker.repository.StudentRepository;
import com.attendance.tracker.repository.SubjectRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
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
        List<Student> classStudents = students.findByClassRoomIdOrderByRollNoAsc(subject.getClassRoom().getId());
        List<AttendanceRecord> records = recordsInRange(subject, from, to);

        List<StudentAttendance> rows = new ArrayList<>();
        for (Student student : classStudents) {
            int total = 0;
            int attended = 0;
            for (AttendanceRecord record : records) {
                if (record.getStudent().getId().equals(student.getId())) {
                    total++;
                    if (record.getStatus() != AttendanceStatus.ABSENT) {
                        attended++; // PRESENT or LATE
                    }
                }
            }
            rows.add(new StudentAttendance(student.getRollNo(), student.getUser().getFullName(), attended, total));
        }
        return rows;
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
            LocalDate date = record.getAttendanceDate();
            boolean afterStart = (from == null) || !date.isBefore(from);
            boolean beforeEnd = (to == null) || !date.isAfter(to);
            if (afterStart && beforeEnd) {
                result.add(record);
            }
        }
        return result;
    }

    /** AT-30: one row per subject of the student's class, with attended / total / percentage. */
    public List<SubjectAttendance> subjectWiseAttendance(Student student) {
        List<Subject> classSubjects = subjects.findByClassRoomIdOrderByNameAsc(student.getClassRoom().getId());
        List<AttendanceRecord> records = attendance.findByStudentId(student.getId());

        List<SubjectAttendance> rows = new ArrayList<>();
        for (Subject subject : classSubjects) {
            int total = 0;
            int attended = 0;
            for (AttendanceRecord record : records) {
                if (record.getSubject().getId().equals(subject.getId())) {
                    total++;
                    if (record.getStatus() != AttendanceStatus.ABSENT) {
                        attended++; // PRESENT or LATE
                    }
                }
            }
            rows.add(new SubjectAttendance(subject.getName(), attended, total));
        }
        return rows;
    }
}
