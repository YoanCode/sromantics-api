package com.sromantics.sromantics_api.enrollment;

import com.sromantics.sromantics_api.entity.Enrollment;
import com.sromantics.sromantics_api.entity.StudentCourse;
import com.sromantics.sromantics_api.repository.EnrollmentRepository;
import com.sromantics.sromantics_api.repository.StudentCourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/enrollments")
@RequiredArgsConstructor
public class EnrollmentController {

    private final EnrollmentRepository repository;
    private final StudentCourseRepository studentCourseRepository;

    @GetMapping
    public List<Enrollment> list() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public Enrollment get(@PathVariable String id) {
        return repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Enrollment create(@RequestBody EnrollmentRequest request) {
        StudentCourse studentCourse = findStudentCourse(request.getStudentCourseId());
        Enrollment enrollment = new Enrollment(UUID.randomUUID().toString(),
                request.getStartedAt(), toEnrollmentPaymentStatus(studentCourse),
                studentCourse.getPurchasedLessons(), studentCourse.getUsedLessons(),
                studentCourse.getRemainingLessons(), studentCourse.getStudentId(),
                request.getStudentCourseId(), request.getClassId(), request.getStartedAt(),
                request.getEndedAt(), request.getStatus());
        return repository.save(enrollment);
    }

    @PutMapping("/{id}")
    public Enrollment update(@PathVariable String id, @RequestBody EnrollmentRequest request) {
        Enrollment enrollment = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        StudentCourse studentCourse = findStudentCourse(request.getStudentCourseId());
        enrollment.setStudentCourseId(request.getStudentCourseId());
        enrollment.setStudentId(studentCourse.getStudentId());
        enrollment.setClassId(request.getClassId());
        enrollment.setStartedAt(request.getStartedAt());
        enrollment.setEndedAt(request.getEndedAt());
        enrollment.setStatus(request.getStatus());
        enrollment.setEnrolledAt(request.getStartedAt());
        enrollment.setPaymentStatus(toEnrollmentPaymentStatus(studentCourse));
        enrollment.setPurchasedLessons(studentCourse.getPurchasedLessons());
        enrollment.setUsedLessons(studentCourse.getUsedLessons());
        enrollment.setRemainingLessons(studentCourse.getRemainingLessons());
        return repository.save(enrollment);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        repository.deleteById(id);
    }

    private StudentCourse findStudentCourse(String id) {
        return studentCourseRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Student course not found"));
    }

    private Enrollment.PaymentStatus toEnrollmentPaymentStatus(StudentCourse studentCourse) {
        return Enrollment.PaymentStatus.valueOf(studentCourse.getPaymentStatus().name());
    }
}
