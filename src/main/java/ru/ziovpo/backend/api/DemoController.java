package ru.ziovpo.backend.api;

import java.util.Map;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.ziovpo.backend.security.AppUserPrincipal;

@RestController
@RequestMapping("/api")
public class DemoController {

    @GetMapping("/me")
    public Map<String, String> me(@AuthenticationPrincipal AppUserPrincipal principal) {
        UUID id = principal.getId();
        return Map.of("id", id.toString(), "username", principal.getUsername());
    }

    @GetMapping("/profile")
    public String profile() {
        return "Authenticated profile endpoint";
    }

    @GetMapping("/admin/ping")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminPing() {
        return "Admin endpoint";
    }
}
