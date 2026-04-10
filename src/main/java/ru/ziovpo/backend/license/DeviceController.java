package ru.ziovpo.backend.license;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.ziovpo.backend.license.dto.DeviceRegisteredResponse;
import ru.ziovpo.backend.license.dto.RegisterDeviceRequest;
import ru.ziovpo.backend.security.AppUserPrincipal;

@RestController
@RequestMapping("/api/devices")
public class DeviceController {

    private final LicenseService licenseService;

    public DeviceController(LicenseService licenseService) {
        this.licenseService = licenseService;
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public DeviceRegisteredResponse register(
            @Valid @RequestBody RegisterDeviceRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal) {
        return licenseService.registerDevice(request, principal);
    }
}
