package com.sromantics.sromantics_api.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "enrollments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Enrollment {

    @Id
    private String id;

    @JsonIgnore
    @Column(name = "enrolled_at", nullable = false)
    private String enrolledAt;

    @JsonIgnore
    @Column(name = "payment_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;

    @JsonIgnore
    @Column(name = "purchased_lessons", nullable = false, columnDefinition = "integer default 0")
    private int purchasedLessons;

    @JsonIgnore
    @Column(name = "used_lessons", nullable = false, columnDefinition = "integer default 0")
    private int usedLessons;

    @JsonIgnore
    @Column(name = "remaining_lessons", nullable = false, columnDefinition = "integer default 0")
    private int remainingLessons;

    @JsonIgnore
    @Column(name = "student_id", nullable = false)
    private String studentId;

    @Column
    private String studentCourseId;

    @Column(nullable = false)
    private String classId;

    @Column
    private String startedAt;

    private String endedAt;

    @Column
    @Enumerated(EnumType.STRING)
    private Status status;

    public enum Status { active, transferred, completed, cancelled }
    public enum PaymentStatus { paid, unpaid, partial }
}
