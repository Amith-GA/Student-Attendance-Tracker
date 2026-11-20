package com.attendance.tracker.repository;

import com.attendance.tracker.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudentRepository extends JpaRepository<Student, Long> {

    // Stops the same roll number being used twice in one class
    boolean existsByRollNoIgnoreCaseAndClassRoomId(String rollNo, Long classRoomId);

    // Sorted by class name, then roll number
    List<Student> findAllByOrderByClassRoomNameAscRollNoAsc();
}
