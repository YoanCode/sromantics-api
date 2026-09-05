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
}
