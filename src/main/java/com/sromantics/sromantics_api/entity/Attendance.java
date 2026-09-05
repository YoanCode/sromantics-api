package com.sromantics.sromantics_api.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "attendances", uniqueConstraints = @UniqueConstraint(columnNames = {"enrollment_id", "attendance_date"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Attendance {

    @Id
    private String id;

    @Column(name = "enrollment_id", nullable = false)
    private String enrollmentId;

    @Column(name = "student_course_id", nullable = false)
    private String studentCourseId;

    @Column(name = "class_id", nullable = false)
    private String classId;

    @Column(name = "attendance_date", nullable = false)
    private String attendanceDate;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Status status;

    private String note;
    private String recordedAt;
    private String makeUpCreditId;

    public enum Status { present, absent, late, excused }
}
