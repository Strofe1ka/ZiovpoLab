package ru.ziovpo.backend.auth;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.ziovpo.backend.user.UserEntity;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, Long> {
    Optional<RefreshTokenEntity> findByToken(String token);
    void deleteByUser(UserEntity user);
}
