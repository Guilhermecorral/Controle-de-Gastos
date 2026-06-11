ALTER TABLE users
    ADD COLUMN IF NOT EXISTS two_factor_enabled BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS two_factor_secret_encrypted VARCHAR(512);

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS two_factor_pending_secret_encrypted VARCHAR(512);

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS two_factor_enabled_at TIMESTAMP;
