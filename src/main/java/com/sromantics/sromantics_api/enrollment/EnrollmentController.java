package com.sromantics.sromantics_api.enrollment;

import com.sromantics.sromantics_api.entity.Enrollment;
import com.sromantics.sromantics_api.repository.EnrollmentRepository;
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
    public Enrollment create(@RequestBody Enrollment enrollment) {
        enrollment.setId(UUID.randomUUID().toString());
        return repository.save(enrollment);
    }

    @PutMapping("/{id}")
    public Enrollment update(@PathVariable String id, @RequestBody Enrollment enrollment) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        enrollment.setId(id);
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
}
