package com.sromantics.sromantics_api.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "make_up_credits", uniqueConstraints = @UniqueConstraint(columnNames = "source_attendance_id"))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MakeUpCredit {

    @Id
    private String id;

    @Column(nullable = false)
    private String studentId;

    @Column(nullable = false)
    private String sourceAttendanceId;

    @Column(nullable = false)
    private String sourceEnrollmentId;

    @Column(nullable = false)
    private String validUntil;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Status status;

    private String targetClassId;
    private String targetDate;
    private String usedAttendanceId;
    private String note;
    private String createdAt;
    private String usedAt;

    public enum Status { available, scheduled, used, expired, cancelled }
}
