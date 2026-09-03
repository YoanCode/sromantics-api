package com.sromantics.sromantics_api.dto.user;

import com.sromantics.sromantics_api.entity.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record UpdateUserRequest(
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(max = 100) String displayName,
        @NotEmpty Set<UserRole> roles,
        boolean enabled,
        boolean accountNonLocked
) {}
