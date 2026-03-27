package com.example.demo.entity;

/**
 * Статус сессии пользователя.
 */
public enum SessionStatus {
    /** Сессия активна, refresh-токен можно использовать */
    ACTIVE,
    /** Сессия завершена (logout или refresh использован) */
    REVOKED,
    /** Сессия истекла по времени */
    EXPIRED
}
