package com.sromantics.sromantics_api.studentcourse;

import com.sromantics.sromantics_api.entity.StudentCourse;
import com.sromantics.sromantics_api.repository.StudentCourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/student-courses")
@RequiredArgsConstructor
public class StudentCourseController {

    private final StudentCourseRepository repository;

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
        repository.deleteById(id);
    }
}
