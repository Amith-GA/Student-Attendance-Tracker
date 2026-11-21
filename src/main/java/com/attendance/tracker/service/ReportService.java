package com.attendance.tracker.service;

import com.attendance.tracker.model.AttendanceRecord;
import com.attendance.tracker.model.AttendanceStatus;
import com.attendance.tracker.model.Student;
import com.attendance.tracker.model.Subject;
import com.attendance.tracker.repository.AttendanceRepository;
import com.attendance.tracker.repository.StudentRepository;
import com.attendance.tracker.repository.SubjectRepository;
import org.springframework.stereotype.Service;

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
     */
    public List<StudentAttendance> classReport(Subject subject) {
        List<Student> classStudents = students.findByClassRoomIdOrderByRollNoAsc(subject.getClassRoom().getId());
        List<AttendanceRecord> records = attendance.findBySubjectId(subject.getId());

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

    /** How many different days attendance was taken for this subject. */
    public int classesHeld(Subject subject) {
        return (int) attendance.findBySubjectId(subject.getId()).stream()
                .map(AttendanceRecord::getAttendanceDate)
                .distinct()
                .count();
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
