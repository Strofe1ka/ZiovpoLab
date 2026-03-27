package com.example.demo.controller;

import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.RefreshRequest;
import com.example.demo.dto.RegisterRequest;
import com.example.demo.entity.User;
import com.example.demo.entity.UserRole;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.AuthTokenService;
import com.example.demo.service.PasswordValidationService;
import com.example.demo.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.util.List;
import java.util.Map;

/**
 * Контроллер аутентификации и регистрации.
 */
@RestController
public class AuthController {
    private final UserService userService;
    private final UserRepository userRepository;
    private final PasswordValidationService passwordValidation;
    private final AuthTokenService authTokenService;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserService userService, UserRepository userRepository,
                          PasswordValidationService passwordValidation,
                          AuthTokenService authTokenService, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.passwordValidation = passwordValidation;
        this.authTokenService = authTokenService;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/")
    public RedirectView home() {
        return new RedirectView("/register");
    }

    @GetMapping(value = "/register", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> registerPage() {
        String html = """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"><title>Регистрация — Carsharing</title>
            <style>
                body { font-family: system-ui; max-width: 400px; margin: 50px auto; padding: 20px; }
                input { width: 100%; padding: 8px; margin: 5px 0; box-sizing: border-box; }
                button { padding: 10px 20px; background: #2563eb; color: white; border: none; cursor: pointer; }
                .error { color: red; font-size: 14px; }
                .success { color: green; }
            </style>
            </head>
            <body>
            <h1>Регистрация</h1>
            <form id="regForm">
                <input type="text" name="firstName" placeholder="Имя" required><br>
                <input type="text" name="lastName" placeholder="Фамилия" required><br>
                <input type="email" name="email" placeholder="Email" required><br>
                <input type="text" name="username" placeholder="Логин" required><br>
                <input type="password" name="password" placeholder="Пароль (8+ символов, заглавная, цифра, спецсимвол)" required><br>
                <input type="text" name="phone" placeholder="Телефон (необязательно)"><br>
                <button type="submit">Зарегистрироваться</button>
            </form>
            <div id="msg"></div>
            <script>
                document.getElementById('regForm').onsubmit = async (e) => {
                    e.preventDefault();
                    const fd = new FormData(e.target);
                    const data = Object.fromEntries(fd);
                    const msg = document.getElementById('msg');
                    try {
                        const r = await fetch('/register', {
                            method: 'POST',
                            headers: {'Content-Type': 'application/json'},
                            body: JSON.stringify(data)
                        });
                        const json = await r.json();
                        if (r.ok) {
                            msg.className = 'success';
                            msg.textContent = 'Регистрация успешна! Логин: ' + json.username;
                        } else {
                            msg.className = 'error';
                            msg.textContent = json.error || json.errors?.join(', ') || 'Ошибка';
                        }
                    } catch (err) {
                        msg.className = 'error';
                        msg.textContent = 'Ошибка: ' + err.message;
                    }
                };
            </script>
            </body>
            </html>
            """;
        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html);
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        List<String> passwordErrors = passwordValidation.validate(request.getPassword());
        if (!passwordErrors.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("errors", passwordErrors));
        }

        if (userService.existsByUsername(request.getUsername())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Логин уже занят"));
        }
        if (userService.existsByEmail(request.getEmail())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email уже зарегистрирован"));
        }

        User user = new User();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setUsername(request.getUsername());
        user.setPassword(request.getPassword()); // UserService.createUser закодирует
        // Первый зарегистрированный пользователь получает роль ADMIN
        user.setRole(userService.hasAnyUsers() ? UserRole.USER : UserRole.ADMIN);

        User created = userService.createUser(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
            "id", created.getId(),
            "username", created.getUsername(),
            "message", "Регистрация успешна"
        ));
    }

    /**
     * Аутентификация по логину и паролю. Возвращает пару access и refresh токенов.
     */
    @PostMapping("/auth/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        User user = userRepository.findByUsername(request.getUsername().trim())
                .orElse(null);
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Неверный логин или пароль"));
        }
        String userAgent = httpRequest.getHeader("User-Agent");
        String ipAddress = httpRequest.getRemoteAddr();
        Map<String, String> tokens = authTokenService.createTokenPair(user, userAgent, ipAddress);
        return ResponseEntity.ok(tokens);
    }

    /**
     * Обновление пары токенов по refresh-токену.
     * Старый refresh-токен после использования становится недействительным.
     */
    @PostMapping("/auth/refresh")
    public ResponseEntity<?> refresh(@Valid @RequestBody RefreshRequest request, HttpServletRequest httpRequest) {
        try {
            String userAgent = httpRequest.getHeader("User-Agent");
            String ipAddress = httpRequest.getRemoteAddr();
            Map<String, String> tokens = authTokenService.refreshTokens(
                    request.getRefreshToken(), userAgent, ipAddress);
            return ResponseEntity.ok(tokens);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", e.getMessage()));
        } catch (io.jsonwebtoken.JwtException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Недействительный или истёкший refresh-токен"));
        }
    }
}
