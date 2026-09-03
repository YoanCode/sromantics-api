package com.sromantics.sromantics_api.dto.user;

import com.sromantics.sromantics_api.entity.UserRole;

import java.time.Instant;
import java.util.Set;

public record UserResponse(
        String id,
        String username,
        String email,
        String displayName,
        Set<UserRole> roles,
        boolean enabled,
        boolean accountNonLocked,
        Instant createdAt,
        Instant updatedAt
) {}
