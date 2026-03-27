package com.example.demo.security;

import com.example.demo.entity.User;
import com.example.demo.entity.UserRole;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

/**
 * Провайдер JWT-токенов с раздельной логикой для access и refresh токенов.
 */
@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    public static final String CLAIM_TYPE = "type";
    public static final String CLAIM_USER_ID = "userId";
    public static final String CLAIM_USERNAME = "sub";
    public static final String CLAIM_ROLE = "role";
    public static final String CLAIM_SESSION_ID = "sessionId";
    public static final String CLAIM_JTI = "jti";

    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";

    private final SecretKey secretKey;
    private final long accessExpirationMs;
    private final long refreshExpirationMs;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-expiration-ms}") long accessExpirationMs,
            @Value("${jwt.refresh-expiration-ms}") long refreshExpirationMs) {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalArgumentException("jwt.secret должен быть не менее 32 символов");
        }
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        this.accessExpirationMs = accessExpirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    /**
     * Генерирует access-токен для пользователя.
     */
    public String createAccessToken(User user) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + accessExpirationMs);

        return Jwts.builder()
                .subject(user.getUsername())
                .claim(CLAIM_TYPE, TYPE_ACCESS)
                .claim(CLAIM_USER_ID, user.getId())
                .claim(CLAIM_ROLE, user.getRole().name())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    /**
     * Генерирует refresh-токен для сессии.
     */
    public String createRefreshToken(User user, Long sessionId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + refreshExpirationMs);
        String jti = UUID.randomUUID().toString();

        return Jwts.builder()
                .subject(user.getUsername())
                .claim(CLAIM_TYPE, TYPE_REFRESH)
                .claim(CLAIM_USER_ID, user.getId())
                .claim(CLAIM_SESSION_ID, sessionId)
                .claim(CLAIM_JTI, jti)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    /**
     * Валидирует access-токен и возвращает claims.
     */
    public Claims validateAccessToken(String token) {
        return validateToken(token, TYPE_ACCESS);
    }

    /**
     * Валидирует refresh-токен и возвращает claims.
     */
    public Claims validateRefreshToken(String token) {
        return validateToken(token, TYPE_REFRESH);
    }

    private Claims validateToken(String token, String expectedType) {
        if (token == null || token.isBlank()) {
            throw new JwtException("Токен отсутствует");
        }
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String type = claims.get(CLAIM_TYPE, String.class);
            if (type == null || !type.equals(expectedType)) {
                throw new JwtException("Неверный тип токена: ожидается " + expectedType);
            }
            return claims;
        } catch (ExpiredJwtException e) {
            log.debug("Токен истёк: {}", e.getMessage());
            throw new JwtException("Токен истёк");
        } catch (SignatureException e) {
            log.debug("Неверная подпись токена");
            throw new JwtException("Неверная подпись токена");
        } catch (JwtException e) {
            log.debug("Ошибка валидации токена: {}", e.getMessage());
            throw e;
        }
    }

    public Long getSessionIdFromRefreshToken(String token) {
        Claims claims = validateRefreshToken(token);
        Object sessionId = claims.get(CLAIM_SESSION_ID);
        if (sessionId == null) return null;
        if (sessionId instanceof Number) return ((Number) sessionId).longValue();
        return null;
    }
}
