package com.sromantics.sromantics_api.repository;

import com.sromantics.sromantics_api.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRepository extends JpaRepository<Course, String> {}
