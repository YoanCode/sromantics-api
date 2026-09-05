package com.sromantics.sromantics_api.repository;

import com.sromantics.sromantics_api.entity.MakeUpCredit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MakeUpCreditRepository extends JpaRepository<MakeUpCredit, String> {
    Optional<MakeUpCredit> findBySourceAttendanceId(String sourceAttendanceId);
    boolean existsByUsedAttendanceId(String attendanceId);
}
