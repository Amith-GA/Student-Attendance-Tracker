package com.attendance.tracker.repository;

import com.attendance.tracker.model.ClassRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClassRoomRepository extends JpaRepository<ClassRoom, Long> {

    // Spring Data builds these queries from the method names
    boolean existsByNameIgnoreCase(String name);

    List<ClassRoom> findAllByOrderByNameAsc();
}
