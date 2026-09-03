# Spec 04: User Password Management

## 目標

為內部系統提供簡潔、易維護的密碼管理功能：
- 簡化的密碼強度驗證 (4 項條件)
- 登入嘗試限制與賬戶鎖定 (5 次失敗後鎖定 30 分鐘)
- 用戶自己變更密碼 (需驗證當前密碼)
- 管理員重置用戶密碼 (無需驗證當前密碼)
- 管理員手動解鎖賬戶

本規格對應前端：
```
sromantics-web/specs/create-user-page/02-user-password-management.md
```

---

## 需建立與修改的檔案

### 後端

```
src/main/java/com/sromantics/sromantics_api/
├── entity/
│   └── User.java                                   ← 修改，添加 4 個欄位與方法
├── dto/user/
│   ├── ChangePasswordRequest.java                  ← 修改，添加 currentPassword
│   └── AdminResetPasswordRequest.java              ← 新增
├── util/
│   └── PasswordStrengthValidator.java              ← 新增
├── exception/
│   └── InvalidPasswordException.java               ← 新增
├── user/
│   ├── UserService.java                            ← 修改，改進變更密碼與新增管理員操作
│   └── UserController.java                         ← 修改，添加 2 個管理員端點
└── repository/
    └── UserRepository.java                         ← 新增查詢方法 (可選)

database/
└── V004__add_password_fields.sql                   ← 新增 Flyway migration
```

### 前端

```
src/features/users/
├── components/
│   ├── admin-reset-password-dialog.tsx            ← 新增
│   ├── data-table-row-actions.tsx                 ← 修改，添加重置密碼/解鎖選項
│   └── users-dialogs.tsx                          ← 修改，導入 AdminResetPasswordDialog
└── types/user.ts                                  ← 修改，更新 ChangePasswordRequest

routes/auth/
└── sign-in/components/user-auth-form.tsx          ← 修改，移除"忘記密碼"連結
```

---

## 後端實現

### Step 1: 資料庫 Migration

新增 `database/V004__add_password_fields.sql`：

```sql
ALTER TABLE users ADD COLUMN password_changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE users ADD COLUMN failed_login_attempts INT NOT NULL DEFAULT 0;
ALTER TABLE users ADD COLUMN last_failed_login_at TIMESTAMP;
ALTER TABLE users ADD COLUMN last_successful_login_at TIMESTAMP;
```

### Step 2: 修改 User Entity

向 `User.java` 添加欄位與方法：

```java
package com.sromantics.sromantics_api.entity;

import java.time.Duration;
import java.time.Instant;

@Entity
@Table(name = "users", uniqueConstraints = {...})
public class User {
    // 既有欄位...
    
    @Column(nullable = false)
    private Instant passwordChangedAt;
    
    @Column(nullable = false)
    private int failedLoginAttempts = 0;
    
    @Column
    private Instant lastFailedLoginAt;
    
    @Column
    private Instant lastSuccessfulLoginAt;
    
    // --- 方法 ---
    
    /**
     * 記錄登入失敗嘗試
     * 5 次失敗後自動鎖定賬戶
     */
    public void recordFailedLoginAttempt() {
        this.failedLoginAttempts++;
        this.lastFailedLoginAt = Instant.now();
        if (this.failedLoginAttempts >= 5) {
            this.accountNonLocked = false;
        }
    }
    
    /**
     * 記錄成功登入，重置失敗計數
     */
    public void recordSuccessfulLogin() {
        this.failedLoginAttempts = 0;
        this.lastSuccessfulLoginAt = Instant.now();
    }
    
    /**
     * 檢查是否暫時被鎖定 (30 分鐘自動解鎖)
     */
    public boolean isTemporarilyLocked() {
        if (!accountNonLocked && lastFailedLoginAt != null) {
            return Instant.now().isBefore(lastFailedLoginAt.plus(Duration.ofMinutes(30)));
        }
        return false;
    }
    
    /**
     * 解鎖賬戶，重置失敗計數
     */
    public void unlock() {
        this.accountNonLocked = true;
        this.failedLoginAttempts = 0;
    }
}
```

### Step 3: 密碼強度驗證器

新增 `util/PasswordStrengthValidator.java`：

```java
package com.sromantics.sromantics_api.util;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class PasswordStrengthValidator {
    
    /**
     * 驗證密碼強度
     * 要求：8-72 字符，包含大寫字母、小寫字母、數字或特殊字符
     */
    public void validate(String password) throws InvalidPasswordException {
        List<String> errors = new ArrayList<>();
        
        if (password.length() < 8 || password.length() > 72) {
            errors.add("密碼長度需 8-72 字元");
        }
        if (!password.matches(".*[A-Z].*")) {
            errors.add("密碼必須包含大寫字母 (A-Z)");
        }
        if (!password.matches(".*[a-z].*")) {
            errors.add("密碼必須包含小寫字母 (a-z)");
        }
        if (!password.matches(".*[0-9!@#$%^&*].*")) {
            errors.add("密碼必須包含數字或特殊字符 (0-9 或 !@#$%^&*)");
        }
        
        if (!errors.isEmpty()) {
            throw new InvalidPasswordException(String.join("; ", errors));
        }
    }
}
```

### Step 4: 自訂異常

新增 `exception/InvalidPasswordException.java`：

```java
package com.sromantics.sromantics_api.exception;

public class InvalidPasswordException extends RuntimeException {
    public InvalidPasswordException(String message) {
        super(message);
    }
}
```

### Step 5: 修改 ChangePasswordRequest DTO

修改 `dto/user/ChangePasswordRequest.java`，添加當前密碼驗證：

```java
package com.sromantics.sromantics_api.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank @Size(min = 8, max = 72) String currentPassword,
        @NotBlank @Size(min = 8, max = 72) String password
) {}
```

### Step 6: 新增 AdminResetPasswordRequest DTO

新增 `dto/user/AdminResetPasswordRequest.java`：

```java
package com.sromantics.sromantics_api.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminResetPasswordRequest(
        @NotBlank @Size(min = 8, max = 72) String newPassword
) {}
```

### Step 7: 修改 UserService

修改 `user/UserService.java` 中的密碼相關方法：

```java
package com.sromantics.sromantics_api.user;

import com.sromantics.sromantics_api.dto.user.ChangePasswordRequest;
import com.sromantics.sromantics_api.dto.user.AdminResetPasswordRequest;
import com.sromantics.sromantics_api.entity.User;
import com.sromantics.sromantics_api.exception.InvalidPasswordException;
import com.sromantics.sromantics_api.repository.UserRepository;
import com.sromantics.sromantics_api.util.PasswordStrengthValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordStrengthValidator passwordValidator;
    
    /**
     * 用戶自己變更密碼 (需驗證當前密碼)
     */
    public void changePassword(String userId, ChangePasswordRequest request) {
        passwordValidator.validate(request.password());
        
        User user = findEntity(userId);
        
        // 驗證當前密碼
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "當前密碼錯誤");
        }
        
        user.changePasswordHash(passwordEncoder.encode(request.password()));
        user.setPasswordChangedAt(Instant.now());
        userRepository.save(user);
    }
    
    /**
     * 管理員重置用戶密碼 (無需驗證當前密碼)
     */
    public void adminResetPassword(String userId, AdminResetPasswordRequest request) {
        passwordValidator.validate(request.newPassword());
        
        User user = findEntity(userId);
        user.changePasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setPasswordChangedAt(Instant.now());
        userRepository.save(user);
    }
    
    /**
     * 管理員解鎖賬戶
     */
    public void unlockUser(String userId) {
        User user = findEntity(userId);
        user.unlock();
        userRepository.save(user);
    }
    
    private User findEntity(String id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}
```

### Step 8: 修改 UserController

修改 `user/UserController.java`，添加管理員端點：

```java
package com.sromantics.sromantics_api.user;

import com.sromantics.sromantics_api.dto.user.AdminResetPasswordRequest;
import com.sromantics.sromantics_api.dto.user.ChangePasswordRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;
    
    // 既有端點...
    
    /**
     * 用戶自己變更密碼
     */
    @PutMapping("/users/{id}/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(
            @PathVariable String id,
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(id, request);
    }
    
    /**
     * 管理員重置用戶密碼 (需要 ADMIN 角色)
     */
    @PostMapping("/admin/users/{id}/password/reset")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void adminResetPassword(
            @PathVariable String id,
            @Valid @RequestBody AdminResetPasswordRequest request) {
        userService.adminResetPassword(id, request);
    }
    
    /**
     * 管理員解鎖賬戶 (需要 ADMIN 角色)
     */
    @PostMapping("/admin/users/{id}/unlock")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unlockUser(@PathVariable String id) {
        userService.unlockUser(id);
    }
}
```

---

## 前端實現

### 密碼強度指示器 (TypeScript)

更新 `src/types/user.ts`：

```typescript
export const changePasswordSchema = z
  .object({
    currentPassword: z.string().min(8, '當前密碼至少需 8 個字元'),
    password: z.string().min(8, '新密碼至少需 8 個字元').max(72),
    confirmPassword: z.string(),
  })
  .refine((data) => data.password === data.confirmPassword, {
    message: '新密碼不一致',
    path: ['confirmPassword'],
  })

export type ChangePasswordInput = z.infer<typeof changePasswordSchema>

// 密碼強度指示
export function getPasswordStrength(password: string) {
  const checks = [
    password.length >= 8,
    /[A-Z]/.test(password),
    /[a-z]/.test(password),
    /[0-9!@#$%^&*]/.test(password),
  ]
  
  const passed = checks.filter(Boolean).length
  
  if (passed <= 2) return { level: 'weak', color: 'red' }
  if (passed <= 3) return { level: 'fair', color: 'orange' }
  if (passed <= 4) return { level: 'good', color: 'green' }
  return { level: 'strong', color: 'green' }
}
```

### 新增管理員重置密碼對話框

新增 `src/features/users/components/admin-reset-password-dialog.tsx`：

```tsx
'use client'

import { useCallback } from 'react'
import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import type { User } from '@/types/user'
import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from '@/components/ui/form'
import { PasswordInput } from '@/components/password-input'
import { useUsers } from './users-provider'

const adminResetPasswordSchema = z.object({
  newPassword: z
    .string()
    .min(8, '密碼長度需至少 8 個字元')
    .max(72, '密碼長度不能超過 72 個字元'),
  confirmPassword: z.string(),
})
  .refine((data) => data.newPassword === data.confirmPassword, {
    message: '密碼不一致',
    path: ['confirmPassword'],
  })

type AdminResetPasswordInput = z.infer<typeof adminResetPasswordSchema>

type AdminResetPasswordDialogProps = {
  user: User
  open: boolean
  onOpenChange: (open: boolean) => void
}

export function AdminResetPasswordDialog({
  user,
  open,
  onOpenChange,
}: AdminResetPasswordDialogProps) {
  const form = useForm<AdminResetPasswordInput>({
    resolver: zodResolver(adminResetPasswordSchema),
    defaultValues: { newPassword: '', confirmPassword: '' },
  })

  const onSubmit = useCallback(
    (values: AdminResetPasswordInput) => {
      // 調用 API 重置密碼
      // await usersApi.adminResetPassword(user.id, { newPassword: values.newPassword })
      form.reset()
      onOpenChange(false)
    },
    [form, onOpenChange]
  )

  return (
    <Dialog
      open={open}
      onOpenChange={(state) => {
        form.reset()
        onOpenChange(state)
      }}
    >
      <DialogContent className='sm:max-w-md'>
        <DialogHeader className='text-start'>
          <DialogTitle>重置密碼</DialogTitle>
          <DialogDescription>
            為 {user.username} 設置新密碼
          </DialogDescription>
        </DialogHeader>
        <Form {...form}>
          <form onSubmit={form.handleSubmit(onSubmit)} className='space-y-4'>
            <FormField
              control={form.control}
              name='newPassword'
              render={({ field }) => (
                <FormItem>
                  <FormLabel>新密碼</FormLabel>
                  <FormControl>
                    <PasswordInput
                      autoComplete='new-password'
                      placeholder='至少 8 個字元'
                      {...field}
                    />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name='confirmPassword'
              render={({ field }) => (
                <FormItem>
                  <FormLabel>確認密碼</FormLabel>
                  <FormControl>
                    <PasswordInput
                      autoComplete='new-password'
                      placeholder='再次輸入密碼'
                      {...field}
                    />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <DialogFooter>
              <Button type='submit'>重置</Button>
            </DialogFooter>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  )
}
```

### 修改行操作菜單

在 `src/features/users/components/data-table-row-actions.tsx` 中添加重置密碼與解鎖選項：

```tsx
export function DataTableRowActions({ row }: { row: Row<User> }) {
  const { setOpen, setCurrentRow } = useUsers()
  
  return (
    <DropdownMenu modal={false}>
      {/* 既有菜單項... */}
      
      {/* 新增：重置密碼 */}
      <DropdownMenuItem
        onClick={() => {
          setCurrentRow(row.original)
          setOpen('admin-reset-password')
        }}
      >
        重置密碼
      </DropdownMenuItem>
      
      {/* 新增：如果賬戶被鎖定，顯示解鎖選項 */}
      {!row.original.accountNonLocked && (
        <DropdownMenuItem
          onClick={async () => {
            // await usersApi.unlockUser(row.original.id)
            setCurrentRow(row.original)
          }}
        >
          解鎖賬戶
        </DropdownMenuItem>
      )}
    </DropdownMenu>
  )
}
```

### 更新 Sign-in 頁面

在 `src/features/auth/sign-in/components/user-auth-form.tsx` 中移除"忘記密碼"連結。

---

## 🐛 已知問題與故障排除

### SQLite DDL 遷移失敗 - NOT NULL 列錯誤

#### 問題描述
當向現有的 `users` 表添加新的密碼管理欄位時，Hibernate 的自動 DDL（`spring.jpa.hibernate.ddl-auto=update`）會在 SQLite 中失敗，報告以下錯誤：

```
Error executing DDL "alter table users add column failed_login_attempts integer not null" 
via JDBC [[SQLITE_ERROR] SQL error or missing database 
(Cannot add a NOT NULL column with default value NULL)]
```

**受影響的列：**
- `passwordChangedAt` (TIMESTAMP NOT NULL)
- `failedLoginAttempts` (INTEGER NOT NULL)
- `lastFailedLoginAt` (TIMESTAMP)
- `lastSuccessfulLoginAt` (TIMESTAMP)

#### 根本原因
SQLite 不支持向現有表添加 NOT NULL 列（不帶默認值）。SQLite 的行為不同於 MySQL 或 PostgreSQL，它要求：
- 新增的 NOT NULL 列必須有默認值
- 或者不能是 NOT NULL

#### 解決方案

**推薦方案：刪除並重建數據庫**

在開發環境中，簡單的解決方法是刪除舊的 SQLite 數據庫文件，讓 Hibernate 重新創建新的表結構：

```powershell
# 停止後端應用
Get-Process java -ErrorAction SilentlyContinue | Stop-Process -Force

# 刪除舊的數據庫文件
Remove-Item -Path "sromantics.db" -Force -ErrorAction SilentlyContinue

# 重啟後端
cd sromantics-api
.\mvnw spring-boot:run
```

Hibernate 會自動使用 `ddl-auto=update` 從頭開始創建新表，所有新欄位都會被正確創建。

**生產環境方案：使用 Flyway Migration**

對於生產環境，應使用顯式的數據庫遷移工具如 Flyway 或 Liquibase，而不是依賴 Hibernate 的 DDL 自動生成：

創建 `src/main/resources/db/migration/V004__add_password_fields.sql`：

```sql
-- 如果表不存在，先創建新表
-- 或使用以下替代方案：將現有數據轉移到新表

-- 創建臨時表
CREATE TABLE users_new AS 
SELECT 
    id, username, email, password_hash, display_name, roles, 
    enabled, account_non_locked, token_version, created_at, updated_at,
    CURRENT_TIMESTAMP as password_changed_at,
    0 as failed_login_attempts,
    NULL as last_failed_login_at,
    NULL as last_successful_login_at
FROM users;

-- 刪除舊表
DROP TABLE users;

-- 重命名新表
ALTER TABLE users_new RENAME TO users;

-- 重建約束
CREATE UNIQUE INDEX uk_users_username ON users(username);
CREATE UNIQUE INDEX uk_users_email ON users(email);
```

#### 驗證問題已解決

重啟後端應用後，檢查以下指標：

1. **應用啟動成功**
   ```
   Started SromanticsApiApplication in X.XXX seconds
   ```

2. **API 可用**
   ```bash
   curl http://localhost:8080/api/users
   ```

3. **返回 HTTP 200 響應**
   ```json
   {
     "value": [
       {
         "id": "xxx",
         "username": "admin",
         "email": "admin@example.com",
         ...
       }
     ]
   }
   ```

#### 預防措施

- **開發環境：** 每次 Entity 重大更改時刪除 `sromantics.db`
- **生產環境：** 使用 Flyway/Liquibase 管理所有 DDL 變更
- **配置檔：** 考慮在不同環境使用不同的 `ddl-auto` 設定：
  ```properties
  # application-dev.properties
  spring.jpa.hibernate.ddl-auto=create-drop
  
  # application-prod.properties
  spring.jpa.hibernate.ddl-auto=validate
  spring.flyway.enabled=true
  ```

---

## 測試用例

### 後端單元測試

```java
@ExtendWith(MockitoExtension.class)
class PasswordManagementTest {
    
    @InjectMocks
    private PasswordStrengthValidator validator;
    
    @Test
    void validate_acceptsValidPassword() {
        validator.validate("Password123!");  // ✓ 通過
    }
    
    @Test
    void validate_rejectsPasswordWithoutUppercase() {
        assertThatThrownBy(() -> validator.validate("password123!"))
            .isInstanceOf(InvalidPasswordException.class);
    }
    
    @Test
    void changePassword_requiresCorrectCurrentPassword() {
        // 測試當前密碼驗證
    }
    
    @Test
    void recordFailedLogin_locksAccountAfterFive() {
        User user = new User();
        for (int i = 0; i < 5; i++) {
            user.recordFailedLoginAttempt();
        }
        assertThat(user.isAccountNonLocked()).isFalse();
    }
    
    @Test
    void isTemporarilyLocked_expiresAfter30Minutes() {
        User user = new User();
        user.setAccountNonLocked(false);
        user.setLastFailedLoginAt(Instant.now().minus(Duration.ofMinutes(31)));
        
        assertThat(user.isTemporarilyLocked()).isFalse();
    }
}
```

---

## HTTP 錯誤約定

| 狀況 | Status | 說明 |
|------|--------|------|
| 密碼強度不符 | `400` | InvalidPasswordException 發生 |
| 當前密碼錯誤 | `401` | changePassword 時驗證失敗 |
| 使用者不存在 | `404` | id 查詢失敗 |
| 非 ADMIN 的管理操作 | `403` | 無 ADMIN 角色調用 admin 端點 |
| JWT 過期 | `401` | 登入時密碼改變導致 token 失效 |

---

## 安全考量

- [x] 密碼永不以明文存儲或記錄
- [x] 密碼 hash 使用 BCrypt
- [x] 登入失敗 5 次後自動鎖定 30 分鐘
- [x] 管理員可手動解鎖
- [x] 密碼變更後強制重新認證 (tokenVersion++)
- [x] 用戶自己改密碼需驗證當前密碼
- [x] 管理員重置不需要驗證當前密碼

---

## 實現順序

1. **Database Migration**: 添加 4 個欄位
2. **User Entity**: 添加方法 (recordFailedLoginAttempt 等)
3. **PasswordStrengthValidator**: 密碼驗證
4. **ChangePasswordRequest**: 修改 DTO
5. **AdminResetPasswordRequest**: 新增 DTO
6. **UserService**: 改進密碼相關方法
7. **UserController**: 添加管理員端點
8. **前端組件**: 新增對話框與菜單項
9. **單元測試**: 測試密碼邏輯
