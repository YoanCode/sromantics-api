package com.sromantics.sromantics_api.repository;

import com.sromantics.sromantics_api.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<Attendance, String> {
    Optional<Attendance> findByEnrollmentIdAndAttendanceDate(String enrollmentId, String attendanceDate);
    boolean existsByStudentCourseId(String studentCourseId);
    boolean existsByEnrollmentId(String enrollmentId);
}
