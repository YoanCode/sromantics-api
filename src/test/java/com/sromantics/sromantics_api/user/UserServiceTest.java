package com.sromantics.sromantics_api.user;

import com.sromantics.sromantics_api.dto.user.ChangePasswordRequest;
import com.sromantics.sromantics_api.dto.user.CreateUserRequest;
import com.sromantics.sromantics_api.dto.user.UpdateUserRequest;
import com.sromantics.sromantics_api.dto.user.UserResponse;
import com.sromantics.sromantics_api.entity.User;
import com.sromantics.sromantics_api.entity.UserRole;
import com.sromantics.sromantics_api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User("u_001", "admin", "admin@example.com", "old-hash",
                "系統管理員", Set.of(UserRole.ADMIN), true, true, 0, null, null);
    }

    @Test
    void create_normalizesIdentityAndStoresEncodedPassword() {
        CreateUserRequest request = new CreateUserRequest(" Admin.User ", " ADMIN@EXAMPLE.COM ",
                " 系統管理員 ", "Passw0rd!", Set.of(UserRole.ADMIN));
        when(passwordEncoder.encode("Passw0rd!")).thenReturn("bcrypt-hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse result = userService.create(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getUsername()).isEqualTo("admin.user");
        assertThat(saved.getEmail()).isEqualTo("admin@example.com");
        assertThat(saved.getDisplayName()).isEqualTo("系統管理員");
        assertThat(saved.getPasswordHash()).isEqualTo("bcrypt-hash");
        assertThat(saved.getTokenVersion()).isZero();
        assertThat(result.username()).isEqualTo("admin.user");
    }

    @Test
    void create_throwsConflictWhenUsernameExists() {
        when(userRepository.existsByUsername("admin")).thenReturn(true);

        assertThatThrownBy(() -> userService.create(new CreateUserRequest(
                "admin", "new@example.com", "新使用者", "Passw0rd!", Set.of(UserRole.STAFF))))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void changePassword_incrementsTokenVersionAndEncodesPassword() {
        when(userRepository.findById("u_001")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("NewPassw0rd!")).thenReturn("new-hash");

        userService.changePassword("u_001", new ChangePasswordRequest("NewPassw0rd!"));

        assertThat(user.getPasswordHash()).isEqualTo("new-hash");
        assertThat(user.getTokenVersion()).isEqualTo(1);
        verify(userRepository).save(user);
    }

    @Test
    void updateAccountState_incrementsTokenVersion() {
        when(userRepository.findById("u_001")).thenReturn(Optional.of(user));
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        userService.update("u_001", new UpdateUserRequest("admin@example.com", "系統管理員",
                Set.of(UserRole.ADMIN), false, true));

        assertThat(user.getTokenVersion()).isEqualTo(1);
        assertThat(user.isEnabled()).isFalse();
    }

    @Test
    void findById_throwsNotFoundWhenMissing() {
        when(userRepository.findById("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findById("unknown"))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }
}
