ALTER TABLE core.refresh_tokens
    ALTER COLUMN ip_address TYPE VARCHAR(45) USING ip_address::VARCHAR;

ALTER TABLE core.password_reset_tokens
    ALTER COLUMN requested_ip TYPE VARCHAR(45) USING requested_ip::VARCHAR;
