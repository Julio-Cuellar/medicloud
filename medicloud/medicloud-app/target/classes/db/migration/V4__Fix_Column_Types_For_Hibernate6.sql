-- CITEXT -> TEXT: Hibernate 6 schema validator expects VARCHAR for String fields
ALTER TABLE core.users
    ALTER COLUMN email TYPE TEXT USING email::TEXT;

ALTER TABLE core.staff_invitations
    ALTER COLUMN email TYPE TEXT USING email::TEXT;

-- SMALLINT -> INTEGER: Hibernate 6 maps Java short/int to INTEGER during schema validation
ALTER TABLE core.users
    ALTER COLUMN failed_login_attempts TYPE INTEGER USING failed_login_attempts::INTEGER;

-- PostgreSQL custom ENUMs -> TEXT: AttributeConverter<..., String> expects VARCHAR
ALTER TABLE core.clinic_staff
    ALTER COLUMN role TYPE TEXT USING role::TEXT;

ALTER TABLE core.staff_invitations
    ALTER COLUMN role TYPE TEXT USING role::TEXT;

ALTER TABLE core.staff_invitations
    ALTER COLUMN status TYPE TEXT USING status::TEXT;

-- doctor_profiles.credential_status has CHECK constraints that reference the ENUM type;
-- they must be dropped before altering the column and recreated after.
ALTER TABLE core.doctor_profiles
    DROP CONSTRAINT chk_cedula_activa,
    DROP CONSTRAINT chk_tramite_documento;

ALTER TABLE core.doctor_profiles
    ALTER COLUMN credential_status TYPE TEXT USING credential_status::TEXT;

ALTER TABLE core.doctor_profiles
    ADD CONSTRAINT chk_cedula_activa CHECK (credential_status <> 'activo' OR cedula_profesional IS NOT NULL),
    ADD CONSTRAINT chk_tramite_documento CHECK (credential_status <> 'en_tramite' OR documento_tramite_url IS NOT NULL);
