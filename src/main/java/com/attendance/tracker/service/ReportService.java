package com.attendance.tracker.service;

import com.attendance.tracker.model.AttendanceRecord;
import com.attendance.tracker.model.AttendanceStatus;
import com.attendance.tracker.model.Student;
import com.attendance.tracker.model.Subject;
import com.attendance.tracker.repository.AttendanceRepository;
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

    public ReportService(AttendanceRepository attendance, SubjectRepository subjects) {
        this.attendance = attendance;
        this.subjects = subjects;
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
