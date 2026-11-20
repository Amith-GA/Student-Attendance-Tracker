package com.attendance.tracker.repository;

import com.attendance.tracker.model.Subject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubjectRepository extends JpaRepository<Subject, Long> {

    // Stops the same subject name being added twice to one class
    boolean existsByNameIgnoreCaseAndClassRoomId(String name, Long classRoomId);

    // Same check when editing, ignoring the subject being edited
    boolean existsByNameIgnoreCaseAndClassRoomIdAndIdNot(String name, Long classRoomId, Long id);

    // Used before deleting a class (a class with subjects cannot be deleted)
    boolean existsByClassRoomId(Long classRoomId);

    // Used when deleting a teacher: their subjects become "not assigned"
    List<Subject> findByTeacherId(Long teacherId);

    // The subjects a teacher may mark attendance for
    List<Subject> findByTeacherIdOrderByClassRoomNameAscNameAsc(Long teacherId);

    // Sorted by class name, then subject name
    List<Subject> findAllByOrderByClassRoomNameAscNameAsc();
}
