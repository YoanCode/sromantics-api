package com.sromantics.sromantics_api.user;

import com.sromantics.sromantics_api.dto.user.AdminResetPasswordRequest;
import com.sromantics.sromantics_api.dto.user.ChangePasswordRequest;
import com.sromantics.sromantics_api.dto.user.CreateUserRequest;
import com.sromantics.sromantics_api.dto.user.UpdateUserRequest;
import com.sromantics.sromantics_api.dto.user.UserResponse;
import com.sromantics.sromantics_api.entity.User;
import com.sromantics.sromantics_api.repository.UserRepository;
import com.sromantics.sromantics_api.util.PasswordStrengthValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordStrengthValidator passwordValidator;

    @Transactional(readOnly = true)
    public List<UserResponse> findAll() {
        return userRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public UserResponse findById(String id) {
        return toResponse(findEntity(id));
    }

    public UserResponse create(CreateUserRequest request) {
        String username = normalize(request.username());
        String email = normalize(request.email());
        ensureUsernameAvailable(username);
        ensureEmailAvailable(email, null);

        User user = new User();
        user.setId(UUID.randomUUID().toString());
        user.setUsername(username);
        user.changeEmail(email);
        user.setDisplayName(request.displayName().trim());
        user.setRoles(new HashSet<>(request.roles()));
        user.initializePasswordHash(passwordEncoder.encode(request.password()));

        return toResponse(userRepository.save(user));
    }

    public UserResponse update(String id, UpdateUserRequest request) {
        User user = findEntity(id);
        String email = normalize(request.email());
        ensureEmailAvailable(email, id);

        boolean invalidatesTokens = user.isEnabled() != request.enabled()
                || user.isAccountNonLocked() != request.accountNonLocked();

        user.changeEmail(email);
        user.setDisplayName(request.displayName().trim());
        user.setRoles(new HashSet<>(request.roles()));
        user.setEnabled(request.enabled());
        user.setAccountNonLocked(request.accountNonLocked());
        if (invalidatesTokens) {
            user.setTokenVersion(user.getTokenVersion() + 1);
        }

        return toResponse(userRepository.save(user));
    }

    public void changePassword(String id, ChangePasswordRequest request) {
        passwordValidator.validate(request.password());

        User user = findEntity(id);

        // 驗證當前密碼
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "當前密碼錯誤");
        }

        user.changePasswordHash(passwordEncoder.encode(request.password()));
        user.setPasswordChangedAt(Instant.now());
        userRepository.save(user);
    }

    public void delete(String id) {
        userRepository.delete(findEntity(id));
    }

    /**
     * 管理員重置用戶密碼 (無需驗證當前密碼)
     */
    public void adminResetPassword(String id, AdminResetPasswordRequest request) {
        passwordValidator.validate(request.newPassword());

        User user = findEntity(id);
        user.changePasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setPasswordChangedAt(Instant.now());
        userRepository.save(user);
    }

    /**
     * 管理員解鎖賬戶
     */
    public void unlockUser(String id) {
        User user = findEntity(id);
        user.unlock();
        userRepository.save(user);
    }

    private User findEntity(String id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private void ensureUsernameAvailable(String username) {
        if (userRepository.existsByUsername(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "username 已被使用");
        }
    }

    private void ensureEmailAvailable(String email, String currentId) {
        userRepository.findByEmail(email)
                .filter(user -> !user.getId().equals(currentId))
                .ifPresent(user -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "email 已被使用");
                });
    }

    private String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getEmail(),
                user.getDisplayName(), Set.copyOf(user.getRoles()), user.isEnabled(),
                user.isAccountNonLocked(), user.getCreatedAt(), user.getUpdatedAt());
    }
}
