package com.sromantics.sromantics_api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "student_courses")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentCourse {

    @Id
    private String id;

    @Column(nullable = false)
    private String studentId;

    @Column(nullable = false)
    private String courseId;

    @Column(nullable = false)
    private String enrolledAt;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;

    @Column(nullable = false, columnDefinition = "integer default 0")
    private int purchasedLessons;

    @Column(nullable = false, columnDefinition = "integer default 0")
    private int usedLessons;

    @Column(nullable = false, columnDefinition = "integer default 0")
    private int remainingLessons;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Status status;

    public enum PaymentStatus { paid, unpaid, partial }
    public enum Status { active, completed, cancelled }
}
