package ru.ziovpo.backend.license.ticket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Signature;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import ru.ziovpo.backend.config.LicenseProperties;

@Service
public class TicketSignatureService {

    private static final Logger log = LoggerFactory.getLogger(TicketSignatureService.class);

    private final LicenseProperties licenseProperties;
    private final ResourceLoader resourceLoader;
    private ObjectMapper ticketMapper;
    private PrivateKey privateKey;

    public TicketSignatureService(LicenseProperties licenseProperties, ResourceLoader resourceLoader) {
        this.licenseProperties = licenseProperties;
        this.resourceLoader = resourceLoader;
    }

    @PostConstruct
    void init() throws Exception {
        ticketMapper = new ObjectMapper();
        ticketMapper.registerModule(new JavaTimeModule());
        ticketMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        ticketMapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);

        LicenseProperties.Signing s = licenseProperties.getSigning();
        if (StringUtils.hasText(s.getKeystoreLocation())) {
            privateKey = loadPrivateKeyFromPkcs12(s);
            log.info("License ticket signing: loaded private key from keystore");
        } else {
            KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
            gen.initialize(2048);
            KeyPair pair = gen.generateKeyPair();
            privateKey = pair.getPrivate();
            log.warn(
                    "License ticket signing: no license.signing.keystore-location set; using ephemeral RSA key (signatures are not portable across restarts)");
        }
    }

    public String signTicket(Ticket ticket) {
        try {
            byte[] payload = ticketMapper.writeValueAsBytes(ticket);
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(privateKey);
            signature.update(payload);
            return Base64.getEncoder().encodeToString(signature.sign());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to sign license ticket", e);
        }
    }

    public TicketResponse signToResponse(Ticket ticket) {
        return new TicketResponse(ticket, signTicket(ticket));
    }

    private PrivateKey loadPrivateKeyFromPkcs12(LicenseProperties.Signing s) throws Exception {
        Resource resource = resourceLoader.getResource(s.getKeystoreLocation());
        char[] storePw = s.getKeystorePassword() != null ? s.getKeystorePassword().toCharArray() : new char[0];
        try (InputStream in = resource.getInputStream()) {
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(in, storePw);
            char[] keyPw = StringUtils.hasText(s.getKeyPassword()) ? s.getKeyPassword().toCharArray() : storePw;
            return (PrivateKey) keyStore.getKey(s.getKeyAlias(), keyPw);
        }
    }
}
