package com.sromantics.sromantics_api.util;

import com.sromantics.sromantics_api.exception.InvalidPasswordException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 密碼強度驗證器
 * 要求：8-72 字符，包含大寫字母、小寫字母、數字或特殊字符
 */
@Component
public class PasswordStrengthValidator {

    public void validate(String password) throws InvalidPasswordException {
        List<String> errors = new ArrayList<>();

        if (password == null || password.length() < 8 || password.length() > 72) {
            errors.add("密碼長度需 8-72 字元");
        }
        if (password != null && !password.matches(".*[A-Z].*")) {
            errors.add("密碼必須包含大寫字母 (A-Z)");
        }
        if (password != null && !password.matches(".*[a-z].*")) {
            errors.add("密碼必須包含小寫字母 (a-z)");
        }
        if (password != null && !password.matches(".*[0-9!@#$%^&*].*")) {
            errors.add("密碼必須包含數字或特殊字符 (0-9 或 !@#$%^&*)");
        }

        if (!errors.isEmpty()) {
            throw new InvalidPasswordException(String.join("; ", errors));
        }
    }
}
