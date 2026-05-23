package ru.ziovpo.backend.license;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LicenseRepository extends JpaRepository<LicenseEntity, UUID> {
    @Query(
            """
                    SELECT DISTINCT l FROM LicenseEntity l
                    JOIN FETCH l.type
                    JOIN FETCH l.product
                    JOIN FETCH l.user
                    JOIN FETCH l.owner
                    WHERE lower(l.code) = lower(:code)
                    """
    )
    Optional<LicenseEntity> findDetailedByCode(@Param("code") String code);

    boolean existsByCodeIgnoreCase(String code);
}
