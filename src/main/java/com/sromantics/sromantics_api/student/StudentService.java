package com.sromantics.sromantics_api.student;

import com.sromantics.sromantics_api.dto.StudentDto;
import com.sromantics.sromantics_api.entity.Student;
import com.sromantics.sromantics_api.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;

    public List<StudentDto> findAll() {
        return studentRepository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    public StudentDto findById(String id) {
        return studentRepository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    public StudentDto create(StudentDto dto) {
        dto.setId(UUID.randomUUID().toString());
        return toDto(studentRepository.save(toEntity(dto)));
    }

    public StudentDto update(String id, StudentDto dto) {
        if (!studentRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        dto.setId(id);
        return toDto(studentRepository.save(toEntity(dto)));
    }

    public void delete(String id) {
        if (!studentRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        studentRepository.deleteById(id);
    }

    private StudentDto toDto(Student s) {
        return new StudentDto(s.getId(), s.getParentId(), s.getName(),
                s.getGender().name(), s.getSchoolName(), s.getGrade(),
                s.getNote(), s.getStatus().name());
    }

    private Student toEntity(StudentDto dto) {
        return new Student(dto.getId(), dto.getParentId(), dto.getName(),
                Student.Gender.valueOf(dto.getGender()),
                dto.getSchoolName(), dto.getGrade(), dto.getNote(),
                Student.Status.valueOf(dto.getStatus()));
    }
}
