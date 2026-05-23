package ru.ziovpo.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.util.Date;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    @Value("${security.jwt.access-secret}")
    private String accessSecret;

    @Value("${security.jwt.refresh-secret}")
    private String refreshSecret;

    @Value("${security.jwt.access-expiration-ms}")
    private long accessExpirationMs;

    @Value("${security.jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    public String generateAccessToken(UserDetails userDetails) {
        return generateToken(userDetails.getUsername(), accessSecret, accessExpirationMs);
    }

    public String generateRefreshToken(UserDetails userDetails) {
        return generateToken(userDetails.getUsername(), refreshSecret, refreshExpirationMs);
    }

    public String extractUsernameFromAccess(String token) {
        return extractAllClaims(token, accessSecret).getSubject();
    }

    public String extractUsernameFromRefresh(String token) {
        return extractAllClaims(token, refreshSecret).getSubject();
    }

    public boolean isAccessTokenValid(String token, UserDetails userDetails) {
        String username = extractUsernameFromAccess(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token, accessSecret);
    }

    public boolean isRefreshTokenValid(String token, UserDetails userDetails) {
        String username = extractUsernameFromRefresh(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token, refreshSecret);
    }

    private String generateToken(String subject, String secret, long expirationMs) {
        Date now = new Date();
        return Jwts.builder()
                .subject(subject)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(getSignInKey(secret))
                .compact();
    }

    private Claims extractAllClaims(String token, String secret) {
        return Jwts.parser()
                .verifyWith((javax.crypto.SecretKey) getSignInKey(secret))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private boolean isTokenExpired(String token, String secret) {
        return extractAllClaims(token, secret).getExpiration().before(new Date());
    }

    private Key getSignInKey(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT secret is not configured");
        }
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
