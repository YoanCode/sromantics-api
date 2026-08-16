# Spec 02: User Controller API

## 目標

建立 User Management Page 所需的 RESTful API，讓 `sromantics-web` 可於 `/users` 管理使用者。API 必須以 request / response DTO 隔離 `User` Entity，並安全處理密碼與帳號狀態，為後續 Spring Security JWT 整合保留正確的權杖失效行為。

本規格的 API contract 對應：

```
sromantics-web/specs/create-user-page/01-user-management-page.md
```

---

## 需建立與修改的檔案

```
src/main/java/com/sromantics/sromantics_api/
├── config/
│   └── PasswordConfig.java                         ← 新增
├── entity/
│   └── User.java                                   ← 修改，加入初始密碼設定 method
├── dto/user/
│   ├── CreateUserRequest.java                      ← 新增
│   ├── UpdateUserRequest.java                      ← 新增
│   ├── ChangePasswordRequest.java                  ← 新增
│   └── UserResponse.java                           ← 新增
└── user/
    ├── UserController.java                         ← 新增
    └── UserService.java                            ← 新增

pom.xml                                              ← 修改
```

---

## API 端點總覽

| Method | Path | 說明 | 成功回應 |
| --- | --- | --- | --- |
| `GET` | `/api/users` | 取得全部使用者 | `200` `UserResponse[]` |
| `GET` | `/api/users/{id}` | 取得單一使用者 | `200` `UserResponse` |
| `POST` | `/api/users` | 建立使用者 | `201` `UserResponse` |
| `PUT` | `/api/users/{id}` | 更新使用者基本資料、角色與帳號狀態 | `200` `UserResponse` |
| `PUT` | `/api/users/{id}/password` | 變更使用者密碼 | `204` |
| `DELETE` | `/api/users/{id}` | 刪除使用者 | `204` |

### HTTP 錯誤約定

| 狀況 | Status | 說明 |
| --- | --- | --- |
| Request 欄位格式錯誤 | `400` | Bean Validation 驗證失敗 |
| username 或 email 已被使用 | `409` | 建立或更新時違反唯一性 |
| 使用者不存在 | `404` | id 查詢、更新、變更密碼或刪除失敗 |
| 未登入 | `401` | JWT Security 完成後套用 |
| 非 `ADMIN` 的管理操作 | `403` | JWT Security 完成後套用 |

> 本階段尚未實作 JWT 驗證；完成 Security 規格後，所有 `/api/users/**` 管理 endpoint 必須限制為 `ROLE_ADMIN`。Controller 與 Service 不可將前端傳入的角色視為授權依據。

---

## Step 1: 加入必要依賴

使用 `spring-security-crypto` 提供 BCrypt 雜湊，不加入 `spring-boot-starter-security`。後者會立即變更整個應用程式的預設認證行為，應在 JWT Security 規格中一併設定。

同時加入 Validation starter，供 `@Valid` 與 Jakarta Bean Validation 使用。

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-crypto</artifactId>
</dependency>
```

---

## Step 2: 建立 PasswordEncoder Bean

新增 `config/PasswordConfig.java`。密碼只可透過 `PasswordEncoder` 寫入，禁止直接使用 `BCryptPasswordEncoder` 的細節散落在 Controller 或以明文寫入 Entity。

```java
package com.sromantics.sromantics_api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class PasswordConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

---

## Step 3: 建立 API DTO

DTO 一律放在 `dto/user` package。不可使用 `User` Entity 作為 `@RequestBody` 或 response，也不可在任何 DTO 放入 `passwordHash`、`tokenVersion`。

### `CreateUserRequest.java`

```java
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
```

`password` 上限 72 是 BCrypt 有效輸入長度上限；更長密碼不可默默截斷。

### `UpdateUserRequest.java`

`username` 不可由此 endpoint 修改，避免登入識別值與既有 JWT / 稽核記錄產生不一致。

```java
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
```

### `ChangePasswordRequest.java`

```java
package com.sromantics.sromantics_api.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank @Size(min = 8, max = 72) String password
) {}
```

### `UserResponse.java`

`Instant` 會由 Jackson 輸出 ISO-8601 UTC 字串，符合 web 規格的 `createdAt`、`updatedAt`。

```java
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
```

---

## Step 4: 建立 `UserService`

Service 負責欄位正規化、唯一性驗證、密碼雜湊與 Entity / DTO 轉換；Controller 只處理 HTTP routing。

先在 `User.java` 加入只供建立帳號使用的方法。這可讓新使用者從 `tokenVersion = 0` 開始，且不會把 Lombok 的一般 setter 開放給密碼欄位。

```java
public void initializePasswordHash(String passwordHash) {
    this.passwordHash = passwordHash;
}
```

```java
package com.sromantics.sromantics_api.user;

import com.sromantics.sromantics_api.dto.user.*;
import com.sromantics.sromantics_api.entity.User;
import com.sromantics.sromantics_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

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
        ensureUsernameAvailable(username, null);
        ensureEmailAvailable(email, null);

        User user = new User();
        user.setId(UUID.randomUUID().toString());
        user.setUsername(username);
        user.changeEmail(email);
        user.setDisplayName(request.displayName().trim());
        user.setRoles(request.roles());
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
        user.setRoles(request.roles());
        user.setEnabled(request.enabled());
        user.setAccountNonLocked(request.accountNonLocked());
        if (invalidatesTokens) {
            user.setTokenVersion(user.getTokenVersion() + 1);
        }
        return toResponse(user);
    }

    public void changePassword(String id, ChangePasswordRequest request) {
        User user = findEntity(id);
        user.changePasswordHash(passwordEncoder.encode(request.password()));
    }

    public void delete(String id) {
        userRepository.delete(findEntity(id));
    }

    private User findEntity(String id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private void ensureUsernameAvailable(String username, String currentId) {
        userRepository.findByUsername(username)
                .filter(user -> !user.getId().equals(currentId))
                .ifPresent(user -> conflict("username 已被使用"));
    }

    private void ensureEmailAvailable(String email, String currentId) {
        userRepository.findByEmail(email)
                .filter(user -> !user.getId().equals(currentId))
                .ifPresent(user -> conflict("email 已被使用"));
    }

    private void conflict(String reason) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, reason);
    }

    private String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getEmail(),
                user.getDisplayName(), user.getRoles(), user.isEnabled(),
                user.isAccountNonLocked(), user.getCreatedAt(), user.getUpdatedAt());
    }
}
```

### Service 規則

- `username`、`email` 在存取前必須 `trim()` 並以 `Locale.ROOT` 轉小寫。
- `displayName` 必須 `trim()`；密碼不可 `trim()`，避免使用者實際輸入與雜湊內容不一致。
- `create()` 不得使用 `changePasswordHash()`，因為該 method 會遞增 `tokenVersion`；新使用者版本必須為 `0`。
- password 變更、`enabled` 改變或 `accountNonLocked` 改變時，必須使既有 JWT 失效，故遞增 `tokenVersion`。
- 僅變更顯示名稱、email、角色時，`tokenVersion` 不變；若日後 JWT 直接包含 role claims，應將角色變更也納入失效條件。
- application-level 唯一性檢查只改善錯誤訊息；資料庫 unique constraint 仍是最終保證。需攔截 `DataIntegrityViolationException` 並回傳 `409`，處理併發建立的競態條件。

---

## Step 5: 建立 `UserController`

```java
package com.sromantics.sromantics_api.user;

import com.sromantics.sromantics_api.dto.user.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

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
```

---

## Step 6: 錯誤回應與後續 Security

新增 `@RestControllerAdvice`（可於共用 exception package）處理 `DataIntegrityViolationException`，對外只回傳通用 `409 Conflict` 訊息，不回傳 SQL、constraint 名稱或密碼資料。

後續整合 Spring Security JWT 時，需補上：

- `/api/users/**` 使用 `hasRole("ADMIN")`。
- 刪除使用者前，拒絕刪除目前登入帳號，避免失去唯一管理者存取權。
- 角色變更後若 access token 攜帶 `roles` claim，需遞增 `tokenVersion` 或於每次 request 從資料庫重新載入 authorities。
- `UserDetailsService` 載入後應檢查 `enabled`、`accountNonLocked` 與 JWT `ver` 是否符合 `tokenVersion`。

---

## 驗證

### Controller / Service 測試

新增 `src/test/java/com/sromantics/sromantics_api/user/` 的 `UserServiceTest`、`UserControllerTest`，至少測試：

- 建立使用者會生成 UUID、將 username / email 正規化，並以 BCrypt 雜湊密碼。
- `UserResponse` JSON 不含 `passwordHash`、`tokenVersion` 或明文 password。
- 重複 username 或 email 回傳 `409`。
- 密碼變更後 `tokenVersion` 增加。
- 停用或鎖定帳號後 `tokenVersion` 增加；僅更新 displayName 時不增加。
- 不存在 id 的 get、update、change password、delete 回傳 `404`。
- 不合法 request（空白欄位、無角色、錯誤 email、短密碼）回傳 `400`。

### 手動驗證

```bash
# 建立使用者
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"username":"teacher.chen","email":"teacher.chen@example.com","displayName":"陳老師","password":"Passw0rd!","roles":["TEACHER","STAFF"]}'

# 取得使用者列表
curl http://localhost:8080/api/users

# 更新帳號狀態與角色
curl -X PUT http://localhost:8080/api/users/{id} \
  -H "Content-Type: application/json" \
  -d '{"email":"teacher.chen@example.com","displayName":"陳老師","roles":["TEACHER"],"enabled":true,"accountNonLocked":false}'

# 變更密碼
curl -X PUT http://localhost:8080/api/users/{id}/password \
  -H "Content-Type: application/json" \
  -d '{"password":"NewPassw0rd!"}'

# 刪除使用者
curl -X DELETE http://localhost:8080/api/users/{id}
```

預期結果：

- `POST` 回傳 `201` 與不含任何密碼欄位的使用者資料。
- `GET` / `PUT` 回傳角色 array、帳號狀態與 ISO-8601 timestamps。
- password endpoint 回傳 `204`，且不回傳 body。
- `DELETE` 回傳 `204`。
- 所有驗證、衝突與不存在資源的錯誤 status 符合本規格。
