package ru.ziovpo.backend.license;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.ziovpo.backend.config.LicenseProperties;
import ru.ziovpo.backend.license.dto.ActivateLicenseRequest;
import ru.ziovpo.backend.license.dto.CreateLicenseRequest;
import ru.ziovpo.backend.license.dto.CreateLicenseTypeRequest;
import ru.ziovpo.backend.license.dto.CreateProductRequest;
import ru.ziovpo.backend.license.dto.DeviceRegisteredResponse;
import ru.ziovpo.backend.license.dto.LicenseCreatedResponse;
import ru.ziovpo.backend.license.dto.LicenseTypeResponse;
import ru.ziovpo.backend.license.dto.ProductResponse;
import ru.ziovpo.backend.license.dto.RegisterDeviceRequest;
import ru.ziovpo.backend.license.dto.RenewLicenseRequest;
import ru.ziovpo.backend.license.dto.RenewLicenseResponse;
import ru.ziovpo.backend.license.dto.VerifyLicenseRequest;
import ru.ziovpo.backend.license.ticket.Ticket;
import ru.ziovpo.backend.license.ticket.TicketResponse;
import ru.ziovpo.backend.license.ticket.TicketSignatureService;
import ru.ziovpo.backend.security.AppUserPrincipal;
import ru.ziovpo.backend.user.UserEntity;
import ru.ziovpo.backend.user.UserRepository;

@Service
public class LicenseService {

    private static final String H_CREATED = "CREATED";
    private static final String H_ACTIVATED = "ACTIVATED";
    private static final String H_VERIFIED = "VERIFIED";
    private static final String H_RENEWED = "RENEWED";

    private final LicenseRepository licenseRepository;
    private final LicenseHistoryRepository licenseHistoryRepository;
    private final DeviceLicenseRepository deviceLicenseRepository;
    private final DeviceRepository deviceRepository;
    private final ProductRepository productRepository;
    private final LicenseTypeRepository licenseTypeRepository;
    private final UserRepository userRepository;
    private final TicketSignatureService ticketSignatureService;
    private final LicenseProperties licenseProperties;

    public LicenseService(LicenseRepository licenseRepository,
                          LicenseHistoryRepository licenseHistoryRepository,
                          DeviceLicenseRepository deviceLicenseRepository,
                          DeviceRepository deviceRepository,
                          ProductRepository productRepository,
                          LicenseTypeRepository licenseTypeRepository,
                          UserRepository userRepository,
                          TicketSignatureService ticketSignatureService,
                          LicenseProperties licenseProperties) {
        this.licenseRepository = licenseRepository;
        this.licenseHistoryRepository = licenseHistoryRepository;
        this.deviceLicenseRepository = deviceLicenseRepository;
        this.deviceRepository = deviceRepository;
        this.productRepository = productRepository;
        this.licenseTypeRepository = licenseTypeRepository;
        this.userRepository = userRepository;
        this.ticketSignatureService = ticketSignatureService;
        this.licenseProperties = licenseProperties;
    }

    @Transactional
    public LicenseCreatedResponse createLicense(CreateLicenseRequest request, AppUserPrincipal actor) {
        UserEntity holder = userRepository.findById(request.userId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Holder user not found"));
        UserEntity owner = request.ownerId() != null
                ? userRepository.findById(request.ownerId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Owner user not found"))
                : holder;
        ProductEntity product = productRepository.findById(request.productId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product not found"));
        LicenseTypeEntity type = licenseTypeRepository.findById(request.typeId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "License type not found"));

        if (product.isBlocked()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product is blocked");
        }

        LicenseEntity license = new LicenseEntity();
        license.setCode(generateUniqueCode());
        license.setUser(holder);
        license.setOwner(owner);
        license.setProduct(product);
        license.setType(type);
        license.setDeviceCount(request.deviceCount());
        license.setDescription(request.description());
        license.setEndingDate(request.endingDate());
        license.setBlocked(false);
        licenseRepository.save(license);

        appendHistory(license, actor.getId(), H_CREATED, "License issued");
        return new LicenseCreatedResponse(license.getId(), license.getCode());
    }

    @Transactional
    public TicketResponse activate(ActivateLicenseRequest request, AppUserPrincipal actor) {
        LicenseEntity license = licenseRepository.findDetailedByCode(request.code().trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "License not found"));
        ensureHolderOrAdmin(license, actor);

        DeviceEntity device = deviceRepository.findById(request.deviceId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Device not found"));
        if (!device.getUser().getId().equals(license.getUser().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Device does not belong to license holder");
        }

        if (license.isBlocked() || license.getProduct().isBlocked()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "License or product is blocked");
        }

        LocalDate today = LocalDate.now();

        if (license.getFirstActivationDate() == null
                && license.getEndingDate() != null
                && license.getEndingDate().isBefore(today)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "License ending date is already in the past");
        }

        if (license.getFirstActivationDate() == null) {
            license.setFirstActivationDate(today);
            if (license.getEndingDate() == null) {
                license.setEndingDate(today.plusDays(license.getType().getDefaultDurationInDays()));
            }
            licenseRepository.save(license);
        } else if (license.getEndingDate() != null && license.getEndingDate().isBefore(today)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "License has expired");
        }

        if (deviceLicenseRepository.existsByLicenseAndDevice(license, device)) {
            return ticketSignatureService.signToResponse(buildTicket(license, device.getId()));
        }

        long activeDevices = deviceLicenseRepository.countByLicense(license);
        if (activeDevices >= license.getDeviceCount()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Device limit reached for this license");
        }

        DeviceLicenseEntity link = new DeviceLicenseEntity();
        link.setLicense(license);
        link.setDevice(device);
        link.setActivationDate(today);
        deviceLicenseRepository.save(link);

        appendHistory(license, actor.getId(), H_ACTIVATED, "License activated on device");
        return ticketSignatureService.signToResponse(buildTicket(license, device.getId()));
    }

    @Transactional
    public TicketResponse verify(VerifyLicenseRequest request, AppUserPrincipal actor) {
        LicenseEntity license = licenseRepository.findDetailedByCode(request.code().trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "License not found"));
        ensureHolderOrAdmin(license, actor);

        DeviceEntity device = deviceRepository.findById(request.deviceId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Device not found"));
        if (!device.getUser().getId().equals(license.getUser().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Device does not belong to license holder");
        }

        deviceLicenseRepository.findByLicenseAndDevice(license, device)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "License is not active on this device"));

        if (license.isBlocked() || license.getProduct().isBlocked()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "License or product is blocked");
        }

        LocalDate today = LocalDate.now();
        if (license.getEndingDate() != null && license.getEndingDate().isBefore(today)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "License has expired");
        }

        appendHistory(license, actor.getId(), H_VERIFIED, "License verified");
        return ticketSignatureService.signToResponse(buildTicket(license, device.getId()));
    }

    @Transactional
    public RenewLicenseResponse renew(RenewLicenseRequest request, AppUserPrincipal actor) {
        LicenseEntity license = licenseRepository.findDetailedByCode(request.code().trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "License not found"));
        ensureOwnerOrAdmin(license, actor);

        int days = request.extendDays() != null
                ? request.extendDays()
                : license.getType().getDefaultDurationInDays();

        LocalDate today = LocalDate.now();
        LocalDate currentEnd = license.getEndingDate();
        LocalDate base = currentEnd == null ? today : currentEnd.isBefore(today) ? today : currentEnd;
        license.setEndingDate(base.plusDays(days));
        licenseRepository.save(license);

        appendHistory(license, actor.getId(), H_RENEWED, "License renewed, +" + days + " days");
        return new RenewLicenseResponse(license.getId(), license.getEndingDate());
    }

    @Transactional
    public DeviceRegisteredResponse registerDevice(RegisterDeviceRequest request, AppUserPrincipal actor) {
        UserEntity user = userRepository.getReferenceById(actor.getId());
        DeviceEntity device = new DeviceEntity();
        device.setUser(user);
        device.setName(request.name());
        device.setMacAddress(request.macAddress());
        deviceRepository.save(device);
        return new DeviceRegisteredResponse(device.getId(), device.getName(), device.getMacAddress());
    }

    @Transactional
    public ProductResponse createProduct(CreateProductRequest request) {
        ProductEntity p = new ProductEntity();
        p.setName(request.name());
        p.setBlocked(false);
        productRepository.save(p);
        return new ProductResponse(p.getId(), p.getName(), p.isBlocked());
    }

    @Transactional
    public LicenseTypeResponse createLicenseType(CreateLicenseTypeRequest request) {
        LicenseTypeEntity t = new LicenseTypeEntity();
        t.setName(request.name());
        t.setDefaultDurationInDays(request.defaultDurationInDays());
        t.setDescription(request.description());
        licenseTypeRepository.save(t);
        return new LicenseTypeResponse(t.getId(), t.getName(), t.getDefaultDurationInDays(), t.getDescription());
    }

    private Ticket buildTicket(LicenseEntity license, UUID deviceId) {
        long ttl = licenseProperties.getTicket().getTtlSeconds();
        return new Ticket(
                Instant.now(),
                ttl,
                license.getFirstActivationDate(),
                license.getEndingDate(),
                license.getUser().getId(),
                deviceId,
                license.isBlocked()
        );
    }

    private void appendHistory(LicenseEntity license, UUID actorUserId, String status, String description) {
        LicenseHistoryEntity row = new LicenseHistoryEntity();
        row.setLicense(license);
        row.setUser(userRepository.getReferenceById(actorUserId));
        row.setStatus(status);
        row.setChangeDate(LocalDate.now());
        row.setDescription(description);
        licenseHistoryRepository.save(row);
    }

    private String generateUniqueCode() {
        for (int i = 0; i < 64; i++) {
            String code = UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase(Locale.ROOT);
            if (!licenseRepository.existsByCodeIgnoreCase(code)) {
                return code;
            }
        }
        throw new IllegalStateException("Could not allocate license code");
    }

    private static void ensureHolderOrAdmin(LicenseEntity license, AppUserPrincipal actor) {
        if (actor.isAdmin()) {
            return;
        }
        if (!license.getUser().getId().equals(actor.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed for this license");
        }
    }

    private static void ensureOwnerOrAdmin(LicenseEntity license, AppUserPrincipal actor) {
        if (actor.isAdmin()) {
            return;
        }
        if (!license.getOwner().getId().equals(actor.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only license owner or admin can renew");
        }
    }
}
