package com.sromantics.sromantics_api.dto.user;

import com.sromantics.sromantics_api.entity.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record CreateUserRequest(
        @NotBlank @Size(min = 3, max = 50)
        @Pattern(regexp = "^[a-zA-Z0-9_.-]+$", message = "username 僅能使用英數字、.、_、-")
        String username,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(max = 100) String displayName,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotEmpty Set<UserRole> roles
) {}
