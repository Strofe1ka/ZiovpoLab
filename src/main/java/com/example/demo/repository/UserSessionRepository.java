package com.example.demo.repository;

import com.example.demo.entity.SessionStatus;
import com.example.demo.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserSessionRepository extends JpaRepository<UserSession, Long> {

    Optional<UserSession> findByRefreshTokenHashAndStatus(String refreshTokenHash, SessionStatus status);

    boolean existsByRefreshTokenHashAndStatus(String refreshTokenHash, SessionStatus status);
}
