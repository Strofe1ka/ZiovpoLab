package com.example.demo.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Проверка надёжности пароля при регистрации.
 */
@Service
public class PasswordValidationService {
    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 100;
    private static final Pattern UPPERCASE = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE = Pattern.compile("[a-z]");
    private static final Pattern DIGIT = Pattern.compile("[0-9]");
    private static final Pattern SPECIAL = Pattern.compile("[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]");

    /**
     * Проверяет пароль на соответствие требованиям.
     * @return список сообщений об ошибках, пустой если пароль надёжен
     */
    public List<String> validate(String password) {
        List<String> errors = new ArrayList<>();

        if (password == null || password.isBlank()) {
            errors.add("Пароль не может быть пустым");
            return errors;
        }

        if (password.length() < MIN_LENGTH) {
            errors.add("Пароль должен содержать минимум " + MIN_LENGTH + " символов");
        }
        if (password.length() > MAX_LENGTH) {
            errors.add("Пароль не должен превышать " + MAX_LENGTH + " символов");
        }
        if (!UPPERCASE.matcher(password).find()) {
            errors.add("Пароль должен содержать хотя бы одну заглавную букву");
        }
        if (!LOWERCASE.matcher(password).find()) {
            errors.add("Пароль должен содержать хотя бы одну строчную букву");
        }
        if (!DIGIT.matcher(password).find()) {
            errors.add("Пароль должен содержать хотя бы одну цифру");
        }
        if (!SPECIAL.matcher(password).find()) {
            errors.add("Пароль должен содержать хотя бы один спецсимвол (!@#$%^&* и т.д.)");
        }

        return errors;
    }
}
