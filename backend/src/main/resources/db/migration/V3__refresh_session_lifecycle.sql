
ALTER TABLE refresh_token_sessions
    ADD COLUMN session_family_id BINARY(16) NULL,
    ADD COLUMN absolute_expires_at DATETIME(6) NULL;

-- Backfill existing sessions, if any.
UPDATE refresh_token_sessions
SET session_family_id = UUID_TO_BIN(UUID()),
    absolute_expires_at = DATE_ADD(created_at, INTERVAL 30 DAY)
WHERE session_family_id IS NULL;

ALTER TABLE refresh_token_sessions
    MODIFY COLUMN session_family_id BINARY(16) NOT NULL,
    MODIFY COLUMN absolute_expires_at DATETIME(6) NOT NULL,
    ADD INDEX ix_refresh_sessions_family (session_family_id, revoked_at),
    ADD CONSTRAINT ck_refresh_sessions_absolute_expiry
        CHECK (absolute_expires_at > created_at);
