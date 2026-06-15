-- Almacena las credenciales del médico en la invitación para poder crear DoctorProfile
-- cuando el invitado acepte (POST /v1/auth/accept-invitation), momento en que ya existe ClinicStaff.
ALTER TABLE core.staff_invitations
    ADD COLUMN IF NOT EXISTS doctor_credentials_json JSONB;
