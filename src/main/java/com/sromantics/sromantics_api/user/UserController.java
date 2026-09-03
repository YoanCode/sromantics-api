package com.sromantics.sromantics_api.user;

import com.sromantics.sromantics_api.dto.user.ChangePasswordRequest;
import com.sromantics.sromantics_api.dto.user.CreateUserRequest;
import com.sromantics.sromantics_api.dto.user.UpdateUserRequest;
import com.sromantics.sromantics_api.dto.user.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public List<UserResponse> list() {
        return userService.findAll();
    }

    @GetMapping("/{id}")
    public UserResponse get(@PathVariable String id) {
        return userService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@Valid @RequestBody CreateUserRequest request) {
        return userService.create(request);
    }

    @PutMapping("/{id}")
    public UserResponse update(
            @PathVariable String id,
            @Valid @RequestBody UpdateUserRequest request
    ) {
        return userService.update(id, request);
    }

    @PutMapping("/{id}/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(
            @PathVariable String id,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        userService.changePassword(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        userService.delete(id);
    }
}
