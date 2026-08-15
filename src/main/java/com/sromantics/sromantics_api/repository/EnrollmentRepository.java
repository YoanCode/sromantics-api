package com.sromantics.sromantics_api.repository;

import com.sromantics.sromantics_api.entity.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnrollmentRepository extends JpaRepository<Enrollment, String> {}
