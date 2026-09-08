-- Shared state; never persist raw bearer tokens or verification codes.
ALTER TABLE usr_user ALTER COLUMN email TYPE VARCHAR(320);
ALTER TABLE usr_user ALTER COLUMN name TYPE VARCHAR(100);
CREATE TABLE auth_refresh_session (
    user_id BIGINT PRIMARY KEY REFERENCES usr_user(user_id) ON DELETE CASCADE,
    token_hash CHAR(64) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_auth_refresh_session_expiry ON auth_refresh_session(expires_at);
CREATE TABLE auth_revoked_token (
    token_hash CHAR(64) PRIMARY KEY,
    expires_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX ix_auth_revoked_token_expiry ON auth_revoked_token(expires_at);
CREATE TABLE auth_email_verification (
    email VARCHAR(320) PRIMARY KEY,
    code_hash VARCHAR(100),
    code_expires_at TIMESTAMPTZ,
    next_send_at TIMESTAMPTZ NOT NULL,
    failed_attempts INTEGER NOT NULL DEFAULT 0 CHECK (failed_attempts >= 0),
    verified_until TIMESTAMPTZ
);
CREATE TABLE dashboard_audit_read_cursor (
    user_id BIGINT PRIMARY KEY REFERENCES usr_user(user_id) ON DELETE CASCADE,
    last_audit_event_id BIGINT NOT NULL DEFAULT 0
);
