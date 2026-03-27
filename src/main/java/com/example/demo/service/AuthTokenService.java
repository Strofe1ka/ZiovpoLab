package com.example.demo.service;

import com.example.demo.entity.SessionStatus;
import com.example.demo.entity.User;
import com.example.demo.entity.UserSession;
import com.example.demo.repository.UserSessionRepository;
import com.example.demo.security.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

/**
 * Сервис работы с парами access/refresh токенов.
 */
@Service
public class AuthTokenService {

    private static final Logger log = LoggerFactory.getLogger(AuthTokenService.class);

    private final JwtTokenProvider jwtTokenProvider;
    private final UserSessionRepository userSessionRepository;

    private final long refreshExpirationMs;

    public AuthTokenService(JwtTokenProvider jwtTokenProvider,
                            UserSessionRepository userSessionRepository,
                            @Value("${jwt.refresh-expiration-ms}") long refreshExpirationMs) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userSessionRepository = userSessionRepository;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    /**
     * Создаёт новую сессию и пару токенов для пользователя.
     */
    @Transactional
    public Map<String, String> createTokenPair(User user, String userAgent, String ipAddress) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusMillis(refreshExpirationMs);

        UserSession session = new UserSession();
        session.setUser(user);
        session.setStatus(SessionStatus.ACTIVE);
        session.setCreatedAt(now);
        session.setExpiresAt(expiresAt);
        session.setUserAgent(userAgent);
        session.setIpAddress(ipAddress);
        session.setRefreshTokenHash(""); // временно, обновим после генерации токена

        session = userSessionRepository.save(session);

        String refreshToken = jwtTokenProvider.createRefreshToken(user, session.getId());
        session.setRefreshTokenHash(hashToken(refreshToken));
        userSessionRepository.save(session);

        String accessToken = jwtTokenProvider.createAccessToken(user);

        return Map.of(
                "accessToken", accessToken,
                "refreshToken", refreshToken,
                "tokenType", "Bearer",
                "expiresIn", String.valueOf(refreshExpirationMs / 1000)
        );
    }

    /**
     * Обновляет пару токенов по refresh-токену. Старая сессия помечается как REVOKED.
     */
    @Transactional
    public Map<String, String> refreshTokens(String refreshToken, String userAgent, String ipAddress) {
        var claims = jwtTokenProvider.validateRefreshToken(refreshToken);
        Long sessionId = jwtTokenProvider.getSessionIdFromRefreshToken(refreshToken);
        if (sessionId == null) {
            throw new IllegalArgumentException("Refresh-токен не содержит sessionId");
        }

        String tokenHash = hashToken(refreshToken);
        var sessionOpt = userSessionRepository.findByRefreshTokenHashAndStatus(tokenHash, SessionStatus.ACTIVE);
        if (sessionOpt.isEmpty()) {
            log.warn("Refresh-токен уже использован или сессия не найдена: sessionId={}", sessionId);
            throw new SecurityException("Refresh-токен недействителен или уже использован");
        }

        UserSession oldSession = sessionOpt.get();
        oldSession.setStatus(SessionStatus.REVOKED);
        oldSession.setRevokedAt(Instant.now());
        userSessionRepository.save(oldSession);

        User user = oldSession.getUser();
        return createTokenPair(user, userAgent, ipAddress);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
