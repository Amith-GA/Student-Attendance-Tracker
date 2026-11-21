package com.attendance.tracker.repository;

import com.attendance.tracker.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {

    // Stops the same roll number being used twice in one class
    boolean existsByRollNoIgnoreCaseAndClassRoomId(String rollNo, Long classRoomId);

    // Same check when editing, ignoring the student being edited
    boolean existsByRollNoIgnoreCaseAndClassRoomIdAndIdNot(String rollNo, Long classRoomId, Long id);

    // Used before deleting a class (a class with students cannot be deleted)
    boolean existsByClassRoomId(Long classRoomId);

    // Does this login account already have a student profile?
    boolean existsByUserId(Long userId);

    // The student profile that belongs to a login account
    Optional<Student> findByUserId(Long userId);

    // All students of one class, used for the attendance sheet
    List<Student> findByClassRoomIdOrderByRollNoAsc(Long classRoomId);

    // Sorted by class name, then roll number
    List<Student> findAllByOrderByClassRoomNameAscRollNoAsc();
}
