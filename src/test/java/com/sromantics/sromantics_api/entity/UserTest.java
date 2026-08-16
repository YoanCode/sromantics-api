package com.sromantics.sromantics_api.entity;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    @Test
    void changePasswordHash_incrementsTokenVersion() {
        User user = new User("u_001", "admin", "admin@example.com", "old-hash",
                "系統管理員", Set.of(UserRole.ADMIN), true, true, 0, null, null);

        user.changePasswordHash("new-hash");

        assertThat(user.getPasswordHash()).isEqualTo("new-hash");
        assertThat(user.getTokenVersion()).isEqualTo(1);
    }

    @Test
    void newUser_canHoldMultipleRoles() {
        User user = new User("u_001", "teacher", "teacher@example.com", "hash",
                "王老師", Set.of(UserRole.TEACHER, UserRole.STAFF), true, true, 0, null, null);

        assertThat(user.getRoles()).containsExactlyInAnyOrder(UserRole.TEACHER, UserRole.STAFF);
    }
}
