package com.attendance.tracker.service;

import com.attendance.tracker.model.Subject;
import com.attendance.tracker.repository.AttendanceRepository;
import com.attendance.tracker.repository.SubjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Deleting a subject must also delete its attendance records
 * (records cannot exist without their subject), so both steps are done together.
 */
@Service
public class SubjectService {

    private final SubjectRepository subjects;
    private final AttendanceRepository attendance;

    public SubjectService(SubjectRepository subjects, AttendanceRepository attendance) {
        this.subjects = subjects;
        this.attendance = attendance;
    }

    @Transactional
    public void deleteSubject(Long subjectId) {
        Subject subject = subjects.findById(subjectId)
                .orElseThrow(() -> new IllegalArgumentException("Subject not found."));
        attendance.deleteBySubjectId(subjectId);
        subjects.delete(subject);
    }
}
