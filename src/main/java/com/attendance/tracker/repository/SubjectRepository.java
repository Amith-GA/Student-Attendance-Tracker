package com.attendance.tracker.repository;

import com.attendance.tracker.model.Subject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubjectRepository extends JpaRepository<Subject, Long> {

    // Stops the same subject name being added twice to one class
    boolean existsByNameIgnoreCaseAndClassRoomId(String name, Long classRoomId);

    // Sorted by class name, then subject name
    List<Subject> findAllByOrderByClassRoomNameAscNameAsc();
}
