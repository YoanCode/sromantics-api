package com.sromantics.sromantics_api.repository;

import com.sromantics.sromantics_api.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentRepository extends JpaRepository<Student, String> {}
