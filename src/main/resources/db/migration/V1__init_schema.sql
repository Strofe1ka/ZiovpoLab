CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    role VARCHAR(64) NOT NULL,
    is_account_expired BOOLEAN NOT NULL DEFAULT FALSE,
    is_account_locked BOOLEAN NOT NULL DEFAULT FALSE,
    is_credentials_expired BOOLEAN NOT NULL DEFAULT FALSE,
    is_disabled BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(2048) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);

CREATE TABLE device (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    mac_address VARCHAR(255) NOT NULL,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_device_user_id ON device (user_id);

CREATE TABLE license_type (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    default_duration_in_days INTEGER NOT NULL,
    description VARCHAR(1024)
);

CREATE TABLE product (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    is_blocked BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE license (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(64) NOT NULL UNIQUE,
    user_id UUID NOT NULL REFERENCES users (id),
    product_id UUID NOT NULL REFERENCES product (id),
    type_id UUID NOT NULL REFERENCES license_type (id),
    first_activation_date DATE,
    ending_date DATE,
    blocked BOOLEAN NOT NULL DEFAULT FALSE,
    device_count INTEGER NOT NULL,
    owner_id UUID NOT NULL REFERENCES users (id),
    description VARCHAR(1024)
);

CREATE INDEX idx_license_user_id ON license (user_id);
CREATE INDEX idx_license_owner_id ON license (owner_id);

CREATE TABLE device_license (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    license_id UUID NOT NULL REFERENCES license (id) ON DELETE CASCADE,
    device_id UUID NOT NULL REFERENCES device (id) ON DELETE CASCADE,
    activation_date DATE NOT NULL,
    CONSTRAINT uq_device_license UNIQUE (license_id, device_id)
);

CREATE TABLE license_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    license_id UUID NOT NULL REFERENCES license (id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users (id),
    status VARCHAR(64) NOT NULL,
    change_date DATE NOT NULL,
    description VARCHAR(1024)
);

CREATE INDEX idx_license_history_license_id ON license_history (license_id);
