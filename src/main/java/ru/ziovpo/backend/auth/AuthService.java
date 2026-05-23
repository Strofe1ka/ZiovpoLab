package ru.ziovpo.backend.auth;

import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.ziovpo.backend.auth.dto.LoginRequest;
import ru.ziovpo.backend.auth.dto.RefreshRequest;
import ru.ziovpo.backend.auth.dto.RegisterRequest;
import ru.ziovpo.backend.auth.dto.TokenResponse;
import ru.ziovpo.backend.security.AppUserPrincipal;
import ru.ziovpo.backend.security.JwtService;
import ru.ziovpo.backend.user.Role;
import ru.ziovpo.backend.user.UserEntity;
import ru.ziovpo.backend.user.UserRepository;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Value("${security.jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    public AuthService(UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Transactional
    public TokenResponse register(RegisterRequest request) {
        if (userRepository.existsByName(request.username())) {
            throw new IllegalArgumentException("Username already exists");
        }

        UserEntity user = new UserEntity();
        user.setName(request.username());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setEmail(request.email());
        user.setRole(userRepository.count() == 0 ? Role.ROLE_ADMIN : Role.ROLE_USER);
        userRepository.save(user);

        return issueTokens(user);
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );

        UserEntity user = userRepository.findByName(request.username())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return issueTokens(user);
    }

    @Transactional
    public TokenResponse refresh(RefreshRequest request) {
        RefreshTokenEntity tokenEntity = refreshTokenRepository.findByToken(request.refreshToken())
                .orElseThrow(() -> new IllegalArgumentException("Refresh token not found"));

        if (tokenEntity.getExpiresAt().isBefore(Instant.now())) {
            refreshTokenRepository.delete(tokenEntity);
            throw new IllegalArgumentException("Refresh token expired");
        }

        UserEntity user = tokenEntity.getUser();
        UserDetails userDetails = toPrincipal(user);

        if (!jwtService.isRefreshTokenValid(request.refreshToken(), userDetails)) {
            throw new IllegalArgumentException("Refresh token is invalid");
        }

        String accessToken = jwtService.generateAccessToken(userDetails);
        return new TokenResponse(accessToken, request.refreshToken());
    }

    private TokenResponse issueTokens(UserEntity user) {
        UserDetails userDetails = toPrincipal(user);

        String accessToken = jwtService.generateAccessToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        refreshTokenRepository.deleteByUser(user);
        RefreshTokenEntity entity = new RefreshTokenEntity();
        entity.setUser(user);
        entity.setToken(refreshToken);
        entity.setExpiresAt(Instant.now().plusMillis(refreshExpirationMs));
        refreshTokenRepository.save(entity);

        return new TokenResponse(accessToken, refreshToken);
    }

    private static AppUserPrincipal toPrincipal(UserEntity user) {
        return new AppUserPrincipal(
                user.getId(),
                user.getName(),
                user.getPasswordHash(),
                List.of(new SimpleGrantedAuthority(user.getRole().name())),
                !user.isAccountExpired(),
                !user.isAccountLocked(),
                !user.isCredentialsExpired(),
                !user.isDisabled()
        );
    }
}
