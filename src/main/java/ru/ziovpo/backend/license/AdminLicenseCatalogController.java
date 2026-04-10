package ru.ziovpo.backend.license;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.ziovpo.backend.license.dto.CreateLicenseTypeRequest;
import ru.ziovpo.backend.license.dto.CreateProductRequest;
import ru.ziovpo.backend.license.dto.LicenseTypeResponse;
import ru.ziovpo.backend.license.dto.ProductResponse;

@RestController
@RequestMapping("/api/admin/catalog")
public class AdminLicenseCatalogController {

    private final LicenseService licenseService;

    public AdminLicenseCatalogController(LicenseService licenseService) {
        this.licenseService = licenseService;
    }

    @PostMapping("/products")
    public ProductResponse createProduct(@Valid @RequestBody CreateProductRequest request) {
        return licenseService.createProduct(request);
    }

    @PostMapping("/license-types")
    public LicenseTypeResponse createLicenseType(@Valid @RequestBody CreateLicenseTypeRequest request) {
        return licenseService.createLicenseType(request);
    }
}
