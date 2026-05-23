package ru.ziovpo.backend.license;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceLicenseRepository extends JpaRepository<DeviceLicenseEntity, UUID> {
    long countByLicense(LicenseEntity license);

    boolean existsByLicenseAndDevice(LicenseEntity license, DeviceEntity device);

    Optional<DeviceLicenseEntity> findByLicenseAndDevice(LicenseEntity license, DeviceEntity device);
}
