CREATE SCHEMA IF NOT EXISTS core;

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "citext";

-- Create custom ENUM types in the core schema
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'staff_role' AND typnamespace = 'core'::regnamespace) THEN
        CREATE TYPE core.staff_role AS ENUM ('admin', 'doctor', 'receptionist', 'assistant', 'accountant', 'cleaning', 'clinic_admin');
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'invitation_status' AND typnamespace = 'core'::regnamespace) THEN
        CREATE TYPE core.invitation_status AS ENUM ('pending', 'accepted', 'expired', 'revoked');
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'doctor_credential_status' AND typnamespace = 'core'::regnamespace) THEN
        CREATE TYPE core.doctor_credential_status AS ENUM ('activo', 'en_tramite', 'suspendido');
    END IF;
END$$;

-- Create update trigger function
CREATE OR REPLACE FUNCTION core.update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ language 'plpgsql';

-- core.users
CREATE TABLE core.users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email CITEXT NOT NULL UNIQUE,
    phone VARCHAR(20),
    full_name VARCHAR(200) NOT NULL,
    password_hash TEXT NOT NULL,
    avatar_url TEXT,
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    theme_preference VARCHAR(10) NOT NULL DEFAULT 'light',
    failed_login_attempts SMALLINT NOT NULL DEFAULT 0,
    locked_until TIMESTAMPTZ,
    last_login_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TRIGGER update_users_modtime
BEFORE UPDATE ON core.users
FOR EACH ROW EXECUTE FUNCTION core.update_updated_at_column();

-- core.organizations
CREATE TABLE core.organizations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(200) NOT NULL,
    rfc VARCHAR(13) UNIQUE,
    owner_user_id UUID NOT NULL REFERENCES core.users(id),
    logo_url TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TRIGGER update_organizations_modtime
BEFORE UPDATE ON core.organizations
FOR EACH ROW EXECUTE FUNCTION core.update_updated_at_column();

-- core.clinics
CREATE TABLE core.clinics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID REFERENCES core.organizations(id),
    owner_user_id UUID NOT NULL REFERENCES core.users(id),
    name VARCHAR(200) NOT NULL,
    legal_name VARCHAR(300),
    rfc VARCHAR(13),
    tax_regime_code VARCHAR(10),
    address_street VARCHAR(300) NOT NULL,
    address_city VARCHAR(100) NOT NULL,
    address_state VARCHAR(100) NOT NULL,
    address_zip VARCHAR(10) NOT NULL,
    phone VARCHAR(20),
    specialties TEXT[] NOT NULL DEFAULT '{}',
    logo_url TEXT,
    timezone VARCHAR(60) NOT NULL DEFAULT 'America/Mexico_City',
    privacy_notice_url TEXT,
    data_processor_agreed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TRIGGER update_clinics_modtime
BEFORE UPDATE ON core.clinics
FOR EACH ROW EXECUTE FUNCTION core.update_updated_at_column();

-- core.clinic_staff
CREATE TABLE core.clinic_staff (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    clinic_id UUID NOT NULL REFERENCES core.clinics(id),
    user_id UUID NOT NULL REFERENCES core.users(id),
    role core.staff_role NOT NULL,
    employee_code VARCHAR(50),
    salary NUMERIC(12,2),
    salary_period VARCHAR(20),
    hire_date DATE,
    end_date DATE,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_clinic_staff_clinic_user UNIQUE (clinic_id, user_id)
);

CREATE TRIGGER update_clinic_staff_modtime
BEFORE UPDATE ON core.clinic_staff
FOR EACH ROW EXECUTE FUNCTION core.update_updated_at_column();

-- core.doctor_profiles
CREATE TABLE core.doctor_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    clinic_staff_id UUID NOT NULL UNIQUE REFERENCES core.clinic_staff(id),
    clinic_id UUID NOT NULL REFERENCES core.clinics(id),
    cedula_profesional VARCHAR(8),
    cedula_especialidad VARCHAR(8),
    especialidad VARCHAR(100),
    sub_especialidad VARCHAR(100),
    universidad_egreso VARCHAR(200) NOT NULL,
    anio_egreso SMALLINT NOT NULL,
    institucion_especialidad VARCHAR(200),
    documento_tramite_url TEXT,
    credential_status core.doctor_credential_status NOT NULL DEFAULT 'en_tramite',
    verified_at TIMESTAMPTZ,
    verified_by_user_id UUID REFERENCES core.users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_cedula_activa CHECK (credential_status <> 'activo' OR cedula_profesional IS NOT NULL),
    CONSTRAINT chk_tramite_documento CHECK (credential_status <> 'en_tramite' OR documento_tramite_url IS NOT NULL)
);

CREATE TRIGGER update_doctor_profiles_modtime
BEFORE UPDATE ON core.doctor_profiles
FOR EACH ROW EXECUTE FUNCTION core.update_updated_at_column();

-- core.refresh_tokens
CREATE TABLE core.refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES core.users(id),
    token_hash TEXT NOT NULL UNIQUE,
    device_label VARCHAR(200),
    ip_address INET,
    user_agent TEXT,
    expires_at TIMESTAMPTZ NOT NULL,
    last_used_at TIMESTAMPTZ,
    revoked_at TIMESTAMPTZ,
    replaced_by_id UUID REFERENCES core.refresh_tokens(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- core.email_verification_tokens
CREATE TABLE core.email_verification_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES core.users(id),
    token_hash TEXT NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- core.password_reset_tokens
CREATE TABLE core.password_reset_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES core.users(id),
    token_hash TEXT NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,
    requested_ip INET,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- core.password_history
CREATE TABLE core.password_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES core.users(id),
    password_hash TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- core.staff_invitations
CREATE TABLE core.staff_invitations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    clinic_id UUID NOT NULL REFERENCES core.clinics(id),
    email CITEXT NOT NULL,
    role core.staff_role NOT NULL,
    invited_by UUID NOT NULL REFERENCES core.users(id),
    token_hash TEXT NOT NULL UNIQUE,
    status core.invitation_status NOT NULL DEFAULT 'pending',
    expires_at TIMESTAMPTZ NOT NULL,
    accepted_at TIMESTAMPTZ,
    accepted_user_id UUID REFERENCES core.users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- core.user_sessions
CREATE TABLE core.user_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES core.users(id),
    refresh_token_id UUID REFERENCES core.refresh_tokens(id),
    ip_address INET,
    user_agent TEXT,
    started_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_seen_at TIMESTAMPTZ,
    ended_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
