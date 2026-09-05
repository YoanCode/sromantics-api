package com.sromantics.sromantics_api.studentcourse;

import com.sromantics.sromantics_api.entity.StudentCourse;
import com.sromantics.sromantics_api.repository.StudentCourseRepository;
import com.sromantics.sromantics_api.repository.AttendanceRepository;
import com.sromantics.sromantics_api.repository.EnrollmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/student-courses")
@RequiredArgsConstructor
public class StudentCourseController {

    private final StudentCourseRepository repository;
    private final AttendanceRepository attendanceRepository;
    private final EnrollmentRepository enrollmentRepository;

    @GetMapping
    public List<StudentCourse> list() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public StudentCourse get(@PathVariable String id) {
        return repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StudentCourse create(@RequestBody StudentCourse studentCourse) {
        studentCourse.setId(UUID.randomUUID().toString());
        if (studentCourse.getEnrolledAt() == null || studentCourse.getEnrolledAt().isBlank()) {
            studentCourse.setEnrolledAt(LocalDate.now().toString());
        }
        studentCourse.setUsedLessons(Math.max(0, studentCourse.getUsedLessons()));
        studentCourse.setRemainingLessons(Math.max(0,
                studentCourse.getPurchasedLessons() - studentCourse.getUsedLessons()));
        return repository.save(studentCourse);
    }

    @PutMapping("/{id}")
    public StudentCourse update(@PathVariable String id, @RequestBody StudentCourse studentCourse) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        studentCourse.setId(id);
        return repository.save(studentCourse);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (attendanceRepository.existsByStudentCourseId(id)
                || enrollmentRepository.existsByStudentCourseId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Student course is referenced by enrollments or attendance records");
        }
        repository.deleteById(id);
    }
}
