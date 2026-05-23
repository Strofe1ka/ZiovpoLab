package ru.ziovpo.backend.license;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LicenseRepository extends JpaRepository<LicenseEntity, UUID> {

    @Query(
            """
                    SELECT DISTINCT l FROM LicenseEntity l
                    LEFT JOIN FETCH l.user
                    JOIN FETCH l.type
                    JOIN FETCH l.product
                    JOIN FETCH l.owner
                    WHERE lower(l.code) = lower(:code)
                    """
    )
    Optional<LicenseEntity> findDetailedByCode(@Param("code") String code);

    @Query(
            """
                    SELECT DISTINCT l FROM LicenseEntity l
                    JOIN FETCH l.type
                    JOIN FETCH l.product
                    JOIN FETCH l.user
                    JOIN FETCH l.owner
                    JOIN DeviceLicenseEntity dl ON dl.license = l
                    WHERE dl.device.id = :deviceId
                    AND l.user.id = :userId
                    AND l.product.id = :productId
                    AND l.blocked = false
                    AND (l.endingDate IS NULL OR l.endingDate >= :today)
                    """
    )
    Optional<LicenseEntity> findActiveByDeviceUserAndProduct(
            @Param("deviceId") UUID deviceId,
            @Param("userId") UUID userId,
            @Param("productId") UUID productId,
            @Param("today") LocalDate today);

    boolean existsByCodeIgnoreCase(String code);
}
