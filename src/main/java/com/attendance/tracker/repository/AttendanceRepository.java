package com.attendance.tracker.repository;

import com.attendance.tracker.model.AttendanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

public interface AttendanceRepository extends JpaRepository<AttendanceRecord, Long> {

    // Has attendance already been saved for this subject on this date?
    boolean existsBySubjectIdAndAttendanceDate(Long subjectId, LocalDate attendanceDate);

    // The saved attendance of one subject on one date, in roll number order
    List<AttendanceRecord> findBySubjectIdAndAttendanceDateOrderByStudentRollNoAsc(
            Long subjectId, LocalDate attendanceDate);

    // ---- clean-up used when something is deleted by the admin ----

    @Transactional
    @Modifying
    @Query("delete from AttendanceRecord r where r.student.id = :studentId")
    void deleteByStudentId(@Param("studentId") Long studentId);

    @Transactional
    @Modifying
    @Query("delete from AttendanceRecord r where r.subject.id = :subjectId")
    void deleteBySubjectId(@Param("subjectId") Long subjectId);

    // When a teacher is deleted the records stay; they just lose the "marked by" link
    @Transactional
    @Modifying
    @Query("update AttendanceRecord r set r.markedBy = null where r.markedBy.id = :teacherId")
    void clearMarkedBy(@Param("teacherId") Long teacherId);
}
