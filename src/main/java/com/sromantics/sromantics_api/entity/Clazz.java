package com.sromantics.sromantics_api.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "classes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Clazz {

    @Id
    private String id;

    @Column(nullable = false)
    private String courseId;

    @Column(nullable = false)
    private String className;

    @Column(nullable = false)
    private String teacherName;

    @Column(nullable = false)
    private String classroom;

    @Column(nullable = false)
    private int dayOfWeek;

    @Column(nullable = false)
    private String startTime;

    @Column(nullable = false)
    private String endTime;

    @Column(nullable = false)
    private int maxCapacity;

    @Column(nullable = false)
    private int pricePerPeriod;
}
