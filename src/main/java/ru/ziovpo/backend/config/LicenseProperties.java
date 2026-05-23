package ru.ziovpo.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "license")
public class LicenseProperties {

    private final Ticket ticket = new Ticket();
    private final Signing signing = new Signing();

    public Ticket getTicket() {
        return ticket;
    }

    public Signing getSigning() {
        return signing;
    }

    public static class Ticket {
        private long ttlSeconds = 300;

        public long getTtlSeconds() {
            return ttlSeconds;
        }

        public void setTtlSeconds(long ttlSeconds) {
            this.ttlSeconds = ttlSeconds;
        }
    }

    public static class Signing {
        private String keystoreLocation = "";
        private String keystorePassword = "";
        private String keyAlias = "license-signing";
        private String keyPassword = "";

        public String getKeystoreLocation() {
            return keystoreLocation;
        }

        public void setKeystoreLocation(String keystoreLocation) {
            this.keystoreLocation = keystoreLocation;
        }

        public String getKeystorePassword() {
            return keystorePassword;
        }

        public void setKeystorePassword(String keystorePassword) {
            this.keystorePassword = keystorePassword;
        }

        public String getKeyAlias() {
            return keyAlias;
        }

        public void setKeyAlias(String keyAlias) {
            this.keyAlias = keyAlias;
        }

        public String getKeyPassword() {
            return keyPassword;
        }

        public void setKeyPassword(String keyPassword) {
            this.keyPassword = keyPassword;
        }
    }
}
