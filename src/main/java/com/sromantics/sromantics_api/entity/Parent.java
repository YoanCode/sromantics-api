package com.sromantics.sromantics_api.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "parents")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Parent {

    @Id
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String phone;

    private String email;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Relationship relationship;

    public enum Relationship { father, mother, guardian }
}
