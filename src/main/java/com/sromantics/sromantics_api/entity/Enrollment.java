package com.sromantics.sromantics_api.entity;

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

    @Column(nullable = false)
    private String studentId;

    @Column(nullable = false)
    private String classId;

    @Column(nullable = false)
    private String enrolledAt;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;

    @Column(nullable = false)
    private int remainingLessons;

    public enum PaymentStatus { paid, unpaid, partial }
}
