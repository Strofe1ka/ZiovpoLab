package com.example.demo.controller;

import com.example.demo.dto.RideRequest;
import com.example.demo.dto.RideResponse;
import com.example.demo.entity.Car;
import com.example.demo.entity.User;
import com.example.demo.entity.UserSession;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.UserSessionRepository;
import com.example.demo.service.CarService;
import com.example.demo.service.RideService;
import com.example.demo.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Временный контроллер для отладки паролей.
 * УДАЛИТЬ перед продакшеном!
 */
@RestController
@RequestMapping("/debug")
public class DebugController {
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final UserSessionRepository userSessionRepository;
    private final CarService carService;
    private final UserService userService;
    private final RideService rideService;

    public DebugController(PasswordEncoder passwordEncoder, UserRepository userRepository,
                           UserSessionRepository userSessionRepository, CarService carService,
                           UserService userService, RideService rideService) {
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.userSessionRepository = userSessionRepository;
        this.carService = carService;
        this.userService = userService;
        this.rideService = rideService;
    }

    /**
     * Генерирует BCrypt-хеш для пароля.
     */
    @PostMapping("/hash")
    public Map<String, String> hashPassword(@RequestBody Map<String, String> body) {
        String password = body.get("password");
        if (password == null) {
            return Map.of("error", "Укажите password в теле запроса");
        }
        String hash = passwordEncoder.encode(password);
        return Map.of("hash", hash, "password", password);
    }

    /**
     * Тест: требует JWT. Если вернёт "ok" — авторизация работает.
     * GET /debug/secure-test — с заголовком Authorization: Bearer &lt;accessToken&gt;
     */
    @GetMapping("/secure-test")
    public Map<String, String> secureTest() {
        return Map.of("status", "ok", "message", "Авторизация работает!");
    }

    /**
     * Тест POST с Basic Auth. Если работает — проблема в /cars, а не в POST.
     * POST /debug/secure-post — Body: {}, Headers: Authorization + Content-Type
     */
    @PostMapping("/secure-post")
    public Map<String, Object> securePost(@RequestHeader(value = "Authorization", required = false) String auth) {
        return Map.of(
            "status", "ok",
            "method", "POST",
            "authReceived", auth != null ? "да" : "НЕТ"
        );
    }

    /**
     * Прокси для создания авто — тот же функционал что POST /cars, но под /debug.
     */
    @PostMapping("/create-car")
    public Car createCar(@RequestBody Car car) {
        return carService.createCar(car);
    }

    /**
     * Прокси для users — обход 401 в Postman.
     */
    @PostMapping("/create-user")
    public User createUser(@RequestBody User user) {
        return userService.createUser(user);
    }

    @GetMapping("/users")
    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<User> getUserById(@PathVariable long id) {
        return userService.getUserById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/users/{id}/income")
    public Map<String, Double> getUserIncome(@PathVariable long id) {
        return Map.of("income", userService.getUserIncome(id));
    }

    /**
     * Прокси для rides — обход 401 в Postman.
     */
    @PostMapping("/start-ride")
    public String startRide(@RequestBody RideRequest request) {
        return rideService.startRide(request.getUserId(), request.getCarId(), request.getStartTime());
    }

    @GetMapping("/rides")
    public List<RideResponse> getAllRides() {
        return rideService.getAllRides();
    }

    @GetMapping("/rides/{id}")
    public ResponseEntity<RideResponse> getRideById(@PathVariable long id) {
        return rideService.getRideById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/rides/{id}/end")
    public ResponseEntity<String> endRide(@PathVariable long id, @RequestBody RideRequest request) {
        String result = rideService.endRide(id, request.getEndTime(), request.getDistance());
        return result.equals("Ride not found") ? ResponseEntity.notFound().build() : ResponseEntity.ok(result);
    }

    @PostMapping("/rides/{id}/pay")
    public ResponseEntity<?> payForRide(@PathVariable long id) {
        return rideService.payForRide(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Показывает заголовки запроса (для отладки Postman).
     * GET /debug/headers — без авторизации
     */
    @GetMapping("/headers")
    public Map<String, Object> showHeaders(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @RequestHeader(value = "Content-Type", required = false) String contentType) {
        return Map.of(
            "authorization", auth != null ? auth : "ОТСУТСТВУЕТ",
            "contentType", contentType != null ? contentType : "ОТСУТСТВУЕТ",
            "authLength", auth != null ? auth.length() : 0
        );
    }

    /**
     * Список сессий в БД (для проверки статусов).
     * GET /debug/sessions — с JWT access-токеном
     */
    @GetMapping("/sessions")
    public List<Map<String, Object>> getSessions() {
        return userSessionRepository.findAll().stream()
                .map(this::sessionToMap)
                .toList();
    }

    private Map<String, Object> sessionToMap(UserSession s) {
        return Map.of(
                "id", s.getId(),
                "userId", s.getUser().getId(),
                "username", s.getUser().getUsername(),
                "status", s.getStatus().name(),
                "createdAt", s.getCreatedAt().toString(),
                "expiresAt", s.getExpiresAt().toString(),
                "revokedAt", s.getRevokedAt() != null ? s.getRevokedAt().toString() : "null"
        );
    }

    /**
     * Проверяет, совпадает ли пароль с хешем в БД.
     * POST /debug/verify с телом {"username": "...", "password": "..."}
     */
    @PostMapping("/verify")
    public Map<String, Object> verifyPassword(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        if (username == null || password == null) {
            return Map.of("error", "Укажите username и password");
        }
        var userOpt = userRepository.findByUsername(username.trim());
        if (userOpt.isEmpty()) {
            return Map.of("found", false, "match", false, "message", "Пользователь не найден");
        }
        var user = userOpt.get();
        String storedHash = user.getPassword();
        boolean match = storedHash != null && passwordEncoder.matches(password, storedHash);
        return Map.of(
            "found", true,
            "match", match,
            "hashLength", storedHash != null ? storedHash.length() : 0,
            "userId", user.getId()
        );
    }
}
