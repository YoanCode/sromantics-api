package com.sromantics.sromantics_api.attendance;

import com.sromantics.sromantics_api.entity.Attendance;
import com.sromantics.sromantics_api.entity.Enrollment;
import com.sromantics.sromantics_api.entity.StudentCourse;
import com.sromantics.sromantics_api.entity.Clazz;
import com.sromantics.sromantics_api.repository.AttendanceRepository;
import com.sromantics.sromantics_api.repository.ClazzRepository;
import com.sromantics.sromantics_api.repository.EnrollmentRepository;
import com.sromantics.sromantics_api.repository.StudentCourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.DateTimeException;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final StudentCourseRepository studentCourseRepository;
    private final ClazzRepository clazzRepository;

    @Transactional(readOnly = true)
    public List<Attendance> findAll() {
        return attendanceRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Attendance findById(String id) {
        return attendanceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    public Attendance create(Attendance request) {
        Enrollment enrollment = findEnrollment(request.getEnrollmentId());
        validateAttendanceEligibility(enrollment, request.getAttendanceDate());
        attendanceRepository.findByEnrollmentIdAndAttendanceDate(
                        enrollment.getId(), request.getAttendanceDate())
                .ifPresent(existing -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Attendance already exists for this enrollment and date");
                });

        Attendance attendance = new Attendance();
        attendance.setId(UUID.randomUUID().toString());
        attendance.setEnrollmentId(enrollment.getId());
        attendance.setStudentCourseId(enrollment.getStudentCourseId());
        attendance.setClassId(enrollment.getClassId());
        attendance.setAttendanceDate(request.getAttendanceDate());
        attendance.setStatus(request.getStatus());
        attendance.setNote(request.getNote());
        attendance.setRecordedAt(LocalDateTime.now().toString());
        applyLessonDelta(attendance.getStudentCourseId(), attendance.getStatus(), 1);
        return attendanceRepository.save(attendance);
    }

    public Attendance update(String id, Attendance request) {
        Attendance attendance = findById(id);
        String attendanceDate = request.getAttendanceDate() != null
            ? request.getAttendanceDate()
            : attendance.getAttendanceDate();
        validateAttendanceEligibility(findEnrollment(attendance.getEnrollmentId()), attendanceDate);
        if (request.getAttendanceDate() != null
                && !request.getAttendanceDate().equals(attendance.getAttendanceDate())) {
            attendanceRepository.findByEnrollmentIdAndAttendanceDate(
                            attendance.getEnrollmentId(), request.getAttendanceDate())
                    .ifPresent(existing -> {
                        throw new ResponseStatusException(HttpStatus.CONFLICT,
                                "Attendance already exists for this enrollment and date");
                    });
        }

        if (request.getStatus() != null && request.getStatus() != attendance.getStatus()) {
            applyLessonDelta(attendance.getStudentCourseId(), attendance.getStatus(), -1);
            applyLessonDelta(attendance.getStudentCourseId(), request.getStatus(), 1);
            attendance.setStatus(request.getStatus());
        }
        if (request.getAttendanceDate() != null) attendance.setAttendanceDate(request.getAttendanceDate());
        attendance.setNote(request.getNote());
        return attendanceRepository.save(attendance);
    }

    public void delete(String id) {
        Attendance attendance = findById(id);
        if (studentCourseRepository.existsById(attendance.getStudentCourseId())) {
            applyLessonDelta(attendance.getStudentCourseId(), attendance.getStatus(), -1);
        }
        attendanceRepository.delete(attendance);
    }

    private Enrollment findEnrollment(String id) {
        return enrollmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Enrollment not found"));
    }

    private void validateAttendanceEligibility(Enrollment enrollment, String attendanceDate) {
        if (attendanceDate == null || attendanceDate.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Attendance date is required");
        }

        LocalDate date;
        try {
            date = LocalDate.parse(attendanceDate);
        } catch (DateTimeException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Attendance date must use yyyy-MM-dd");
        }

        if (enrollment.getStatus() == Enrollment.Status.cancelled
                || (enrollment.getStartedAt() != null && attendanceDate.compareTo(enrollment.getStartedAt()) < 0)
                || (enrollment.getEndedAt() != null && attendanceDate.compareTo(enrollment.getEndedAt()) > 0)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Student was not enrolled in this class on the attendance date");
        }

        Clazz clazz = clazzRepository.findById(enrollment.getClassId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Class not found for enrollment"));
        if (date.getDayOfWeek().getValue() != clazz.getDayOfWeek()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Attendance date is not a scheduled class day");
        }
    }

    private void applyLessonDelta(String studentCourseId, Attendance.Status status, int direction) {
        if (!consumesLesson(status)) return;
        StudentCourse studentCourse = studentCourseRepository.findById(studentCourseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Student course not found"));
        int usedLessons = Math.max(0, studentCourse.getUsedLessons() + direction);
        studentCourse.setUsedLessons(usedLessons);
        studentCourse.setRemainingLessons(
                Math.max(0, studentCourse.getPurchasedLessons() - usedLessons));
        studentCourseRepository.save(studentCourse);
    }

    private boolean consumesLesson(Attendance.Status status) {
        return status == Attendance.Status.present || status == Attendance.Status.late;
    }
}
