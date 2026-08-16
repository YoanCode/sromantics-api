# Spec 01: User Data Model

## 目標

建立可供帳號登入與授權使用的 `User` 資料模型。此規格只定義資料層與 Repository，不建立登入 API、JWT 簽發或 Spring Security 設定；但欄位設計必須能在後續直接對應 Spring Security 的 `UserDetails` 與 JWT claims。

---

## 新增檔案結構

```
src/main/java/com/sromantics/sromantics_api/
├── entity/
│   ├── User.java
│   └── UserRole.java
└── repository/
    └── UserRepository.java
```

---

## Step 1: 建立 `UserRole` enum

角色是授權資料，儲存時使用 enum 名稱；後續轉為 Spring Security `GrantedAuthority` 時，一律加上 `ROLE_` 前綴，例如 `ADMIN` 對應 `ROLE_ADMIN`。

```java
package com.sromantics.sromantics_api.entity;

public enum UserRole {
    ADMIN,
    STAFF,
    TEACHER
}
```

> 若未來需要讓家長登入，可新增 `PARENT`；不可將角色寫死在 JWT 驗證邏輯中，以利後續擴充。

---

## Step 2: 建立 `User` Entity

資料表名稱使用 `users`，避免與部分資料庫的保留字 `user` 衝突。

```java
package com.sromantics.sromantics_api.entity;

import jakarta.persistence.*;
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

    public void changeEmail(String email) {
        this.email = email;
    }
}
```

### 欄位說明

| 欄位 | 用途 | JWT / Spring Security 對應 |
| --- | --- | --- |
| `id` | UUID 字串、建立後不可修改 | JWT `sub`；作為使用者穩定識別值，不使用可變的帳號或 email |
| `username` | 登入帳號，系統內唯一 | `UserDetails#getUsername()` 與登入識別欄位 |
| `email` | 使用者 email，系統內唯一 | 個人資料或通知用途，不放入必要 claims |
| `passwordHash` | BCrypt 或 Argon2 產生的密碼雜湊 | `UserDetails#getPassword()`；絕不回傳給 API 或放進 JWT |
| `displayName` | 介面顯示名稱 | 可選的 `name` claim |
| `roles` | 使用者角色集合 | 轉成 `ROLE_{role}` authorities；可選的 `roles` claim |
| `enabled` | 帳號是否啟用 | `UserDetails#isEnabled()` |
| `accountNonLocked` | 帳號是否被鎖定 | `UserDetails#isAccountNonLocked()` |
| `tokenVersion` | 權杖版本 | 可選的 `ver` claim；改密碼或管理員強制登出時遞增，使舊 token 失效 |
| `createdAt` / `updatedAt` | 建立與最後更新時間 | 稽核與帳號管理用途 |

### 設計限制

- 建立使用者時，`id` 由 Service 以 `UUID.randomUUID().toString()` 產生；不可讓前端指定。
- `username` 與 `email` 必須在寫入前 `trim()`，並統一轉為小寫，以保證唯一性比較一致。
- 密碼只能透過 `PasswordEncoder` 雜湊後寫入 `passwordHash`。禁止儲存明文、可逆加密密碼或自行實作雜湊。
- 不使用 Lombok `@Data`，避免 `passwordHash` 被自動納入 `toString()`、`equals()` 或 API 除錯輸出。
- 不可在 Controller 回傳 `User` entity；後續 API 應建立不含 `passwordHash` 的 Response DTO。
- `roles` 預設為空集合；帳號建立流程必須明確指定至少一個角色，避免無權限帳號被誤建立。

---

## Step 3: 建立 `UserRepository`

登入流程可用 username 或 email 查詢帳號；存在性查詢用於註冊或後台建立帳號時的唯一性驗證。

```java
package com.sromantics.sromantics_api.repository;

import com.sromantics.sromantics_api.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}
```

---

## 後續 JWT 整合約定

後續實作 Spring Security 時，建立 `UserDetailsService` 由 `UserRepository.findByUsername(...)` 載入使用者，並將 `roles` 映射為 `SimpleGrantedAuthority("ROLE_" + role.name())`。

Access token 建議的 claims：

```json
{
  "sub": "使用者 UUID",
  "username": "登入帳號",
  "roles": ["ADMIN"],
  "ver": 0,
  "iat": 0,
  "exp": 0
}
```

- JWT 驗證成功後，仍須確認帳號存在、`enabled` 為 `true`、`accountNonLocked` 為 `true`，並比對 token 的 `ver` 與 `tokenVersion`。
- 密碼變更、帳號停用、帳號鎖定，或管理員要求全裝置登出時，應遞增 `tokenVersion`。
- JWT 不可包含 `passwordHash`、email、或其他不必要個資。JWT 是簽章而非加密格式。
- Refresh token、登入失敗次數、密碼重設 token 等需撤銷或有生命週期的資料，應在未來建立獨立資料表；不要塞入 `users` 表。

---

## 驗證

完成後應確認：

- 應建立 `users` 與 `user_roles` 兩張資料表。
- `username`、`email` 的重複資料庫寫入會被拒絕。
- 每位使用者可擁有一個以上角色，且角色以 enum 字串保存。
- 使用者 entity 的字串輸出與任何 Response DTO 都不會洩漏 `passwordHash`。
- 更新密碼時，`changePasswordHash()` 會同步遞增 `tokenVersion`。
