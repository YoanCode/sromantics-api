package com.sromantics.sromantics_api.student;

import com.sromantics.sromantics_api.dto.StudentDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentControllerTest {

    @Mock
    private StudentService studentService;

    @InjectMocks
    private StudentController studentController;

    private StudentDto studentDto;

    @BeforeEach
    void setUp() {
        studentDto = new StudentDto("s_001", "p_001", "王小智",
                "male", "東山國中", "國二", null, "active");
    }

    @Test
    void list_delegatesToServiceAndReturnsResult() {
        when(studentService.findAll()).thenReturn(List.of(studentDto));

        List<StudentDto> result = studentController.list();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo("s_001");
        verify(studentService).findAll();
    }

    @Test
    void get_delegatesToServiceWithCorrectId() {
        when(studentService.findById("s_001")).thenReturn(studentDto);

        StudentDto result = studentController.get("s_001");

        assertThat(result.getId()).isEqualTo("s_001");
        verify(studentService).findById("s_001");
    }

    @Test
    void get_propagates404FromService() {
        when(studentService.findById("unknown"))
                .thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> studentController.get("unknown"))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void create_delegatesToServiceAndReturnsCreatedDto() {
        StudentDto input = new StudentDto(null, "p_001", "新學生",
                "female", "測試國小", "小五", null, "active");
        StudentDto created = new StudentDto("uuid-generated", "p_001", "新學生",
                "female", "測試國小", "小五", null, "active");
        when(studentService.create(any(StudentDto.class))).thenReturn(created);

        StudentDto result = studentController.create(input);

        assertThat(result.getId()).isEqualTo("uuid-generated");
        verify(studentService).create(input);
    }

    @Test
    void update_delegatesToServiceWithCorrectIdAndBody() {
        when(studentService.update(eq("s_001"), any(StudentDto.class))).thenReturn(studentDto);

        StudentDto result = studentController.update("s_001", studentDto);

        assertThat(result.getId()).isEqualTo("s_001");
        verify(studentService).update("s_001", studentDto);
    }

    @Test
    void update_propagates404FromService() {
        when(studentService.update(eq("unknown"), any(StudentDto.class)))
                .thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> studentController.update("unknown", studentDto))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void delete_delegatesToServiceWithCorrectId() {
        doNothing().when(studentService).delete("s_001");

        studentController.delete("s_001");

        verify(studentService).delete("s_001");
    }

    @Test
    void delete_propagates404FromService() {
        doThrow(new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND))
                .when(studentService).delete("unknown");

        assertThatThrownBy(() -> studentController.delete("unknown"))
                .isInstanceOf(ResponseStatusException.class);
    }
}

