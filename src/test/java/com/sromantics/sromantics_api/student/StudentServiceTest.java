package com.sromantics.sromantics_api.student;

import com.sromantics.sromantics_api.dto.StudentDto;
import com.sromantics.sromantics_api.entity.Student;
import com.sromantics.sromantics_api.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    @Mock
    private StudentRepository studentRepository;

    @InjectMocks
    private StudentService studentService;

    private Student student;
    private StudentDto studentDto;

    @BeforeEach
    void setUp() {
        student = new Student("s_001", "p_001", "王小智",
                Student.Gender.male, "東山國中", "國二", null, Student.Status.active);
        studentDto = new StudentDto("s_001", "p_001", "王小智",
                "male", "東山國中", "國二", null, "active");
    }

    @Test
    void findAll_returnsAllStudentsAsDtos() {
        when(studentRepository.findAll()).thenReturn(List.of(student));

        List<StudentDto> result = studentService.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo("s_001");
        assertThat(result.get(0).getName()).isEqualTo("王小智");
        assertThat(result.get(0).getGender()).isEqualTo("male");
        assertThat(result.get(0).getStatus()).isEqualTo("active");
    }

    @Test
    void findAll_returnsEmptyListWhenNoStudents() {
        when(studentRepository.findAll()).thenReturn(List.of());

        List<StudentDto> result = studentService.findAll();

        assertThat(result).isEmpty();
    }

    @Test
    void findById_returnsStudentDtoWhenFound() {
        when(studentRepository.findById("s_001")).thenReturn(Optional.of(student));

        StudentDto result = studentService.findById("s_001");

        assertThat(result.getId()).isEqualTo("s_001");
        assertThat(result.getParentId()).isEqualTo("p_001");
    }

    @Test
    void findById_throws404WhenNotFound() {
        when(studentRepository.findById("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studentService.findById("unknown"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }

    @Test
    void create_savesStudentWithGeneratedIdAndReturnsDto() {
        StudentDto input = new StudentDto(null, "p_001", "新學生",
                "female", "測試國小", "小五", null, "active");
        when(studentRepository.save(any(Student.class))).thenAnswer(inv -> {
            Student s = inv.getArgument(0);
            return new Student(s.getId(), s.getParentId(), s.getName(),
                    s.getGender(), s.getSchoolName(), s.getGrade(), s.getNote(), s.getStatus());
        });

        StudentDto result = studentService.create(input);

        assertThat(result.getId()).isNotNull().isNotEmpty();
        assertThat(result.getName()).isEqualTo("新學生");
        verify(studentRepository).save(any(Student.class));
    }

    @Test
    void update_updatesStudentWhenExists() {
        when(studentRepository.existsById("s_001")).thenReturn(true);
        when(studentRepository.save(any(Student.class))).thenReturn(student);

        StudentDto result = studentService.update("s_001", studentDto);

        assertThat(result.getId()).isEqualTo("s_001");
        verify(studentRepository).save(any(Student.class));
    }

    @Test
    void update_throws404WhenNotFound() {
        when(studentRepository.existsById("unknown")).thenReturn(false);

        assertThatThrownBy(() -> studentService.update("unknown", studentDto))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");

        verify(studentRepository, never()).save(any());
    }

    @Test
    void delete_deletesStudentWhenExists() {
        when(studentRepository.existsById("s_001")).thenReturn(true);

        studentService.delete("s_001");

        verify(studentRepository).deleteById("s_001");
    }

    @Test
    void delete_throws404WhenNotFound() {
        when(studentRepository.existsById("unknown")).thenReturn(false);

        assertThatThrownBy(() -> studentService.delete("unknown"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");

        verify(studentRepository, never()).deleteById(any());
    }

    @Test
    void findAll_mapsNoteCorrectlyWhenPresent() {
        Student studentWithNote = new Student("s_002", "p_001", "王小美",
                Student.Gender.female, "東山國小", "小六", "備註內容", Student.Status.active);
        when(studentRepository.findAll()).thenReturn(List.of(studentWithNote));

        List<StudentDto> result = studentService.findAll();

        assertThat(result.get(0).getNote()).isEqualTo("備註內容");
    }
}
