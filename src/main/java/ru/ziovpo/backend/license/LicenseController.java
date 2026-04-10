package ru.ziovpo.backend.license;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.ziovpo.backend.license.dto.ActivateLicenseRequest;
import ru.ziovpo.backend.license.dto.CreateLicenseRequest;
import ru.ziovpo.backend.license.dto.LicenseCreatedResponse;
import ru.ziovpo.backend.license.dto.RenewLicenseRequest;
import ru.ziovpo.backend.license.dto.RenewLicenseResponse;
import ru.ziovpo.backend.license.dto.VerifyLicenseRequest;
import ru.ziovpo.backend.license.ticket.TicketResponse;
import ru.ziovpo.backend.security.AppUserPrincipal;

@RestController
@RequestMapping("/api/licenses")
public class LicenseController {

    private final LicenseService licenseService;

    public LicenseController(LicenseService licenseService) {
        this.licenseService = licenseService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public LicenseCreatedResponse create(
            @Valid @RequestBody CreateLicenseRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal) {
        return licenseService.createLicense(request, principal);
    }

    @PostMapping("/activate")
    @PreAuthorize("isAuthenticated()")
    public TicketResponse activate(
            @Valid @RequestBody ActivateLicenseRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal) {
        return licenseService.activate(request, principal);
    }

    @PostMapping("/verify")
    @PreAuthorize("isAuthenticated()")
    public TicketResponse verify(
            @Valid @RequestBody VerifyLicenseRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal) {
        return licenseService.verify(request, principal);
    }

    @PostMapping("/renew")
    @PreAuthorize("isAuthenticated()")
    public RenewLicenseResponse renew(
            @Valid @RequestBody RenewLicenseRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal) {
        return licenseService.renew(request, principal);
    }
}
