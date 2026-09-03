package com.sromantics.sromantics_api.entity;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    @Test
    void changePasswordHash_incrementsTokenVersion() {
        Instant now = Instant.now();
        User user = new User("u_001", "admin", "admin@example.com", "old-hash",
                "系統管理員", Set.of(UserRole.ADMIN), true, true, 0, now, 0, null, null, now, now);

        user.changePasswordHash("new-hash");

        assertThat(user.getPasswordHash()).isEqualTo("new-hash");
        assertThat(user.getTokenVersion()).isEqualTo(1);
    }

    @Test
    void newUser_canHoldMultipleRoles() {
        Instant now = Instant.now();
        User user = new User("u_001", "teacher", "teacher@example.com", "hash",
                "王老師", Set.of(UserRole.TEACHER, UserRole.STAFF), true, true, 0, now, 0, null, null, now, now);

        assertThat(user.getRoles()).containsExactlyInAnyOrder(UserRole.TEACHER, UserRole.STAFF);
    }
}
