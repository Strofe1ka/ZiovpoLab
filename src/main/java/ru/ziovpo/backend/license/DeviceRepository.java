package ru.ziovpo.backend.license;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceRepository extends JpaRepository<DeviceEntity, UUID> {

    Optional<DeviceEntity> findByUser_IdAndMacAddressIgnoreCase(UUID userId, String macAddress);
}
