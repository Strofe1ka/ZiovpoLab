package ru.ziovpo.backend.license;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LicenseTypeRepository extends JpaRepository<LicenseTypeEntity, UUID> {
}
