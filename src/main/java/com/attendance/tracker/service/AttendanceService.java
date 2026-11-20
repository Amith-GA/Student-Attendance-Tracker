package com.attendance.tracker.service;

import com.attendance.tracker.model.AppUser;
import com.attendance.tracker.model.AttendanceRecord;
import com.attendance.tracker.model.AttendanceStatus;
import com.attendance.tracker.model.Student;
import com.attendance.tracker.model.Subject;
import com.attendance.tracker.repository.AttendanceRepository;
import com.attendance.tracker.repository.StudentRepository;
import com.attendance.tracker.repository.SubjectRepository;
import com.attendance.tracker.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * AT-18: a teacher marks attendance for one subject on one date.
 * Throws IllegalArgumentException with a readable message when something is wrong;
 * the controller shows that message on the page.
 */
@Service
public class AttendanceService {

    private final AttendanceRepository attendance;
    private final SubjectRepository subjects;
    private final StudentRepository students;
    private final UserRepository users;

    public AttendanceService(AttendanceRepository attendance, SubjectRepository subjects,
                             StudentRepository students, UserRepository users) {
        this.attendance = attendance;
        this.subjects = subjects;
        this.students = students;
        this.users = users;
    }

    /**
     * Finds the subject and checks that this teacher is allowed to use it.
     * A teacher can only mark attendance for subjects the admin assigned to them.
     */
    public Subject findSubjectOfTeacher(String teacherUsername, Long subjectId) {
        AppUser teacher = users.findByUsername(teacherUsername)
                .orElseThrow(() -> new IllegalArgumentException("Teacher not found."));
        Subject subject = (subjectId == null) ? null : subjects.findById(subjectId).orElse(null);
        if (subject == null) {
            throw new IllegalArgumentException("Please choose a subject.");
        }
        if (subject.getTeacher() == null || !subject.getTeacher().getId().equals(teacher.getId())) {
            throw new IllegalArgumentException("You are not assigned to this subject.");
        }
        return subject;
    }

    /** Checks the date: it must be given and cannot be in the future. */
    public void checkDate(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("Please choose a date.");
        }
        if (date.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("You cannot mark attendance for a future date.");
        }
    }

    /**
     * Saves one record per student of the subject's class.
     * statusByStudentId maps a student's id to the chosen status.
     * @Transactional: either every student is saved, or none are.
     * @return the number of records saved
     */
    @Transactional
    public int markAttendance(String teacherUsername, Long subjectId, LocalDate date,
                              Map<Long, AttendanceStatus> statusByStudentId) {
        Subject subject = findSubjectOfTeacher(teacherUsername, subjectId);
        checkDate(date);
        AppUser teacher = users.findByUsername(teacherUsername).get();

        if (attendance.existsBySubjectIdAndAttendanceDate(subjectId, date)) {
            throw new IllegalArgumentException("Attendance for " + subject.getName() + " on "
                    + date + " is already saved.");
        }
        List<Student> classStudents = students.findByClassRoomIdOrderByRollNoAsc(subject.getClassRoom().getId());
        if (classStudents.isEmpty()) {
            throw new IllegalArgumentException("This class has no students yet.");
        }

        // every student of the class needs a status
        List<AttendanceRecord> records = new ArrayList<>();
        for (Student student : classStudents) {
            AttendanceStatus status = statusByStudentId.get(student.getId());
            if (status == null) {
                throw new IllegalArgumentException(
                        "Please choose Present, Absent or Late for " + student.getUser().getFullName() + ".");
            }
            records.add(new AttendanceRecord(student, subject, date, status, teacher));
        }
        attendance.saveAll(records);
        return records.size();
    }

    /**
     * AT-23: saved attendance may be edited only if EVERY record in it was created today.
     * Records from an earlier day are read-only.
     */
    public boolean isEditable(List<AttendanceRecord> saved) {
        if (saved.isEmpty()) {
            return false;
        }
        for (AttendanceRecord record : saved) {
            if (!record.isCreatedToday()) {
                return false;
            }
        }
        return true;
    }

    /**
     * AT-23: change the statuses of attendance that was saved earlier today.
     * Same all-or-nothing rule as saving: if anything is wrong, nothing is changed.
     * @return the number of records that were checked and updated
     */
    @Transactional
    public int updateAttendance(String teacherUsername, Long subjectId, LocalDate date,
                                Map<Long, AttendanceStatus> statusByStudentId) {
        findSubjectOfTeacher(teacherUsername, subjectId);
        checkDate(date);

        List<AttendanceRecord> saved =
                attendance.findBySubjectIdAndAttendanceDateOrderByStudentRollNoAsc(subjectId, date);
        if (saved.isEmpty()) {
            throw new IllegalArgumentException("No attendance has been saved for this date yet.");
        }
        if (!isEditable(saved)) {
            throw new IllegalArgumentException(
                    "This attendance was saved on an earlier day, so it can no longer be edited.");
        }

        // check everything first, then change, so a mistake never leaves a half-edited sheet
        for (AttendanceRecord record : saved) {
            if (statusByStudentId.get(record.getStudent().getId()) == null) {
                throw new IllegalArgumentException("Please choose Present, Absent or Late for "
                        + record.getStudent().getUser().getFullName() + ".");
            }
        }
        for (AttendanceRecord record : saved) {
            // the record is a managed object, so this change is saved when the method ends
            record.setStatus(statusByStudentId.get(record.getStudent().getId()));
        }
        return saved.size();
    }
}
