CREATE TABLE IF NOT EXISTS channel_credential (
    channel_id VARCHAR(128) PRIMARY KEY,
    encrypted_access_token TEXT NOT NULL,
    encrypted_app_secret TEXT NOT NULL,
    encrypted_verify_token TEXT NOT NULL,
    key_version INTEGER NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_channel_credential_channel
        FOREIGN KEY (channel_id) REFERENCES business_channel (channel_id)
        ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_channel_credential_updated
    ON channel_credential (updated_at);
