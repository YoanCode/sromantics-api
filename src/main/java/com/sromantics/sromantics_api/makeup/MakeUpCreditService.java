package com.sromantics.sromantics_api.makeup;

import com.sromantics.sromantics_api.entity.Attendance;
import com.sromantics.sromantics_api.entity.Clazz;
import com.sromantics.sromantics_api.entity.Enrollment;
import com.sromantics.sromantics_api.entity.MakeUpCredit;
import com.sromantics.sromantics_api.entity.StudentCourse;
import com.sromantics.sromantics_api.repository.AttendanceRepository;
import com.sromantics.sromantics_api.repository.ClazzRepository;
import com.sromantics.sromantics_api.repository.EnrollmentRepository;
import com.sromantics.sromantics_api.repository.MakeUpCreditRepository;
import com.sromantics.sromantics_api.repository.StudentCourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class MakeUpCreditService {

    private static final int DEFAULT_VALID_DAYS = 30;

    private final MakeUpCreditRepository repository;
    private final AttendanceRepository attendanceRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final StudentCourseRepository studentCourseRepository;
    private final ClazzRepository clazzRepository;

    @Transactional(readOnly = true)
    public List<MakeUpCredit> findAll() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public MakeUpCredit findById(String id) {
        return repository.findById(id).orElseThrow(() -> notFound("Make-up credit not found"));
    }

    public MakeUpCredit createFromAbsence(Attendance attendance, Enrollment enrollment) {
        if (attendance.getStatus() != Attendance.Status.absent
                || repository.findBySourceAttendanceId(attendance.getId()).isPresent()) {
            return repository.findBySourceAttendanceId(attendance.getId()).orElse(null);
        }
        MakeUpCredit credit = new MakeUpCredit();
        credit.setId(UUID.randomUUID().toString());
        credit.setStudentId(enrollment.getStudentId());
        credit.setSourceAttendanceId(attendance.getId());
        credit.setSourceEnrollmentId(enrollment.getId());
        credit.setValidUntil(LocalDate.parse(attendance.getAttendanceDate()).plusDays(DEFAULT_VALID_DAYS).toString());
        credit.setStatus(MakeUpCredit.Status.available);
        credit.setCreatedAt(LocalDateTime.now().toString());
        return repository.save(credit);
    }

    public MakeUpCredit update(String id, MakeUpCreditRequest request) {
        MakeUpCredit credit = findById(id);
        expireIfNeeded(credit);
        if (credit.getStatus() == MakeUpCredit.Status.used
                || credit.getStatus() == MakeUpCredit.Status.expired
                || credit.getStatus() == MakeUpCredit.Status.cancelled) {
            throw conflict("Make-up credit is no longer schedulable");
        }

        if (request.getTargetClassId() == null || request.getTargetDate() == null) {
            throw badRequest("Target class and target date are required");
        }
        Attendance sourceAttendance = attendanceRepository.findById(credit.getSourceAttendanceId())
                .orElseThrow(() -> badRequest("Source attendance not found"));
        Enrollment sourceEnrollment = enrollmentRepository.findById(credit.getSourceEnrollmentId())
                .orElseThrow(() -> badRequest("Source enrollment not found"));
        StudentCourse sourceCourse = studentCourseRepository.findById(sourceEnrollment.getStudentCourseId())
                .orElseThrow(() -> badRequest("Source student course not found"));
        Clazz targetClass = clazzRepository.findById(request.getTargetClassId())
                .orElseThrow(() -> badRequest("Target class not found"));
        if (!targetClass.getCourseId().equals(sourceCourse.getCourseId())) {
            throw badRequest("Target class must belong to the same course");
        }

        LocalDate targetDate = parseDate(request.getTargetDate());
        LocalDate sourceDate = parseDate(sourceAttendance.getAttendanceDate());
        if (targetDate.isBefore(sourceDate) || targetDate.isAfter(parseDate(credit.getValidUntil()))) {
            throw badRequest("Target date must be within the make-up credit validity period");
        }
        if (targetDate.getDayOfWeek().getValue() != targetClass.getDayOfWeek()) {
            throw badRequest("Target date is not a scheduled class day");
        }

        credit.setTargetClassId(targetClass.getId());
        credit.setTargetDate(targetDate.toString());
        credit.setStatus(MakeUpCredit.Status.scheduled);
        credit.setNote(request.getNote());
        return repository.save(credit);
    }

    public void cancel(String id) {
        MakeUpCredit credit = findById(id);
        if (credit.getStatus() == MakeUpCredit.Status.used) {
            throw conflict("Used make-up credit cannot be cancelled");
        }
        credit.setStatus(MakeUpCredit.Status.cancelled);
        repository.save(credit);
    }

    public void cancelForSourceAttendance(String attendanceId) {
        repository.findBySourceAttendanceId(attendanceId).ifPresent(credit -> {
            if (credit.getStatus() == MakeUpCredit.Status.used) {
                throw conflict("Used make-up credit cannot be cancelled");
            }
            if (credit.getStatus() != MakeUpCredit.Status.cancelled) {
                credit.setStatus(MakeUpCredit.Status.cancelled);
                repository.save(credit);
            }
        });
    }

    public MakeUpCredit markUsed(String id, String attendanceId) {
        MakeUpCredit credit = findById(id);
        expireIfNeeded(credit);
        if (credit.getStatus() != MakeUpCredit.Status.scheduled) {
            throw conflict("Make-up credit is not scheduled");
        }
        credit.setStatus(MakeUpCredit.Status.used);
        credit.setUsedAttendanceId(attendanceId);
        credit.setUsedAt(LocalDateTime.now().toString());
        return repository.save(credit);
    }

    public String validateScheduledCredit(String id, Enrollment targetEnrollment, String attendanceDate) {
        MakeUpCredit credit = findById(id);
        expireIfNeeded(credit);
        if (credit.getStatus() != MakeUpCredit.Status.scheduled) {
            throw conflict("Make-up credit is not scheduled");
        }
        if (!targetEnrollment.getStudentCourseId().equals(
                enrollmentRepository.findById(credit.getSourceEnrollmentId())
                        .orElseThrow(() -> badRequest("Source enrollment not found"))
                        .getStudentCourseId())) {
            throw badRequest("Make-up attendance must use the same student course");
        }
        if (!attendanceDate.equals(credit.getTargetDate())) {
            throw badRequest("Attendance does not match the scheduled make-up date");
        }
        return credit.getTargetClassId();
    }

    public void restoreFromAttendance(String id) {
        repository.findById(id).ifPresent(credit -> {
            if (credit.getStatus() == MakeUpCredit.Status.used) {
                credit.setStatus(MakeUpCredit.Status.scheduled);
                credit.setUsedAttendanceId(null);
                credit.setUsedAt(null);
                repository.save(credit);
            } else if (credit.getStatus() != MakeUpCredit.Status.cancelled) {
                repository.delete(credit);
            }
        });
    }

    public void expireIfNeeded(MakeUpCredit credit) {
        if ((credit.getStatus() == MakeUpCredit.Status.available || credit.getStatus() == MakeUpCredit.Status.scheduled)
                && LocalDate.now().isAfter(parseDate(credit.getValidUntil()))) {
            credit.setStatus(MakeUpCredit.Status.expired);
            repository.save(credit);
        }
    }

    private LocalDate parseDate(String value) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeException exception) {
            throw badRequest("Date must use yyyy-MM-dd");
        }
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }

    private ResponseStatusException notFound(String message) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }
}
