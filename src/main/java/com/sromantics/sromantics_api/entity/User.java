package com.sromantics.sromantics_api.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_users_username", columnNames = "username"),
                @UniqueConstraint(name = "uk_users_email", columnNames = "email")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @Column(nullable = false, updatable = false, length = 36)
    private String id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Setter(AccessLevel.NONE)
    @Column(nullable = false, unique = true, length = 254)
    private String email;

    @Setter(AccessLevel.NONE)
    @Column(nullable = false, name = "password_hash", length = 255)
    private String passwordHash;

    @Column(nullable = false, length = 100)
    private String displayName;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 30)
    private Set<UserRole> roles = new HashSet<>();

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(nullable = false)
    private boolean accountNonLocked = true;

    @Column(nullable = false)
    private int tokenVersion = 0;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public void changePasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
        this.tokenVersion++;
    }

    public void initializePasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void changeEmail(String email) {
        this.email = email;
    }
}
