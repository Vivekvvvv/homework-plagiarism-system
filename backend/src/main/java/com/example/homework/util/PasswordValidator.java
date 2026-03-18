package com.example.homework.util;

import com.example.homework.common.exception.BusinessException;
import com.example.homework.common.exception.ErrorCodes;

import java.util.regex.Pattern;

/**
 * Password strength validation utility.
 */
public final class PasswordValidator {

    private static final int MIN_LENGTH = 8;
    private static final Pattern UPPERCASE = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE = Pattern.compile("[a-z]");
    private static final Pattern DIGIT = Pattern.compile("[0-9]");
    private static final Pattern SPECIAL = Pattern.compile("[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?~`]");

    private PasswordValidator() {
    }

    /**
     * Validate password strength. Throws BusinessException if invalid.
     */
    public static void validate(String password) {
        if (password == null || password.length() < MIN_LENGTH) {
            throw new BusinessException(ErrorCodes.BAD_REQUEST,
                "密码长度至少" + MIN_LENGTH + "位");
        }
        if (!UPPERCASE.matcher(password).find()) {
            throw new BusinessException(ErrorCodes.BAD_REQUEST,
                "密码必须包含至少一个大写字母");
        }
        if (!LOWERCASE.matcher(password).find()) {
            throw new BusinessException(ErrorCodes.BAD_REQUEST,
                "密码必须包含至少一个小写字母");
        }
        if (!DIGIT.matcher(password).find()) {
            throw new BusinessException(ErrorCodes.BAD_REQUEST,
                "密码必须包含至少一个数字");
        }
        if (!SPECIAL.matcher(password).find()) {
            throw new BusinessException(ErrorCodes.BAD_REQUEST,
                "密码必须包含至少一个特殊字符");
        }
    }

    /**
     * Check if password meets strength requirements without throwing.
     */
    public static boolean isValid(String password) {
        try {
            validate(password);
            return true;
        } catch (BusinessException e) {
            return false;
        }
    }
}
