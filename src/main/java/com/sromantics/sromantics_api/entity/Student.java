package com.sromantics.sromantics_api.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "students")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Student {

    @Id
    private String id;

    @Column(nullable = false)
    private String parentId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Column(nullable = false)
    private String schoolName;

    @Column(nullable = false)
    private String grade;

    private String note;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Status status;

    public enum Gender { male, female }
    public enum Status { active, graduated, suspended }
}
